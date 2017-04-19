package de.dkfz.tbi.otp.job.processing

import com.jcraft.jsch.*
import com.jcraft.jsch.agentproxy.*
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.ProcessHelperService.ProcessOutput
import de.dkfz.tbi.otp.utils.logging.*
import groovy.transform.*
import org.apache.commons.logging.*

import java.util.concurrent.*

/**
 * @short Helper class providing functionality for remote execution of jobs.
 *
 * Provides connection to a remote host via SSH
 */
class ExecutionService {

    static final String MAX_SSH_CALLS = "MaximumParallelSshCalls"

    @SuppressWarnings("GrailsStatelessService")
    def configService

    private JSch jsch

    private Map<String, Session> sessionPerUser = [:]

    private static Semaphore maxSshCalls

    /**
     * Executes a command on a specified host and logs stdout and stderr
     *
     * @param realm The realm which identifies the host
     * @param command The command to be executed
     * @param userName The user name used to log in (optional; if not set, unixUser of realm is used)
     * @return standard output of the command executed
     */
    public String executeCommand(Realm realm, String command, String userName = null) {
        return executeCommandReturnProcessOutput(realm, command, userName).stdout
    }


    /**
     * Executes a command on a specified host and logs stdout and stderr
     *
     * @param realm The realm which identifies the host
     * @param command The command to be executed
     * @param userName The user name used to log in (optional; if not set, unixUser of realm is used)
     * @return process output of the command executed
     */

    public ProcessOutput executeCommandReturnProcessOutput(Realm realm, String command, String userName = null) {
        assert realm : "No realm specified."
        assert command : "No command specified to be run remotely."
        String password = configService.getPbsPassword()
        File keyFile = configService.getSshKeyFile()
        boolean useSshAgent = configService.useSshAgent()
        return querySsh(realm.host, realm.port, realm.timeout, userName ?: realm.unixUser, password, keyFile, useSshAgent, command)
    }

    /**
     * Executes a command on a specified host and logs stdout and stderr
     *
     * Opens an SSH connection to a specified host with specific credentials.
     *
     * @param host The host on which the command shall be executed
     * @param port The port to be addressed on the server
     * @param timeout The timeout to use for the ssh connection
     * @param username The user name for the connection
     * @param password The password for the user
     * @param keyFile The key file which contains the SSH key for passwordless login
     * @param useSshAgent Whether the SSH agent should be used to decrypt the SSH key
     * @param command The command to be executed on the remote server
     * @return process output of the command executed
     */
    protected ProcessOutput querySsh(String host, int port, int timeout, String username, String password, File keyFile, boolean useSshAgent, String command) {
        assert command : "No command specified."
        if (!password && !keyFile) {
            throw new ProcessingException("Neither password nor key file for remote connection specified.")
        }
        if (!maxSshCalls) {
            maxSshCalls = new Semaphore((int)ProcessingOptionService.findOptionAsNumber(MAX_SSH_CALLS, null, null, 30), true)
        }
        maxSshCalls.acquire()
        try {
            Session session = connectSshIfNeeded(host, port, timeout, username, password, keyFile, useSshAgent)

            ChannelExec channel = (ChannelExec)session.openChannel("exec")
            logToJob("executed command: " + command)
            channel.setCommand(command)

            ProcessOutput processOutput = getOutput(channel)

            if (processOutput.exitCode != 0) {
                logToJob("received exit code:\n" + processOutput.exitCode)
            }
            logToJob("received response:\n" + processOutput.stdout)
            if (processOutput.stderr) {
                logToJob("received error response:\n" + processOutput.stderr)
            }
            channel.disconnect()
            return processOutput
        } catch (Exception e) {
            log.info(e.toString(), e)
            throw new ProcessingException(e)
        } finally {
            maxSshCalls.release()
        }
    }

    private Session connectSshIfNeeded(String host, int port, int timeout, String username, String password, File keyFile, boolean useSshAgent) {
        Session session = sessionPerUser[username]
        if (session == null || !session.isConnected()) {
            session = createSessionAndJsch(host, port, timeout, username, password, keyFile, useSshAgent)
        }
        return session
    }

    @Synchronized
    private Session createSessionAndJsch(String host, int port, int timeout, String username, String password, File keyFile, boolean useSshAgent) {
        Session session = sessionPerUser[username]
        if (session == null || !session.isConnected()) {
            log.info("create new session for ${username}")
            if (jsch == null) {
                log.info("create new jsch")
                jsch = new JSch()
                if (keyFile) {
                    jsch.addIdentity(keyFile.absolutePath)

                    if (useSshAgent) {
                        Connector connector = ConnectorFactory.getDefault().createConnector()
                        if (connector != null) {
                            IdentityRepository repository = new RemoteIdentityRepository(connector)
                            jsch.setIdentityRepository(repository)
                        }
                    }
                }
            }

            session = jsch.getSession(username, host, port)
            if (!keyFile) {
                session.setPassword(password)
            }
            session.setTimeout(timeout)
            Properties config = new Properties()
            config.put("StrictHostKeyChecking", "no")
            if (keyFile) {
                config.put("PreferredAuthentications", "publickey")
            }
            session.setConfig(config)
            try {
                session.connect()
            } catch (JSchException e) {
                throw new ProcessingException("Connecting to ${host}:${port} with username ${username} failed.", e)
            }
            sessionPerUser[username] = session
        }
        return session
    }




    /**
     * Retrieves the command output
     *
     * @param channel The channel to read from
     * @return The output of the finished process
     */
    private static ProcessOutput getOutput(ChannelExec channel) {
        OutputStream outputErrorStream = new ByteArrayOutputStream()
        OutputStream outputStream = new ByteArrayOutputStream()
        channel.setOutputStream(outputStream)
        channel.setErrStream(outputErrorStream)

        channel.connect()
        while(!channel.isClosed()) {
            Thread.sleep(10)
        }
        return new ProcessOutput(
                outputStream.toString("UTF-8"),
                outputErrorStream.toString("UTF-8"),
                channel.getExitStatus()
        )
    }

    private void logToJob(String message) {
        Log log = LogThreadLocal.getThreadLog()
        log?.debug message
    }
}
