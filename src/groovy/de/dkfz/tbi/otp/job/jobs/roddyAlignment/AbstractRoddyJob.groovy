package de.dkfz.tbi.otp.job.jobs.roddyAlignment

import de.dkfz.tbi.otp.dataprocessing.RoddyBamFile
import de.dkfz.tbi.otp.utils.ProcessHelperService
import de.dkfz.tbi.otp.utils.ProcessHelperService.ProcessOutput
import de.dkfz.tbi.otp.utils.WaitingFileUtils

import static de.dkfz.tbi.otp.utils.logging.LogThreadLocal.*

import de.dkfz.tbi.otp.dataprocessing.roddy.JobStateLogFile
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyResult
import de.dkfz.tbi.otp.infrastructure.ClusterJob
import de.dkfz.tbi.otp.infrastructure.ClusterJobIdentifier
import de.dkfz.tbi.otp.infrastructure.ClusterJobService
import de.dkfz.tbi.otp.job.processing.AbstractMaybeSubmitWaitValidateJob
import de.dkfz.tbi.otp.job.processing.ProcessingStep
import de.dkfz.tbi.otp.job.scheduler.SchedulerService
import de.dkfz.tbi.otp.ngsdata.ConfigService
import de.dkfz.tbi.otp.ngsdata.Realm
import de.dkfz.tbi.otp.ngsdata.SeqType
import de.dkfz.tbi.otp.utils.ExecuteRoddyCommandService
import org.springframework.beans.factory.annotation.Autowired
import de.dkfz.tbi.otp.job.processing.AbstractMultiJob.NextAction

import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * class for roddy jobs that handle failed or not finished cluster jobs, analyse them and provide
 * information about their failure for {@link AbstractMaybeSubmitWaitValidateJob}
 */
abstract class AbstractRoddyJob extends AbstractMaybeSubmitWaitValidateJob{

    public static final String NO_STARTED_JOBS_MESSAGE = '\nThere were no started jobs, the execution directory will be removed.\n'
    public static final Pattern roddyExecutionStoreDirectoryPattern = Pattern.compile(/(?:^|\n)Creating\sthe\sfollowing\sexecution\sdirectory\sto\sstore\sinformation\sabout\sthis\sprocess:\s*\n\s*(\/.*\/${RoddyBamFile.RODDY_EXECUTION_DIR_PATTERN})(?:\n|$)/)

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
    static final Pattern roddyOutputPattern = Pattern.compile(/^\s*(?:Running job|Rerun job) (.*_([^_]+)) => ([0-9]+)\s*$/)

    @Override
    protected final NextAction maybeSubmit() throws Throwable {
        Realm.withTransaction {
            final RoddyResult roddyResult = getRefreshedProcessParameterObject()
            final Realm realm = configService.getRealmDataManagement(roddyResult.project)
            String cmd = prepareAndReturnWorkflowSpecificCommand(roddyResult, realm)

            ProcessHelperService.ProcessOutput output =  ProcessHelperService.executeCommandAndAssertExistCodeAndReturnProcessOutput(cmd)

            Collection<ClusterJob> submittedClusterJobs = createClusterJobObjects(roddyResult, realm, output)
            if (submittedClusterJobs) {
                saveRoddyExecutionStoreDirectory(roddyResult, output.stderr)
                submittedClusterJobs.each {
                    threadLog?.info(getLogFilePaths(it))
                }
                return NextAction.WAIT_FOR_CLUSTER_JOBS
            } else {
                threadLog?.info 'Roddy has not submitted any cluster jobs. Running validate().'
                try {
                    validate()
                } catch (Throwable t) {
                    throw new RuntimeException('validate() failed after Roddy has not submitted any cluster jobs.', t)
                }
                return NextAction.SUCCEED
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
    protected String getLogFilePaths(ClusterJob clusterJob) {
        if (getRefreshedProcessParameterObject().isOldStructureUsed()) {
            //TODO: OTP-1734 delete the if part
            File logDirectory = ((RoddyResult) getRefreshedProcessParameterObject()).latestTmpRoddyExecutionDirectory
            return "Log file: ${new File(logDirectory, "${clusterJob.clusterJobName}.o${clusterJob.clusterJobId}")}"
        } else {
            File logDirectory = ((RoddyResult) getRefreshedProcessParameterObject()).latestWorkExecutionDirectory
            return "Log file: ${new File(logDirectory, "${clusterJob.clusterJobName}.o${clusterJob.clusterJobId}")}"
        }
    }

    @Override
    protected Map<ClusterJobIdentifier, String> failedOrNotFinishedClusterJobs(
            Collection<? extends ClusterJobIdentifier> finishedClusterJobs) throws Throwable {

        RoddyResult roddyResult = getRefreshedProcessParameterObject()
        assert roddyResult

        // Roddy has started at least one pbs-job, hence the jobStateLogFile must exist.
        File latestExecutionDirectory
        if (roddyResult.isOldStructureUsed()) {
            //TODO: OTP-1734 delete the if part
            latestExecutionDirectory = roddyResult.latestTmpRoddyExecutionDirectory
        } else {
            latestExecutionDirectory = roddyResult.latestWorkExecutionDirectory
        }
        JobStateLogFile jobStateLogFile = JobStateLogFile.getInstance(latestExecutionDirectory)

        if (jobStateLogFile.isEmpty()) {
            throw new RuntimeException("${jobStateLogFile.getFilePath()} is empty.")
        }

        return analyseFinishedClusterJobs(finishedClusterJobs, jobStateLogFile)
    }

    public Map<ClusterJobIdentifier, String> analyseFinishedClusterJobs(
            Collection<? extends ClusterJobIdentifier> finishedClusterJobs, JobStateLogFile jobStateLogFile) {

        Map<ClusterJobIdentifier, String> failedOrNotFinishedClusterJobs = [:]

        finishedClusterJobs.each {
            if (!jobStateLogFile.containsPbsId(it.clusterJobId)) {
                failedOrNotFinishedClusterJobs.put(it, "JobStateLogFile contains no information for ${it}")
            } else if (!jobStateLogFile.isClusterJobFinishedSuccessfully(it.clusterJobId)) {
                failedOrNotFinishedClusterJobs.put(it, "${it} has not finished successfully. Status code: ${jobStateLogFile.getPropertyFromLatestLogFileEntry(it.clusterJobId, "statusCode")}")
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

                submittedClusterJobs.add(clusterJobService.createClusterJob(realm, pbsId, processingStep, seqType, jobName, jobClass))
            } else {
                throw new RuntimeException("Could not match '${it}' against '${roddyOutputPattern}")
            }
        }
        assert submittedClusterJobs.empty == roddyOutput.stderr.contains(NO_STARTED_JOBS_MESSAGE)
        return submittedClusterJobs
    }

    public void saveRoddyExecutionStoreDirectory(RoddyResult roddyResult, String roddyOutput) {
        assert roddyResult

        File directory = parseRoddyExecutionStoreDirectoryFromRoddyOutput(roddyOutput)
        if (getRefreshedProcessParameterObject().isOldStructureUsed()) {
            //TODO: OTP-1734 delete the if part
            assert directory.parentFile == roddyResult.tmpRoddyExecutionStoreDirectory
        } else {
            assert directory.parentFile == roddyResult.workExecutionStoreDirectory
        }
        assert WaitingFileUtils.waitUntilExists(directory)
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
