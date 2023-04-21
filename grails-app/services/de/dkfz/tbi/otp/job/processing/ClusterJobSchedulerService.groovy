/*
 * Copyright 2011-2020 The OTP authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package de.dkfz.tbi.otp.job.processing

import grails.compiler.GrailsCompileStatic
import grails.gorm.transactions.Transactional
import grails.util.Environment
import org.slf4j.event.Level
import org.springframework.beans.factory.annotation.Autowired

import de.dkfz.roddy.config.JobLog
import de.dkfz.roddy.config.ResourceSet
import de.dkfz.roddy.execution.jobs.*
import de.dkfz.roddy.tools.BufferValue
import de.dkfz.tbi.otp.OtpException
import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.infrastructure.*
import de.dkfz.tbi.otp.job.scheduler.ClusterJobStatus
import de.dkfz.tbi.otp.job.scheduler.SchedulerService
import de.dkfz.tbi.otp.ngsdata.Realm
import de.dkfz.tbi.otp.ngsdata.SeqType
import de.dkfz.tbi.otp.utils.logging.LogThreadLocal
import de.dkfz.tbi.otp.utils.logging.SimpleLogger
import de.dkfz.tbi.otp.workflowExecution.ProcessingPriority

import java.nio.file.FileSystem
import java.nio.file.Path
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * This class contains methods to communicate with the cluster job scheduler.
 *
 * It executes job submission/monitoring commands on the cluster head node using
 * the <a href="https://github.com/eilslabs/BatchEuphoria">BatchEuphoria</a> library.
 */
@GrailsCompileStatic
@Transactional
@Deprecated//replace by a new system ClusterStatisticService
class ClusterJobSchedulerService {

    static final int WAITING_TIME_FOR_SECOND_TRY_IN_MILLISECONDS = (Environment.current == Environment.TEST) ? 0 : 10000

    static final String CLUSTER_JOBS_STATE_LOG_DIRECTORY = "cluster-jobs-state-old"

    static final DateTimeFormatter PATH_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd/HH-mm-ss'.txt'")

    @Autowired
    SchedulerService schedulerService

    ClusterJobLoggingService clusterJobLoggingService
    ClusterJobManagerFactoryService clusterJobManagerFactoryService
    ClusterJobService clusterJobService
    ClusterJobSubmissionOptionsService clusterJobSubmissionOptionsService
    ConfigService configService
    FileService fileService
    JobStatusLoggingService jobStatusLoggingService
    ProcessingOptionService processingOptionService
    FileSystemService fileSystemService

    /**
     * Executes a job on a cluster specified by the realm.
     *
     * @param realm The realm which identifies the submission host of the cluster
     * @param script The script to be run on the cluster
     * @param environmentVariables environment variables to set for the job
     * @param jobSubmissionOptions additional options for the job
     * @return the cluster job ID
     */
    @Deprecated
    @SuppressWarnings("ThrowRuntimeException") // ignored: will be removed with the old workflow system
    String executeJob(Realm realm, String script, Map<String, String> environmentVariables = [:],
                      Map<JobSubmissionOption, String> jobSubmissionOptions = [:]) throws Throwable {
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

        ProcessingPriority processingPriority = domainObject?.processingPriority
        if (processingPriority) {
            options.put(
                    JobSubmissionOption.QUEUE,
                    processingPriority.queue
            )
        }

        String jobName = processingStep.clusterJobName
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
                JobLog.toOneFile(new File(clusterLogDirectory, "${jobName}-{JOB_ID}.log")),
                null,
        )

        BEJobResult jobResult = jobManager.submitJob(job)
        if (!jobResult.successful) {
            try {
                jobManager.killJobs([job])
            } catch (Exception ignored) {
            }

            throw new RuntimeException("An error occurred when submitting the job")
        }
        jobManager.startHeldJobs([job])

        ClusterJob clusterJob = clusterJobService.createClusterJob(
                realm, job.jobID.shortID, configService.sshUser, processingStep, seqType, jobName
        )
        retrieveAndSaveJobInformationAfterJobStarted(clusterJob)

        LogThreadLocal.threadLog?.info("Log file: ${clusterJob.jobLog}")

