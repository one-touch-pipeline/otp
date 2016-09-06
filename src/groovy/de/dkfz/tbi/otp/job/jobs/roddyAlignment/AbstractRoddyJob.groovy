package de.dkfz.tbi.otp.job.jobs.roddyAlignment

import de.dkfz.tbi.otp.dataprocessing.roddy.*
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.infrastructure.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.job.scheduler.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*
import de.dkfz.tbi.otp.utils.ProcessHelperService.ProcessOutput
import org.springframework.beans.factory.annotation.*

import java.util.regex.*

import static de.dkfz.tbi.otp.utils.logging.LogThreadLocal.*

/**
 * class for roddy jobs that handle failed or not finished cluster jobs, analyse them and provide
 * information about their failure for {@link AbstractMaybeSubmitWaitValidateJob}
 */
abstract class AbstractRoddyJob extends AbstractMaybeSubmitWaitValidateJob{

    public static final String NO_STARTED_JOBS_MESSAGE = '\nThere were no started jobs, the execution directory will be removed.\n'
    public static final Pattern roddyExecutionStoreDirectoryPattern = Pattern.compile(/(?:^|\n)Creating\sthe\sfollowing\sexecution\sdirectory\sto\sstore\sinformation\sabout\sthis\sprocess:\s*\n\s*(\/.*\/${RoddySnvCallingInstance.RODDY_EXECUTION_DIR_PATTERN})(?:\n|$)/)

    @Autowired
    ExecuteRoddyCommandService executeRoddyCommandService

    @Autowired
    ConfigService configService
    @Autowired
    ClusterJobService clusterJobService
    @Autowired
    SchedulerService schedulerService

    // Example:
    // Running job r150428_104246480_stds_snvCallingMetaScript => 3504988
    static final Pattern roddyOutputPattern = Pattern.compile(/^\s*(?:Running|Rerun)\sjob\s(.*_(\S+))\s=>\s(\S+)\s*$/)

    @Override
    protected final AbstractMultiJob.NextAction maybeSubmit() throws Throwable {
        Realm.withTransaction {
            final RoddyResult roddyResult = getRefreshedProcessParameterObject()
            final Realm realm = configService.getRealmDataManagement(roddyResult.project)
            String cmd = prepareAndReturnWorkflowSpecificCommand(roddyResult, realm)

            ProcessOutput output = ProcessHelperService.executeAndWait(cmd).assertExitCodeZero()

            Collection<ClusterJob> submittedClusterJobs = createClusterJobObjects(roddyResult, realm, output)
            if (submittedClusterJobs) {
                saveRoddyExecutionStoreDirectory(roddyResult, output.stderr)
                submittedClusterJobs.each {
                    threadLog?.info("Log file: ${getLogFilePath(it)}" )
                }
                return AbstractMultiJob.NextAction.WAIT_FOR_CLUSTER_JOBS
            } else {
                threadLog?.info 'Roddy has not submitted any cluster jobs. Running validate().'
                try {
                    validate()
                } catch (Throwable t) {
                    throw new RuntimeException('validate() failed after Roddy has not submitted any cluster jobs.', t)
                }
                return AbstractMultiJob.NextAction.SUCCEED
            }
        }
    }


    protected abstract String prepareAndReturnWorkflowSpecificCommand(RoddyResult roddyResult, Realm realm) throws Throwable


    @Override
    protected void validate() throws Throwable {
        Realm.withTransaction {
            final RoddyResult roddyResultObject = getRefreshedProcessParameterObject()
            validate(roddyResultObject)
        }
    }

    protected abstract void validate(RoddyResult roddyResultObject) throws Throwable

    @Override
    protected File getLogFilePath(ClusterJob clusterJob) {
        File logDirectory = ((RoddyResult) getRefreshedProcessParameterObject()).latestWorkExecutionDirectory
        return new File(logDirectory, "${clusterJob.clusterJobName}.o${clusterJob.clusterJobId}")
    }

