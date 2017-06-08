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
import de.dkfz.tbi.otp.utils.*
import de.dkfz.tbi.otp.utils.ProcessHelperService.ProcessOutput
import groovy.transform.*

import java.time.*
import java.util.regex.*

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

    private Map<Realm, ClusterJobManager> managerPerRealm = [:]

    private static final String JOB_LIST_PATTERN = /^(\d+)\.(?:\S+\s+){9}(\w)\s+\S+\s*$/

    /**
     * Possible states for a PBS cluster job
     *
     * @see <a href="http://docs.adaptivecomputing.com/torque/6-0-1/help.htm#topics/torque/commands/qstat.htm">TORQUE Commands Overview</a>
     */
    @TupleConstructor
    enum ClusterJobStatus {
        COMPLETED("C"),
        EXITED("E"),
        HELD("H"),
        QUEUED("Q"),
        RUNNING("R"),
        BEING_MOVED("T"),
        WAITING("W"),
        SUSPENDED("S")

        final String code

        public static ClusterJobStatus getStatusByCode(String code) {
            for (ClusterJobStatus status : values()) {
                if (status.code == code) {
                    return status
                }
            }
            throw new IllegalArgumentException()
        }
    }


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

        BatchEuphoriaJobManager jobManager = getJobManager(realm)

        ResourceSet resourceSet = new ResourceSet(
                null,
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
                null,
                environmentVariables,
                null,
                null,
                jobManager,
        )
        job.setLoggingDirectory(clusterLogDirectory)
        job.customUserAccount = options.get(JobSubmissionOption.ACCOUNT) as String

        BEJobResult jobResult = jobManager.runJob(job)
        if (!jobResult.wasExecuted) {
            throw new RuntimeException("An error occurred when submitting the job")
        }
        jobManager.startHeldJobs([job])

        ClusterJob clusterJob = clusterJobService.createClusterJob(realm, job.jobID, realm.unixUser, processingStep, seqType, jobName)
        threadLog?.info("Log file: ${AbstractOtpJob.getDefaultLogFilePath(clusterJob).path}")

        return job.jobID
    }

    /**
     * Returns a map of jobs PBS knows about
     *
     * @param realm The realm to connect to
     * @param userName The name of the user whose jobs should be checked
     * @return A map containing job identifiers and their status
     */
    public Map<ClusterJobIdentifier, ClusterJobStatus> retrieveKnownJobsWithState(Realm realm, String userName) throws Exception {
        assert realm : "No realm specified."
        assert userName : "No user name specified."
        Map<ClusterJobIdentifier, ClusterJobStatus> jobStates = [:]
        String endString = HelperUtils.getRandomMd5sum()
        // print a string at the end so we know we get the whole output
        ProcessOutput out = executionService.executeCommandReturnProcessOutput(realm,
                "qstat -u ${userName} && echo ${endString}", userName)
        if(out.exitCode != 0 || out.stderr != "") {
            throw new IllegalStateException("qstat returned error, exit code: '${out.exitCode}', stderr: '${out.stderr}'")
        }
        validateQstatResult(out.stdout, endString)
        out.stdout.eachLine { String line ->
            Matcher matcher = line =~ JOB_LIST_PATTERN
            if (matcher) {
                jobStates.put(new ClusterJobIdentifier(realm, matcher.group(1), userName),
                        ClusterJobStatus.getStatusByCode(matcher.group(2)))
            }
        }
        return jobStates
    }

    protected static void validateQstatResult(String out, String endString) {
        List<String> lines = out.readLines()

        if(!((lines.size() == 1 && lines[0] == endString) || (
                lines[0] == "" &&
                lines[1] =~ /^.*:\s*$/ &&
                lines[2] =~ /^\s+Req'd\s+Req'd\s+Elap\s*$/ &&
                lines[3] =~ /^Job ID\s+Username\s+Queue\s+Jobname\s+SessID\s+NDS\s+TSK\s+Memory\s+Time\s+S\s+Time\s*$/ &&
                lines[4] =~ /^(-+\s+){10}-+\s*$/ &&
                lines.subList(5, lines.size() - 1).every { String line ->
                    line =~ JOB_LIST_PATTERN
                } &&
                lines[lines.size() - 1] == endString
        ))) {
            throw new IllegalStateException("qstat output doesn't match expected output: '${out}'")
        }
    }

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

    private BatchEuphoriaJobManager getJobManager(Realm realm) {
        BatchEuphoriaJobManager manager = managerPerRealm[realm]

        if (manager == null) {
            JobManagerCreationParameters jobManagerParameters = new JobManagerCreationParametersBuilder()
                    .setCreateDaemon(false)
                    .setUserIdForJobQueries(realm.unixUser)
                    .setTrackUserJobsOnly(true)
                    .setTrackOnlyStartedJobs(false)
                    .setUserMask("027")
                    .build()

            if (realm.jobScheduler == Realm.JobScheduler.PBS) {
                manager = new PBSJobManager(new BEExecutionServiceAdapter(executionService, realm), jobManagerParameters)
            } else {
                throw new Exception("Unsupported cluster job scheduler")
            }
            managerPerRealm[realm] = manager
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
