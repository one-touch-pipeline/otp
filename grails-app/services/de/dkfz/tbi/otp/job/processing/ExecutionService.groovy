package de.dkfz.tbi.otp.job.processing

import static org.springframework.util.Assert.notNull
import de.dkfz.tbi.otp.dataprocessing.ProcessingPriority

import java.util.regex.Matcher
import java.util.regex.Pattern
import org.apache.commons.logging.Log
import com.jcraft.jsch.Channel
import com.jcraft.jsch.ChannelExec
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import de.dkfz.tbi.otp.infrastructure.ClusterJob
import de.dkfz.tbi.otp.infrastructure.ClusterJobService
import de.dkfz.tbi.otp.job.scheduler.SchedulerService
import de.dkfz.tbi.otp.ngsdata.Realm
import de.dkfz.tbi.otp.ngsdata.SeqType
import de.dkfz.tbi.otp.utils.logging.LogThreadLocal
import static org.springframework.util.Assert.*
import static de.dkfz.tbi.otp.utils.CollectionUtils.*

/**
 * @short Helper class providing functionality for remote execution of jobs.
 *
 * Provides connection to a remote host via ssh and validation of
 * pbs ids. Specifically the remote hosts are PBS' meaning that a
 * format specific for usage on a PBS is built and executed.
 *
 */
class ExecutionService {

    //Job id                    Name             User            Time Use S Queue
    public static final String KNOWN_JOB_ID_PATTERN = /(i?)\s*Job\sid\s*Name\s*User.*/
    //qstat: Unknown Job Id 22.headnode.long-domain
    public static final String UNKNOWN_JOB_ID_PATTERN = /(i?)\s*qstat:\s*Unknown\sJob\sId\s\d*.*/

    enum ClusterJobStatus {
        COMPLETED("C"),
        HELD("H"),
        RUNNING("R"),
        QUEUED("Q")

        private final String value

        public String value() {
            return value
        }

        ClusterJobStatus(String value) {
            this.value = value
        }

        public boolean equals(String value) {
            return this.value == value
        }
    }

    /**
     * Dependency injection of grailsApplication
     */
    @SuppressWarnings("GrailsStatelessService")
    def grailsApplication
    /**
     * Dependency injection of config service
     */
    @SuppressWarnings("GrailsStatelessService")
    def configService

    PbsOptionMergingService pbsOptionMergingService
    JobStatusLoggingService jobStatusLoggingService
    SchedulerService schedulerService
    ClusterJobService clusterJobService

    /**
     * Executes a command on a specified host
     *
     * The host the command is to be executed
     * is identified by the realm.
     * @param realm The realm which identifies the host
     * @param command The command to be executed
     * @return what the server sends back
     */
    public String executeCommand(Realm realm, String command) {
        if (!command) {
            throw new ProcessingException("No command specified")
        }
        List<String> values = executeRemoteJob(realm, command)
        return concatResults(values)
    }

    /**
     * Executes a job on a specified host.
     * It uses {@link PbsOptionMergingService#mergePbsOptions(Realm, String)}
     * with the given realm and jobIdentifier to create the merged PBS options String to pass to qsub command.
     * If a job key is given, a {@link ProcessingOption} for the cluster defined by the realm
     * has to exist, otherwise a {@link NullPointerException} is thrown.
     *
     * @param realm The realm which identifies the host
     * @param text The script to be run a pbs system
     * @param jobIdentifier the name of a job to take job-cluster specific parameters
     * @param qsubParameter The parameter which are needed for some qsub commands and can not be included in the text parameter
     * The qsubParameter must be in JSON format!
     * @return what the server sends back
     * @throws NullPointerException if a job identifier is provided, but no PBS option is defined for this
     *          job identifier and the cluster ({@link Realm#cluster}) of the {@link Realm}
     * @see PbsOptionMergingService#mergePbsOptions(Realm, String)
     */
    public String executeJob(Realm realm, String text, String jobIdentifier = null, String qsubParameters = "") {
        if (!text) {
            throw new ProcessingException("No job text specified.")
        }
        notNull realm, 'No realm specified.'

        ProcessingStep processingStep = schedulerService.jobExecutedByCurrentThread.processingStep
        def domainObject = atMostOneElement(ProcessParameter.findAllByProcess(processingStep.process))?.toObject()

        SeqType seqType = domainObject?.seqType
        short processingPriority = domainObject?.processingPriority ?: ProcessingPriority.NORMAL_PRIORITY


        // check if the project has FASTTRACK priority
        String fastTrackParameter
        if (processingPriority >= ProcessingPriority.FAST_TRACK_PRIORITY) {
            fastTrackParameter = '{"-A": "FASTTRACK"}'
        }

        String pbsOptions = pbsOptionMergingService.mergePbsOptions(realm, jobIdentifier, qsubParameters, fastTrackParameter)

        String pbsJobDescription = processingStep.getPbsJobDescription()
        String scriptText = """
#PBS -S /bin/bash
#PBS -N ${pbsJobDescription}

# OTP: Fail on first non-zero exit code
set -e

umask 0027

# BEGIN ORIGINAL SCRIPT
${text}
# END ORIGINAL SCRIPT
"""
        /*
         * Only log job status if a processing step is passed (to be backwards-compatible). Getting a processing
         * step should always be the case when a closure is used. Other jobs need to be adopted to do this explicitly.
         * The log file will be locked at the time of writing so concurrent (cluster) jobs will not corrupt the file.
         */
        if (processingStep) {
            String logFile = jobStatusLoggingService.logFileLocation(realm, processingStep)
            String logMessage = jobStatusLoggingService.constructMessage(processingStep)

            scriptText += """
touch '${logFile}'
chmod 0640 ${logFile}
flock -x '${logFile}' -c "echo \\"${logMessage}\\" >> '${logFile}'"
"""
        }
        String command = "echo '${scriptText}' | qsub " + pbsOptions
        List<String> values = executeRemoteJob(realm, command)


        String concatenatedValues = concatResults(values)

        String pbsId
        try {
            pbsId = exactlyOneElement(extractPbsIds(concatenatedValues))
        } catch (AssertionError e) {
            throw new RuntimeException("Could not extract exactly one pbs id from '${concatenatedValues}'", e)
        }
        logToJob("cluster log files: \noutput: ${pbsJobDescription}.o${pbsId} \nerror: ${pbsJobDescription}.e${pbsId}")
        ClusterJob clusterJob = clusterJobService.createClusterJob(realm, pbsId, processingStep, seqType, pbsJobDescription)

        return concatenatedValues
    }

