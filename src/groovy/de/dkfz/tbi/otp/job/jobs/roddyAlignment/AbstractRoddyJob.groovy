package de.dkfz.tbi.otp.job.jobs.roddyAlignment

import de.dkfz.tbi.otp.dataprocessing.roddy.JobStateLogFiles
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyResult
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
 * information about their failure for {@AbstractMaybeSubmitWaitValidateJob}
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
    protected Map<ClusterJobIdentifier, String> failedOrNotFinishedClusterJobs(
            Collection<? extends ClusterJobIdentifier> finishedClusterJobs) throws Throwable {

        RoddyResult roddyResult = getProcessParameterObject()
        assert roddyResult

        JobStateLogFiles jobStateLogFiles = JobStateLogFiles.create(roddyResult.getTmpRoddyExecutionStoreDirectory().path)

        return analyseFinishedClusterJobs(finishedClusterJobs, jobStateLogFiles)
    }

    public Map<ClusterJobIdentifier, String> analyseFinishedClusterJobs(
            Collection<? extends ClusterJobIdentifier> finishedClusterJobs, JobStateLogFiles jobStateLogFiles) {

        Map<ClusterJobIdentifier, String> failedOrNotFinishedClusterJobs = [:]

        finishedClusterJobs.each {
            if (!jobStateLogFiles.containsPbsId(it.clusterJobId)) {
                failedOrNotFinishedClusterJobs.put(it, "JobStateLogFile contains no information for ${it}")
            } else {
                if (jobStateLogFiles.isClusterJobInProgress(it.clusterJobId)) {
                    failedOrNotFinishedClusterJobs.put(it, "${it} is not finished.")
                } else if (!jobStateLogFiles.isClusterJobFinishedSuccessfully(it.clusterJobId)) {
                    failedOrNotFinishedClusterJobs.put(it, "${it} failed processing. ExitCode: ${jobStateLogFiles.getPropertyFromLatestLogFileEntry(it.clusterJobId, "exitCode")}")
                }
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

                clusterJobService.createClusterJob(realm, pbsId, processingStep, seqType, jobName, jobClass)
            } else {
                throw new RuntimeException("Could not match '${it}' against '${roddyOutputPattern}")
            }
        }
    }
}