        return job.jobID.shortID
    }

    /**
     * Returns a map of jobs the cluster job scheduler knows about
     *
     * @param realm The realm to connect to
     * @param userName The name of the user whose jobs should be checked
     * @return A map containing job identifiers and their status
     */
    @Deprecated
    Map<ClusterJobIdentifier, ClusterJobStatus> retrieveKnownJobsWithState(Realm realm) throws Exception {
        assert realm: "No realm specified."
        BatchEuphoriaJobManager jobManager = clusterJobManagerFactoryService.getJobManager(realm)

        Map<BEJobID, JobState> jobStates = queryAndLogAllClusterJobs(jobManager)

        return jobStates.collectEntries { BEJobID jobId, JobState state ->
            [
                    new ClusterJobIdentifier(realm, jobId.id),
                    (state in finished || state in failed) ? ClusterJobStatus.COMPLETED :
                            state == JobState.UNKNOWN ? ClusterJobStatus.UNKNOWN : ClusterJobStatus.NOT_COMPLETED,
            ]
        } as Map<ClusterJobIdentifier, ClusterJobStatus>
    }

    @Deprecated
    private Map<BEJobID, JobState> queryAndLogAllClusterJobs(BatchEuphoriaJobManager jobManager) {
        Map<BEJobID, JobState> jobStates
        StringBuilder logStringBuilder = new StringBuilder()
        LogThreadLocal.withThreadLog(logStringBuilder) {
            ((SimpleLogger) LogThreadLocal.threadLog).level = Level.DEBUG
            jobStates = jobManager.queryJobStatusAll()
        }

        Path logFile = pathForLogging()

        fileService.createFileWithContentOnDefaultRealm(logFile, logStringBuilder.toString())

        return jobStates
    }

    @Deprecated
    private Path pathForLogging() {
        String dateDirectory = LocalDateTime.now().format(PATH_FORMATTER)

        FileSystem fileSystem = fileSystemService.remoteFileSystemOnDefaultRealm

        Path baseLogDir = fileSystem.getPath(configService.loggingRootPath.path)

        Path logFile = baseLogDir.resolve(CLUSTER_JOBS_STATE_LOG_DIRECTORY).resolve(dateDirectory)
        assert logFile.absolute

        return logFile
    }

    @Deprecated
    void retrieveAndSaveJobInformationAfterJobStarted(ClusterJob clusterJob) throws Exception {
        BEJobID beJobID = new BEJobID(clusterJob.clusterJobId)
        BatchEuphoriaJobManager jobManager = clusterJobManagerFactoryService.getJobManager(clusterJob.realm)
        GenericJobInfo jobInfo = null

        try {
            jobInfo = jobManager.queryExtendedJobStateById([beJobID]).get(beJobID)
            if (jobInfo?.jobState == JobState.UNKNOWN) {
                throw new OtpException("Jobstate is ${JobState.UNKNOWN}")
            }
        } catch (Throwable ignored) {
            LogThreadLocal.threadLog?.warn("Failed to fill in runtime statistics after start for ${clusterJob.clusterJobId}, try again")
            Thread.sleep(WAITING_TIME_FOR_SECOND_TRY_IN_MILLISECONDS)
            try {
                jobInfo = jobManager.queryExtendedJobStateById([beJobID]).get(beJobID)
            } catch (Throwable e) {
                LogThreadLocal.threadLog?.warn("Failed to fill in runtime statistics after start for ${clusterJob.clusterJobId} the second time", e)
            }
        }
        if (jobInfo) {
            clusterJobService.amendClusterJob(clusterJob, jobInfo)
        }
    }

    @Deprecated
    void retrieveAndSaveJobStatisticsAfterJobFinished(ClusterJob clusterJob) throws Exception {
        BatchEuphoriaJobManager jobManager = clusterJobManagerFactoryService.getJobManager(clusterJob.realm)
        BEJobID beJobId = new BEJobID(clusterJob.clusterJobId)
        GenericJobInfo jobInfo = jobManager.queryExtendedJobStateById([beJobId]).get(beJobId)

        if (jobInfo) {
            ClusterJob.Status status = null
            if (jobInfo.jobState && jobInfo.exitCode != null) {
                status = jobInfo.jobState in finished && jobInfo.exitCode == 0 ? ClusterJob.Status.COMPLETED : ClusterJob.Status.FAILED
            }
            clusterJobService.completeClusterJob(clusterJob, status, jobInfo)
        }
    }

    @Deprecated
    private final List<JobState> failed = [
            JobState.FAILED,
            JobState.ABORTED,
            JobState.STARTED, //roddy internal
            JobState.DUMMY,   //roddy internal
    ].asImmutable()

    // a job is considered complete if it either has status "completed" or it is not known anymore
    @Deprecated
    private final List<JobState> finished = [
            JobState.COMPLETED_SUCCESSFUL,
            JobState.COMPLETED_UNKNOWN,
            //JobState.UNKNOWN, //This state is used if no mapping is available, so it can also be running
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
