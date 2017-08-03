package de.dkfz.tbi.otp.job.processing

import de.dkfz.roddy.config.*
import de.dkfz.roddy.execution.jobs.*
import de.dkfz.roddy.execution.jobs.cluster.*
import de.dkfz.roddy.execution.jobs.cluster.pbs.*
import de.dkfz.roddy.tools.*
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.infrastructure.*
import de.dkfz.tbi.otp.job.scheduler.*
import de.dkfz.tbi.otp.ngsdata.*
import groovy.transform.*

import java.time.*

import static de.dkfz.tbi.otp.utils.logging.LogThreadLocal.*
import static org.springframework.util.Assert.*


/**
 * This class contains methods to communicate with the cluster job scheduler.
 *
 * It executes job submission/monitoring commands on the cluster head node using
 * the <a href="https://github.com/eilslabs/BatchEuphoria">BatchEuphoria</a> library and {@link ExecutionService}.
 */
class PbsService {

    PbsOptionMergingService pbsOptionMergingService
    JobStatusLoggingService jobStatusLoggingService
    SchedulerService schedulerService
    ClusterJobService clusterJobService
    ExecutionService executionService

    ClusterJobLoggingService clusterJobLoggingService

    private Map<RealmAndUser, ClusterJobManager> managerPerRealm = [:]

    /**
     * Executes a job on a cluster specified by the realm.
     *
     * @param realm The realm which identifies the submission host of the cluster
     * @param script The script to be run on the cluster
     * @param environmentVariables environment variables to set for the job
     * @param jobSubmissionOptions additional options for the job
     * @return the cluster job ID
     */
    public String executeJob(Realm realm, String script, Map<String, String> environmentVariables = [:], Map<JobSubmissionOption, String> jobSubmissionOptions = [:]) {
        if (!script) {
            throw new ProcessingException("No job script specified.")
        }
        notNull realm, 'No realm specified.'

        ProcessingStep processingStep = schedulerService.jobExecutedByCurrentThread.processingStep
        ProcessParameterObject domainObject = processingStep.processParameterObject

        SeqType seqType = domainObject?.seqType

        Map<JobSubmissionOption, String> options = pbsOptionMergingService.readOptionsFromDatabase(processingStep, realm)
        options.putAll(jobSubmissionOptions)

        // check if the project has FASTTRACK priority
        short processingPriority = domainObject?.processingPriority ?: ProcessingPriority.NORMAL_PRIORITY
        if (processingPriority >= ProcessingPriority.FAST_TRACK_PRIORITY) {
            options.putAll([
                    (JobSubmissionOption.ACCOUNT): "FASTTRACK",
            ])
        }

        String jobName = processingStep.getClusterJobName()
        String logFile = jobStatusLoggingService.constructLogFileLocation(realm, processingStep)
        String logMessage = jobStatusLoggingService.constructMessage(realm, processingStep)
        File clusterLogDirectory = clusterJobLoggingService.createAndGetLogDirectory(realm, processingStep)

        String scriptText = """
# OTP: Fail on first non-zero exit code
set -e

umask 0027
date +%Y-%m-%d-%H-%M
echo \$HOST

# BEGIN ORIGINAL SCRIPT
${script}
# END ORIGINAL SCRIPT

touch "${logFile}"
chmod 0640 "${logFile}"
echo "${logMessage}" >> "${logFile}"
"""

        BatchEuphoriaJobManager jobManager = getJobManager(realm, realm.unixUser)

        ResourceSet resourceSet = new ResourceSet(
                options.get(JobSubmissionOption.MEMORY) ? new BufferValue(options.get(JobSubmissionOption.MEMORY)) : null,
                options.get(JobSubmissionOption.CORES) as Integer,
                options.get(JobSubmissionOption.NODES) as Integer,
                (Duration)(options.get(JobSubmissionOption.WALLTIME) ? Duration.parse(options.get(JobSubmissionOption.WALLTIME)) : null),
                options.get(JobSubmissionOption.STORAGE) ? new BufferValue(options.get(JobSubmissionOption.STORAGE)) : null,
                options.get(JobSubmissionOption.QUEUE),
                options.get(JobSubmissionOption.NODE_FEATURE),
        )

        BEJob job = new BEJob(
                jobName,
                null,
                scriptText,
                null,
                resourceSet,
                environmentVariables,
                jobManager,
        )
        job.setLoggingDirectory(clusterLogDirectory)
        job.customUserAccount = options.get(JobSubmissionOption.ACCOUNT) as String

        BEJobResult jobResult = jobManager.runJob(job)
        if (!jobResult.wasExecuted) {
            throw new RuntimeException("An error occurred when submitting the job")
        }
        jobManager.startHeldJobs([job])

        ClusterJob clusterJob = clusterJobService.createClusterJob(
                realm, job.jobID.shortID, realm.unixUser, processingStep, seqType, jobName
        )
        threadLog?.info("Log file: ${AbstractOtpJob.getDefaultLogFilePath(clusterJob).path}")

        return job.jobID
    }

