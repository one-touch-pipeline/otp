package de.dkfz.tbi.otp.job.jobs.roddyAlignment

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
            final RoddyResult roddyResult = getProcessParameterObject()
            final Realm realm = configService.getRealmDataManagement(roddyResult.project)
            String cmd = prepareAndReturnWorkflowSpecificCommand(roddyResult, realm)
            log.debug "The roddy command:\n${cmd}"

            Process process = executeRoddyCommandService.executeRoddyCommand(cmd)

            String stdout = executeRoddyCommandService.returnStdoutOfFinishedCommandExecution(process)
            executeRoddyCommandService.checkIfRoddyWFExecutionWasSuccessful(process)

            createClusterJobObjects(realm, stdout)


            return NextAction.WAIT_FOR_CLUSTER_JOBS
        }
    }


    protected abstract String prepareAndReturnWorkflowSpecificCommand(RoddyResult roddyResult, Realm realm) throws Throwable


    @Override
    protected void validate() throws Throwable {
        Realm.withTransaction {
            final RoddyResult roddyResultObject = getProcessParameterObject()
            validate(roddyResultObject)
        }
    }

    protected abstract void validate(RoddyResult roddyResultObject) throws Throwable

    @Override
    protected String getLogFilePaths(ClusterJob clusterJob) {
        File logDirectory = ((RoddyResult)getProcessParameterObject()).latestTmpRoddyExecutionDirectory
        return "Log file: ${new File(logDirectory, "${clusterJob.clusterJobName}.o${clusterJob.clusterJobId}")}"
    }

    @Override
    protected Map<ClusterJobIdentifier, String> failedOrNotFinishedClusterJobs(
            Collection<? extends ClusterJobIdentifier> finishedClusterJobs) throws Throwable {

        RoddyResult roddyResult = getProcessParameterObject()
        assert roddyResult

        // Roddy has started at least one pbs-job, hence the jobStateLogFile must exist.
        JobStateLogFile jobStateLogFile = JobStateLogFile.getInstance(roddyResult.getLatestTmpRoddyExecutionDirectory())

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

    void createClusterJobObjects(Realm realm, String roddyOutput) {
        assert realm
        ProcessingStep processingStep = getProcessingStep()
        assert processingStep
        SeqType seqType = ((RoddyResult)getProcessParameterObject()).getSeqType()
        assert seqType

        roddyOutput.eachLine {
            if (it.trim().isEmpty()) {
                return //skip empty lines
            }
            Matcher m = it =~ roddyOutputPattern
            if (m.matches()) {
                String jobName = m.group(1)
                String jobClass = m.group(2)
                String pbsId = m.group(3)

                ClusterJob clusterJob = clusterJobService.createClusterJob(realm, pbsId, processingStep, seqType, jobName, jobClass)
                threadLog?.info(getLogFilePaths(clusterJob))
            } else {
                throw new RuntimeException("Could not match '${it}' against '${roddyOutputPattern}")
            }
        }
    }
}