    @Override
    protected Map<ClusterJobIdentifier, String> failedOrNotFinishedClusterJobs(
            Collection<? extends ClusterJobIdentifier> finishedClusterJobs) throws Throwable {

        RoddyResult roddyResult = getRefreshedProcessParameterObject()
        assert roddyResult

        // Roddy has started at least one pbs-job, hence the jobStateLogFile must exist.
        File latestExecutionDirectory = roddyResult.latestWorkExecutionDirectory
        JobStateLogFile jobStateLogFile = JobStateLogFile.getInstance(latestExecutionDirectory)

        return analyseFinishedClusterJobs(finishedClusterJobs, jobStateLogFile)
    }

    public Map<ClusterJobIdentifier, String> analyseFinishedClusterJobs(
            Collection<? extends ClusterJobIdentifier> finishedClusterJobs, JobStateLogFile jobStateLogFile) {

        Map<ClusterJobIdentifier, String> failedOrNotFinishedClusterJobs = [:]

        finishedClusterJobs.each {
            if (!jobStateLogFile.containsPbsId(it.clusterJobId)) {
                failedOrNotFinishedClusterJobs.put(it, "JobStateLogFile contains no information for this cluster job.")
            } else if (!jobStateLogFile.isClusterJobFinishedSuccessfully(it.clusterJobId)) {
                failedOrNotFinishedClusterJobs.put(it, "Status code: ${jobStateLogFile.getPropertyFromLatestLogFileEntry(it.clusterJobId, "statusCode")}")
            }
        }
        return failedOrNotFinishedClusterJobs
    }

    Collection<ClusterJob> createClusterJobObjects(RoddyResult roddyResult, Realm realm, ProcessOutput roddyOutput) {
        assert realm
        assert roddyResult
        ProcessingStep processingStep = getProcessingStep()
        assert processingStep
        SeqType seqType = roddyResult.getSeqType()
        assert seqType

        Collection<ClusterJob> submittedClusterJobs = []
        roddyOutput.stdout.eachLine {
            if (it.trim().isEmpty()) {
                return //skip empty lines
            }
            Matcher m = it =~ roddyOutputPattern
            if (m.matches()) {
                String jobName = m.group(1)
                String jobClass = m.group(2)
                String pbsId = m.group(3)

                if (!pbsId.matches(/[1-9]\d*/)) {
                    throw new RuntimeException("'${pbsId}' is not a valid PBS ID.")
                }

                submittedClusterJobs.add(clusterJobService.createClusterJob(realm, pbsId, realm.roddyUser, processingStep, seqType, jobName, jobClass))
            }
        }
        assert submittedClusterJobs.empty == roddyOutput.stderr.contains(NO_STARTED_JOBS_MESSAGE)
        return submittedClusterJobs
    }

    public void saveRoddyExecutionStoreDirectory(RoddyResult roddyResult, String roddyOutput) {
        assert roddyResult

        File directory = parseRoddyExecutionStoreDirectoryFromRoddyOutput(roddyOutput)
        assert directory.parentFile == roddyResult.workExecutionStoreDirectory
        WaitingFileUtils.waitUntilExists(directory)
        assert directory.isDirectory()

        roddyResult.roddyExecutionDirectoryNames.add(directory.name)

        assert roddyResult.roddyExecutionDirectoryNames.last() == roddyResult.roddyExecutionDirectoryNames.max()
        assert roddyResult.save(flush: true, failOnError: true)
    }

    public File parseRoddyExecutionStoreDirectoryFromRoddyOutput(String roddyOutput) {
        Matcher m = roddyOutput =~ roddyExecutionStoreDirectoryPattern
        if (m.find()) {
            File directory = new File(m.group(1))
            assert !m.find()
            return directory
        } else {
            throw new RuntimeException("Roddy output contains no information about output directories")
        }
    }
}