    /**
     * Returns a map of jobs the cluster job scheduler knows about
     *
     * @param realm The realm to connect to
     * @param userName The name of the user whose jobs should be checked
     * @return A map containing job identifiers and their status
     */
    public Map<ClusterJobIdentifier, PbsMonitorService.Status> retrieveKnownJobsWithState(Realm realm, String userName) throws Exception {
        assert realm: "No realm specified."
        assert userName: "No user name specified."

        BatchEuphoriaJobManager jobManager = getJobManager(realm, userName)
        Map<String, JobState> jobStates = jobManager.queryJobStatusAll(true)

        return jobStates.collectEntries { String jobId, JobState state ->
            [
                    new ClusterJobIdentifier(realm, jobId, userName),
                    (state in finished || state in failed) ? PbsMonitorService.Status.COMPLETED : PbsMonitorService.Status.NOT_COMPLETED
            ]
        }
    }

    public void retrieveAndSaveJobStatistics(ClusterJobIdentifier jobIdentifier) {
        BatchEuphoriaJobManager jobManager = getJobManager(jobIdentifier.realm, jobIdentifier.userName)
        Map<String, GenericJobInfo> jobInfos = jobManager.queryExtendedJobStateById([jobIdentifier.clusterJobId], true)
        GenericJobInfo jobInfo = jobInfos.get(jobIdentifier.clusterJobId)

        ClusterJob.Status status = null
        if (jobInfo.jobState && jobInfo.exitCode) {
            status = jobInfo.jobState in finished && jobInfo.exitCode != 0 ? ClusterJob.Status.COMPLETED : ClusterJob.Status.FAILED
        }

        clusterJobService.completeClusterJob(jobIdentifier, status, jobInfo)
    }

    private final List<JobState> failed = [
            JobState.FAILED,
            JobState.ABORTED,
            JobState.STARTED, //roddy internal
            JobState.DUMMY,   //roddy internal
    ].asImmutable()

    // a job is considered complete if it either has status "completed" or it is not known anymore
    private final List<JobState> finished = [
            JobState.COMPLETED_SUCCESSFUL,
            JobState.COMPLETED_UNKNOWN,
            JobState.UNKNOWN,
    ].asImmutable()

    private final List<JobState> notFinished = [
            JobState.HOLD,
            JobState.QUEUED,
            JobState.UNSTARTED, // not submitted yet
            JobState.RUNNING,
            JobState.SUSPENDED,
    ].asImmutable()

    /**
     * Get name of environment variable which contains the cluster job ID in a running job
     * @return variable returned in the format ${VARIABLE} so it can be used directly in a shell script
     */
    public static String getJobIdEnvironmentVariable(Realm realm) {
        if (realm.jobScheduler == Realm.JobScheduler.PBS) {
            return PBSJobManager.PBS_JOBID
        } else {
            throw new Exception("Unsupported cluster job scheduler")
        }
    }

    private BatchEuphoriaJobManager getJobManager(Realm realm, String user) {
        BatchEuphoriaJobManager manager = managerPerRealm[new RealmAndUser(realm, user)]

        if (manager == null) {
            JobManagerCreationParameters jobManagerParameters = new JobManagerCreationParametersBuilder()
                    .setCreateDaemon(false)
                    .setUserIdForJobQueries(user)
                    .setTrackUserJobsOnly(true)
                    .setTrackOnlyStartedJobs(false)
                    .setUserMask("027")
                    .build()
            jobManagerParameters.strictMode = true

            if (realm.jobScheduler == Realm.JobScheduler.PBS) {
                manager = new PBSJobManager(new BEExecutionServiceAdapter(executionService, realm, user), jobManagerParameters)
            } else {
                throw new Exception("Unsupported cluster job scheduler")
            }
            managerPerRealm[new RealmAndUser(realm, user)] = manager
        }
        return manager
    }
}

enum JobSubmissionOption {
    ACCOUNT,
    CORES,
    MEMORY,
    NODE_FEATURE,
    NODES,
    QUEUE,
    STORAGE,
    WALLTIME,
}

@EqualsAndHashCode(includes = ["userName", "realmId"])
public class RealmAndUser {

    final Realm realm

    final String userName

    def getRealmId() {
        realm.id
    }

    public RealmAndUser(final Realm realm, final String userName) {
        assert realm : "Realm not specified"
        assert userName : "User name not specified"
        this.realm = realm
        this.userName = userName
    }
}
