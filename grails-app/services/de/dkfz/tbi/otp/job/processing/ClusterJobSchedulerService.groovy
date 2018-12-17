package de.dkfz.tbi.otp.job.processing

import grails.compiler.GrailsCompileStatic
import grails.util.Environment
import groovy.transform.EqualsAndHashCode
import org.apache.commons.logging.impl.SimpleLog

import de.dkfz.roddy.config.JobLog
import de.dkfz.roddy.config.ResourceSet
import de.dkfz.roddy.execution.jobs.*
import de.dkfz.roddy.tools.BufferValue
import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.dataprocessing.ProcessingPriority
import de.dkfz.tbi.otp.infrastructure.*
import de.dkfz.tbi.otp.job.scheduler.ClusterJobMonitoringService
import de.dkfz.tbi.otp.job.scheduler.SchedulerService
import de.dkfz.tbi.otp.ngsdata.Realm
import de.dkfz.tbi.otp.ngsdata.SeqType
import de.dkfz.tbi.otp.utils.logging.LogThreadLocal

import java.nio.file.Path
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import static de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName.CLUSTER_SUBMISSIONS_FAST_TRACK_QUEUE
import static de.dkfz.tbi.otp.utils.logging.LogThreadLocal.threadLog

/**
 * This class contains methods to communicate with the cluster job scheduler.
 *
 * It executes job submission/monitoring commands on the cluster head node using
 * the <a href="https://github.com/eilslabs/BatchEuphoria">BatchEuphoria</a> library.
 */
@GrailsCompileStatic
class ClusterJobSchedulerService {

    static final int WAITING_TIME_FOR_SECOND_TRY_IN_MILLISECONDS = (Environment.getCurrent() == Environment.TEST) ? 0 : 10000

    static final String CLUSTER_JOBS_STATE_LOG_DIRECTORY = "cluster-jobs-state"

    final DateTimeFormatter PATH_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd/HH-mm-ss'.txt'")

    SchedulerService schedulerService

    ClusterJobLoggingService clusterJobLoggingService
    ClusterJobManagerFactoryService clusterJobManagerFactoryService
    ClusterJobService clusterJobService
    ClusterJobSubmissionOptionsService clusterJobSubmissionOptionsService
    ConfigService configService
    FileService fileService
    JobStatusLoggingService jobStatusLoggingService
    ProcessingOptionService processingOptionService

    /**
     * Executes a job on a cluster specified by the realm.
     *
     * @param realm The realm which identifies the submission host of the cluster
     * @param script The script to be run on the cluster
     * @param environmentVariables environment variables to set for the job
     * @param jobSubmissionOptions additional options for the job
     * @return the cluster job ID
     */
    String executeJob(Realm realm, String script, Map<String, String> environmentVariables = [:], Map<JobSubmissionOption, String> jobSubmissionOptions = [:]) throws Throwable {
        if (!script) {
            throw new ProcessingException("No job script specified.")
        }
        assert realm: 'No realm specified.'

        ProcessingStep processingStep = schedulerService.jobExecutedByCurrentThread.processingStep
        processingStep.refresh() // processingStep.jobClass might be outdated by now
        ProcessParameterObject domainObject = processingStep.processParameterObject

        SeqType seqType = domainObject?.seqType

        Map<JobSubmissionOption, String> options = clusterJobSubmissionOptionsService.readOptionsFromDatabase(processingStep, realm)
        options.putAll(jobSubmissionOptions)

        // check if the project has FASTTRACK priority
        short processingPriority = domainObject?.processingPriority ?: ProcessingPriority.NORMAL.priority
        if (processingPriority >= ProcessingPriority.FAST_TRACK.priority) {
            options.put(
                    JobSubmissionOption.QUEUE,
                    processingOptionService.findOptionAsString(CLUSTER_SUBMISSIONS_FAST_TRACK_QUEUE)
            )
        }

        String jobName = processingStep.getClusterJobName()
        String logFile = jobStatusLoggingService.constructLogFileLocation(realm, processingStep)
        String logMessage = jobStatusLoggingService.constructMessage(realm, processingStep)
        File clusterLogDirectory = clusterJobLoggingService.createAndGetLogDirectory(realm, processingStep)

        String scriptText = """\
            |# OTP: Fail on first non-zero exit code
            |set -e

            |umask 0027
            |date +%Y-%m-%d-%H-%M
            |echo \$HOST

            |# BEGIN ORIGINAL SCRIPT
            |${script}
            |# END ORIGINAL SCRIPT

            |touch "${logFile}"
            |chmod 0640 "${logFile}"
            |echo "${logMessage}" >> "${logFile}"
            |""".stripMargin()

        BatchEuphoriaJobManager jobManager = clusterJobManagerFactoryService.getJobManager(realm)

        ResourceSet resourceSet = new ResourceSet(
                options.get(JobSubmissionOption.MEMORY) ? new BufferValue(options.get(JobSubmissionOption.MEMORY)) : null,
                options.get(JobSubmissionOption.CORES) as Integer,
                options.get(JobSubmissionOption.NODES) as Integer,
                (Duration) (options.get(JobSubmissionOption.WALLTIME) ? Duration.parse(options.get(JobSubmissionOption.WALLTIME)) : null),
                options.get(JobSubmissionOption.STORAGE) ? new BufferValue(options.get(JobSubmissionOption.STORAGE)) : null,
                options.get(JobSubmissionOption.QUEUE),
                options.get(JobSubmissionOption.NODE_FEATURE),
        )

        BEJob job = new BEJob(
                null,
                jobName,
                null,
                scriptText,
                null,
                resourceSet,
                [],
                environmentVariables,
                jobManager,
                JobLog.toOneFile(clusterLogDirectory),
                null,
        )

        BEJobResult jobResult = jobManager.submitJob(job)
        if (!jobResult.isSuccessful()) {
            try {
                jobManager.killJobs([job])
            } catch (Exception e) {
            }

            throw new RuntimeException("An error occurred when submitting the job")
        }
        jobManager.startHeldJobs([job])

        ClusterJob clusterJob = clusterJobService.createClusterJob(
                realm, job.jobID.shortID, configService.getSshUser(), processingStep, seqType, jobName
        )
        retrieveAndSaveJobInformationAfterJobStarted(clusterJob)

        threadLog?.info("Log file: ${clusterJob.jobLog}")

        return job.getJobID().shortID
    }

