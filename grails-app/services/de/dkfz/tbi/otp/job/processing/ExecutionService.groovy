package de.dkfz.tbi.otp.job.processing


import com.jcraft.jsch.Channel
import com.jcraft.jsch.ChannelExec
import com.jcraft.jsch.JSch
import com.jcraft.jsch.JSchException
import com.jcraft.jsch.Session
import com.jcraft.jsch.IdentityRepository
import com.jcraft.jsch.agentproxy.Connector
import com.jcraft.jsch.agentproxy.ConnectorFactory
import com.jcraft.jsch.agentproxy.RemoteIdentityRepository
import de.dkfz.tbi.otp.ngsdata.Realm
import de.dkfz.tbi.otp.utils.logging.LogThreadLocal
import org.apache.commons.logging.Log
import de.dkfz.tbi.otp.utils.ProcessHelperService.ProcessOutput


/**
 * @short Helper class providing functionality for remote execution of jobs.
 *
 * Provides connection to a remote host via SSH
 */
class ExecutionService {

    @SuppressWarnings("GrailsStatelessService")
    def configService


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
        try {
            JSch jsch = new JSch()

            if (keyFile) {
                jsch.addIdentity(keyFile.absolutePath)

                if (useSshAgent) {
                    Connector connector = ConnectorFactory.getDefault().createConnector()
                    if (connector != null ) {
                        IdentityRepository repository = new RemoteIdentityRepository(connector)
                        jsch.setIdentityRepository(repository)
                    }
                }
            }

            Session session = jsch.getSession(username, host, port)
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

            disconnectSsh(channel)
            return processOutput
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
    private static void disconnectSsh(Channel channel) {
        channel.session.disconnect()
        channel.disconnect()
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