    /**
     * Triggers the sending of remote jobs
     *
     * Commands are executed on remote servers using a pbs infrastructure.
     * The host can be specified and so the service is flexible for
     * executing on several different servers. Either command or script
     * have to be specified otherwise it does not work and an exception
     * is thrown.
     *
     * @param host The host on which the command shall be executed
     * @param port The port to be addressed on the server
     * @param timeout The timeout in milliseconds after which execution interrupts
     * @param username The user name for the connection
     * @param password The password for the user
     * @param command The command to be executed on the remote server
     * @param script The script to be executed on the remote server
     * @param options The options To make the command more specific
     *
     * @return List of Strings containing the output of the triggered remote job
     */

    private List<String> executeRemoteJob(Realm realm, String command = null, File script = null) {
        if (!command && !script) {
            throw new ProcessingException("Neither a command nor a script specified to be run remotely.")
        }
        String password = configService.getPbsPassword()
        return querySsh(realm.host, realm.port, realm.timeout, realm.unixUser, password, command, script, realm.pbsOptions)
    }

    /**
     * Queries ssh on a pbs infrastructure
     *
     * Opens an ssh connection to a specified host with specific credentials.
     * With the parameter options can options for the command be specified.
     *
     * @param host The host on which the command shall be executed
     * @param port The port to be addressed on the server
     * @param timeout The timeout to use for the ssh connection
     * @param username The user name for the connection
     * @param password The password for the user
     * @param command The command to be executed on the remote server
     * @param script The script to be executed on the remote server
     * @param options The options To make the command more specific
     * @return List of Strings containing the output of the executed job
     */
    private List<String> querySsh(String host, int port, int timeout, String username, String password, String command, File script, String options) {
        if (!password) {
            throw new ProcessingException("No password for remote connection specified.")
        }
        try {
            JSch jsch = new JSch()
            Session session = jsch.getSession(username, host, port)
            session.setPassword(password)
            session.setTimeout(timeout)
            java.util.Properties config = new java.util.Properties()
            config.put("StrictHostKeyChecking", "no")
            session.setConfig(config)
            session.connect()
            Channel channel = session.openChannel("exec")
            if (command) {
                logToJob("executed command: " + command)
                ((ChannelExec)channel).setCommand(command)
            } else if (script) {
                command = "qsub"
                if (options) {
                    command += " ${options}"
                }
                logToJob("executed script: " + script + " with command: " + command)
                ((ChannelExec)channel).setCommand(command)
                ((ChannelExec)channel).setInputStream(script.newInputStream())
            }
            ((ChannelExec)channel).setErrStream(System.err)
            List<String> values = getInputStream(channel)
            if (values == null) {
                // TODO: How to handle this?
                throw new ProcessingException("test!")
            }
            logToJob("received response: " + concatResults(values))
            disconnectSsh(channel)
            return values
        } catch (Exception e) {
            log.info(e.toString(), e)
            throw new ProcessingException(e)
        }
    }

