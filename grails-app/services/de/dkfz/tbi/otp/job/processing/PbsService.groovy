package de.dkfz.tbi.otp.job.processing

import de.dkfz.tbi.otp.dataprocessing.ProcessingPriority
import de.dkfz.tbi.otp.infrastructure.ClusterJob
import de.dkfz.tbi.otp.infrastructure.ClusterJobIdentifier
import de.dkfz.tbi.otp.infrastructure.ClusterJobService
import de.dkfz.tbi.otp.job.scheduler.SchedulerService
import de.dkfz.tbi.otp.ngsdata.Realm
import de.dkfz.tbi.otp.ngsdata.SeqType
import de.dkfz.tbi.otp.utils.HelperUtils
import de.dkfz.tbi.otp.utils.ProcessHelperService.ProcessOutput
import groovy.transform.TupleConstructor

import java.util.regex.Matcher

import static de.dkfz.tbi.otp.utils.logging.LogThreadLocal.getThreadLog
import static org.springframework.util.Assert.notNull


/**
 * This class contains methods to communicate with the PBS system.
 *
 * It executes PBS commands on the cluster head node using {@link ExecutionService}.
 */
class PbsService {

    PbsOptionMergingService pbsOptionMergingService
    JobStatusLoggingService jobStatusLoggingService
    SchedulerService schedulerService
    ClusterJobService clusterJobService
    ExecutionService executionService


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
     * Executes a job on a specified host.
     *
     * @param realm The realm which identifies the host
     * @param script The script to be run on the PBS system
     * @param qsubParameters The parameters which are needed for some qsub commands and can not be included in the script parameter, must be in JSON format
     * @return what the server sends back
     */
    public String executeJob(Realm realm, String script, String qsubParameters = "") {
        if (!script) {
            throw new ProcessingException("No job script specified.")
        }
        notNull realm, 'No realm specified.'

        ProcessingStep processingStep = schedulerService.jobExecutedByCurrentThread.processingStep
        ProcessParameterObject domainObject = processingStep.processParameterObject

        SeqType seqType = domainObject?.seqType
        short processingPriority = domainObject?.processingPriority ?: ProcessingPriority.NORMAL_PRIORITY


        // check if the project has FASTTRACK priority
        String fastTrackParameter
        if (processingPriority >= ProcessingPriority.FAST_TRACK_PRIORITY) {
            fastTrackParameter = '{"-A": "FASTTRACK"}'
        }

        String pbsOptions = pbsOptionMergingService.mergePbsOptions(processingStep, realm, qsubParameters, fastTrackParameter)

        String pbsJobDescription = processingStep.getPbsJobDescription()
        String logFile = jobStatusLoggingService.logFileLocation(realm, processingStep)
        String logMessage = jobStatusLoggingService.constructMessage(processingStep)
        String scriptText = """
#PBS -S /bin/bash
#PBS -N ${pbsJobDescription}

# OTP: Fail on first non-zero exit code
set -e

umask 0027

# BEGIN ORIGINAL SCRIPT
${script}
# END ORIGINAL SCRIPT

touch '${logFile}'
chmod 0640 ${logFile}
flock -x '${logFile}' -c "echo \\"${logMessage}\\" >> '${logFile}'"
"""

        String command = "echo '${scriptText}' | qsub " + pbsOptions
        ProcessOutput output = executionService.executeCommandReturnProcessOutput(realm, command)
        String pbsId = extractPbsId(output.stdout)

        ClusterJob clusterJob = clusterJobService.createClusterJob(realm, pbsId, realm.unixUser, processingStep, seqType, pbsJobDescription)
        threadLog?.info(AbstractOtpJob.getLogFileNames(clusterJob))

        return pbsId
    }


    /**
     * Extracts the PBS ID from a given string
     *
     * @param pbsOutput standard output from qsub
     * @return PBS ID as a string
     * @throw RuntimeException if pbsOutput doesn't contain exactly one PBS ID
     */
    protected static String extractPbsId(String pbsOutput) {
        Matcher pbsId = pbsOutput =~ /^(\d+)\.[^\n]+\n?$/
        if (!pbsId) {
            throw new RuntimeException("Could not extract exactly one pbs id from '${pbsOutput}'")
        }
        return pbsId.group(1)
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
}