    /**
     * Returns a map of jobs the cluster job scheduler knows about
     *
     * @param realm The realm to connect to
     * @param userName The name of the user whose jobs should be checked
     * @return A map containing job identifiers and their status
     */
    Map<ClusterJobIdentifier, ClusterJobMonitoringService.Status> retrieveKnownJobsWithState(Realm realm, String userName) throws Exception {
        assert realm: "No realm specified."
        assert userName: "No user name specified."
        BatchEuphoriaJobManager jobManager = clusterJobManagerFactoryService.getJobManager(realm)


        Map<BEJobID, JobState> jobStates = queryAndLogAllClusterJobs(jobManager)

        return jobStates.collectEntries { BEJobID jobId, JobState state ->
            [
                    new ClusterJobIdentifier(realm, jobId.id, userName),
                    (state in finished || state in failed) ? ClusterJobMonitoringService.Status.COMPLETED : ClusterJobMonitoringService.Status.NOT_COMPLETED,
            ]
        } as Map<ClusterJobIdentifier, ClusterJobMonitoringService.Status>
    }

    private Map<BEJobID, JobState> queryAndLogAllClusterJobs(BatchEuphoriaJobManager jobManager) {
        Map<BEJobID, JobState> jobStates
        StringBuilder logStringBuilder = new StringBuilder()
        LogThreadLocal.withThreadLog(logStringBuilder) {
            ((SimpleLog) LogThreadLocal.threadLog).setLevel(SimpleLog.LOG_LEVEL_DEBUG)
            jobStates = jobManager.queryJobStatusAll()
        }

        Path logFile = pathForLogging()

        fileService.createFileWithContent(logFile, logStringBuilder.toString())

        return jobStates
    }

    private Path pathForLogging() {
        String dateDirectory = LocalDateTime.now().format(PATH_FORMATTER)
        Path baseLogDir = configService.getLoggingRootPath().toPath()

        Path logFile = baseLogDir.resolve(CLUSTER_JOBS_STATE_LOG_DIRECTORY).resolve(dateDirectory)
        assert logFile.absolute

        return logFile
    }


    void retrieveAndSaveJobInformationAfterJobStarted(ClusterJob clusterJob) throws Exception {
        BEJobID beJobID = new BEJobID(clusterJob.clusterJobId)
        BatchEuphoriaJobManager jobManager = clusterJobManagerFactoryService.getJobManager(clusterJob.realm)
        GenericJobInfo jobInfo = null

        try {
            jobInfo = jobManager.queryExtendedJobStateById([beJobID]).get(beJobID)
        } catch (Throwable e) {
            threadLog?.warn("Failed to fill in runtime statistics after start for ${clusterJob.clusterJobId}, try again", e)
            Thread.sleep(WAITING_TIME_FOR_SECOND_TRY_IN_MILLISECONDS)
            try {
                jobInfo = jobManager.queryExtendedJobStateById([beJobID]).get(beJobID)
            } catch (Throwable e2) {
                threadLog?.warn("Failed to fill in runtime statistics after start for ${clusterJob.clusterJobId} the second time", e2)
            }
        }
        if (jobInfo) {
            clusterJobService.amendClusterJob(clusterJob, jobInfo)
        }
    }

    void retrieveAndSaveJobStatisticsAfterJobFinished(ClusterJobIdentifier jobIdentifier) throws Exception {
        BatchEuphoriaJobManager jobManager = clusterJobManagerFactoryService.getJobManager(jobIdentifier.realm)
        GenericJobInfo jobInfo = jobManager.queryExtendedJobStateById([new BEJobID(jobIdentifier.clusterJobId)])
                .get(new BEJobID(jobIdentifier.clusterJobId))

        if (jobInfo) {
            ClusterJob.Status status = null
            if (jobInfo.jobState && jobInfo.exitCode != null) {
                status = jobInfo.jobState in finished && jobInfo.exitCode == 0 ? ClusterJob.Status.COMPLETED : ClusterJob.Status.FAILED
            }
            clusterJobService.completeClusterJob(jobIdentifier, status, jobInfo)
        }
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
}

enum JobSubmissionOption {
    CORES,
    MEMORY,
    NODE_FEATURE,
    NODES,
    QUEUE,
    STORAGE,
    WALLTIME,
}

@EqualsAndHashCode(includes = ["userName", "realm"])
class RealmAndUser {

    final Realm realm

    final String userName

    RealmAndUser(final Realm realm, final String userName) {
        assert realm: "Realm not specified"
        assert userName: "User name not specified"
        this.realm = realm
        this.userName = userName
    }
}