    /**
     * Disconnects channel and session to have a clear disconnect
     * from the remote host
     *
     * @param channel The channel to be disconnected
     */
    private void disconnectSsh(Channel channel) {
        channel.session.disconnect()
        channel.disconnect()
    }

    /**
     * Retrives the input stream and converts it to a List of Strings
     *
     * @param channel The channel to read the input stream from
     * @return List of Strings containing the returned stream
     */
    private List<String> getInputStream(Channel channel) {
        InputStream inputStream = channel.getInputStream()
        channel.connect()
        byte[] tmp = new byte[1024]
        List<String> values = []
        while (true) {
            while (inputStream.available() > 0) {
                int i = inputStream.read(tmp, 0, 1024)
                if (i < 0) {
                    break
                }
                values.add(new String(tmp, 0, i))
            }
            if (channel.isClosed()) {
                break
            }
            try {
                Thread.sleep(1000)
            } catch(Exception ee){
            }
        }
        return values
    }

    private String concatResults(List<String> values) {
        String answer = ""
        values.each { String value ->
            if (value) {
                answer += value
            }
        }
        return answer
    }

    /**
     * Extracts pbs ids from a given String
     *
     * @param sshOutput List of Strings containing output of ssh session from pbs
     * @return List of Strings each of them a pbs id
     */
    public List<String> extractPbsIds(String sshOutput) {
        Pattern pattern = Pattern.compile("\\d+")
        List<String> pbsIds = []
        sshOutput.eachLine { String line ->
            Matcher m = pattern.matcher(line)
            if (m.find()) {
                pbsIds.add(m.group())
            }
            else {
                return null
            }
        }
        return pbsIds
    }

    /**
     * Validates if jobs of which the pbs ids are are handed over are running
     *
     * @param pbsIds Pbs ids to be validated
     * @return Map of pbs ids with associated validation identifiers, which are Boolean values
     */
    public Map<String, Boolean> validate(List<String> pbsIds) {
        if (!pbsIds) {
            throw new InvalidStateException("No pbs ids handed over to be validated.")
        }
        // TODO: improve algorithm to query PBS once
        List<Realm> realms = Realm.list()
        Map<String, Boolean> stats = [:]
        for (String pbsId in pbsIds) {
            stats.put(pbsId, false)
            for (Realm realm in realms) {
                if (checkRunningJob(pbsId, realm)) {
                    stats.put(pbsId, true)
                    // no need to query further Realms, it's running
                    break
                }
            }
        }
        return stats
    }

    /**
     * Checks whether the given PBS Ids are running on the given PBS Realm.
     * @param pbsIds The list of PBS Ids to query for
     * @param realm The PBS Realm which should be checked
     * @return Map of pbs ids with associated validation identifiers, which are Boolean values
     */
    public Map<String, Boolean> checkRunning(List<String> pbsIds, Realm realm) {
        if (!pbsIds) {
            throw new InvalidStateException("No pbs ids handed over to be validated.")
        }
        Map<String, Boolean> stats = [:]
        for (String pbsId in pbsIds) {
            stats.put(pbsId, checkRunningJob(pbsId, realm))
        }
        return stats
    }

    /**
     * Checks whether the given pbsId is pending on the given Realm
     * @param pbsId The PBS Job Id to check whether it is pending
     * @param realm The PBS Realm on which it should be checked whether the Job pending
     * @return true if pending, false otherwise
     */
    public boolean checkRunningJob(String pbsId, Realm realm) {
        boolean isRunning = true
        try {
            String qstatOut = executeCommand(realm, "qstat ${pbsId} 2>&1")
            if(qstatOut =~ KNOWN_JOB_ID_PATTERN) {
                isRunning = !isExistingJobCompleted(qstatOut)
            } else if(qstatOut =~ UNKNOWN_JOB_ID_PATTERN) {
                isRunning = false
            } else {
                if(qstatOut == '') {
                    log.info("qstat returned empty result.")
                } else {
                    log.info("qstat returned ${qstatOut}")
                }
                isRunning = true
            }
        } catch (Exception e) {
            /*
             * Catch all exceptions, which can appear during the check if the job is still running.
             * If an exception is thrown it is assumed that the job is still running,
             * since it can appear when it is not possible to connect to the server
             */
            log.info("An Exception was thrown in checkRunningJob due to the following reason: ", e)
        }
        return isRunning
    }

    private boolean isExistingJobCompleted(String output) {
        return ClusterJobStatus.COMPLETED.value == existingJobStatus(output)
    }

    private String existingJobStatus(String output) {
        final int STATUS_INDEX = 4
        List<String> lines = output.readLines()
        String jobStatus = lines.last()
        return jobStatus.split()[STATUS_INDEX]
    }

    private void logToJob(String message) {
        Log log = LogThreadLocal.getThreadLog()
        log?.debug message
    }
}
