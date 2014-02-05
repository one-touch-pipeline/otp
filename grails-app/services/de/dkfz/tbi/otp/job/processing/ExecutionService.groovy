package de.dkfz.tbi.otp.job.processing

import java.util.regex.Matcher
import java.util.regex.Pattern
import org.apache.commons.logging.Log
import com.jcraft.jsch.Channel
import com.jcraft.jsch.ChannelExec
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import de.dkfz.tbi.otp.ngsdata.Realm
import de.dkfz.tbi.otp.utils.logging.LogThreadLocal

/**
 * @short Helper class providing functionality for remote execution of jobs.
 *
 * Provides connection to a remote host via ssh and validation of
 * pbs ids. Specifically the remote hosts are PBS' meaning that a
 * format specific for usage on a PBS is built and executed.
 *
 */
class ExecutionService {

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
     * @return what the server sends back
     * @throws NullPointerException if a job identifier is provided, but no PBS option is defined for this
     *          job identifier and the cluster ({@link Realm#cluster}) of the {@link Realm}
     * @see PbsOptionMergingService#mergePbsOptions(Realm, String)
     */
    public String executeJob(Realm realm, String text, String jobIdentifier = null) {
        if (!text) {
            throw new ProcessingException("No job text specified.")
        }
        String pbsOptions = pbsOptionMergingService.mergePbsOptions(realm, jobIdentifier)
        text = """
# OTP: Fail on first non-zero exit code
set -e

# BEGIN ORIGINAL SCRIPT
${text}
# END ORIGINAL SCRIPT
"""
        String command = "echo '${text}' | qsub " + pbsOptions
        List<String> values = executeRemoteJob(realm, command)
        return concatResults(values)
    }

    /**
     * Executes a script on a specified host
     *
     * @param realm The realm which identifies the host
     * @param filePath The path of the file to be executed
     * @return what the server sends back
     */
    @Deprecated
    public String executeJobScript(Realm realm, String filePath) {
        if (!filePath || filePath == "") {
            throw new ProcessingException("No file path specified.")
        }
        File file = new File(filePath)
        List<String> values = executeRemoteJob(realm, null, file)
        return concatResults(values)
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
    private List<String> querySsh(String host, int port, int timeout, String username, String password, String command = null, File script = null, String options) {
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
        while (true){
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
        boolean retVal = true
        try {
            retVal = isJobPending(executeCommand(realm, "qstat ${pbsId}"))
        } catch (Exception e) {
            /*
             * Catch all exceptions, which can appear during the check if the job is still running.
             * If an exception is thrown it is assumed that the job is still running,
             * since it can appear when it is not possible to connect to the server
             */
            log.info("An Exception was thrown in checkRunningJob due to the following reason: ", e)
        }
        return retVal
    }

    /**
     * Verifies if a job status is still pending (Not completed)
     *
     * @param String containing output of a pbs "qstat jobId" command
     * @return {@code true} if job is pending, otherwise {@code false}.
     */
    private boolean isJobPending(String output) {
        boolean pending = false
        if (isJobStatusAvailable(output)) {
            pending = !isExistingJobCompleted(output)
        }
        return pending
    }

    private boolean isJobStatusAvailable(String output) {
        Pattern pattern = Pattern.compile("\\s*Job id\\s*Name\\s*User.*", Pattern.CASE_INSENSITIVE)
        boolean valid = false
        output.eachLine { String line ->
            Matcher m = pattern.matcher(line)
            if (m.find()) {
                valid = true
            }
        }
        return valid
    }

    private boolean isExistingJobCompleted(String output) {
        return ClusterJobStatus.COMPLETED.value == existingJobStatus(output)
    }

    private String existingJobStatus(String output) {
        assert(isJobStatusAvailable(output))
        final int STATUS_INDEX = 4
        List<String> lines = output.readLines()
        String jobStatus = lines.last()
        return jobStatus.split()[STATUS_INDEX]
    }

    private void logToJob(String message) {
        Log log = LogThreadLocal.getJobLog()
        log?.debug message
    }
}
