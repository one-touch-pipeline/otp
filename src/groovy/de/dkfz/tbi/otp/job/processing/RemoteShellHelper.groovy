package de.dkfz.tbi.otp.job.processing

import com.jcraft.jsch.*
import com.jcraft.jsch.agentproxy.*
import de.dkfz.tbi.otp.config.*
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.LocalShellHelper
import de.dkfz.tbi.otp.utils.LocalShellHelper.ProcessOutput
import de.dkfz.tbi.otp.utils.logging.*
import groovy.transform.*
import org.apache.commons.logging.*
import org.springframework.scheduling.annotation.*
import org.springframework.beans.factory.annotation.*
import org.springframework.context.annotation.*
import org.springframework.stereotype.*

import java.util.concurrent.*

/**
 * @short Helper class providing functionality for remote execution of jobs.
 *
 * Provides connection to a remote host via SSH
 *
 * @see LocalShellHelper
 */
@Scope("singleton")
@Component
class RemoteShellHelper {

    static private final int TIME_FOR_RETRY_REMOTE_ACCESS = 10 * 60

    static private final int CHANNEL_TIMEOUT = 5 * 60


    @Autowired
    ConfigService configService

    @Autowired
    ProcessingOptionService processingOptionService


    private JSch jsch

    private Map<Realm, Session> sessionPerRealm = [:]

    private static Semaphore maxSshCalls

    /**
     * Executes a command on a specified host and logs stdout and stderr
     *
     * @param realm The realm which identifies the host
     * @param command The command to be executed
     * @return standard output of the command executed
     */
    String executeCommand(Realm realm, String command) {
        return executeCommandReturnProcessOutput(realm, command).stdout
    }

    /**
     * Executes a command on a specified host and logs stdout and stderr
     *
     * @param realm The realm which identifies the host
     * @param command The command to be executed
     * @return process output of the command executed
     */
    ProcessOutput executeCommandReturnProcessOutput(Realm realm, String command) {
        assert realm: "No realm specified."
        assert command: "No command specified to be run remotely."
        String sshUser = configService.getSshUser()
        String password = configService.getSshPassword()
        File keyFile = configService.getSshKeyFile()
        SshAuthMethod sshAuthMethod = configService.getSshAuthenticationMethod()
        try {
            return querySsh(realm, sshUser, password, keyFile, sshAuthMethod, command)
        } catch (ProcessingException e) {
            if (e.cause && e.cause instanceof JSchException && e.cause.message.contains('channel is not opened.')) {
                logToJob("'channel is not opened' error occur, try again in ${TIME_FOR_RETRY_REMOTE_ACCESS} seconds")
                log.error("'channel is not opened' error occur, try again in ${TIME_FOR_RETRY_REMOTE_ACCESS} seconds")
                Thread.sleep(TIME_FOR_RETRY_REMOTE_ACCESS * 1000)
                return querySsh(realm, sshUser, password, keyFile, sshAuthMethod, command)
            } else {
                throw e
            }
        }
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
     * @param sshAuthMethod The authentication method used for the SSH connection
     * @param command The command to be executed on the remote server
     * @return process output of the command executed
     */
    protected ProcessOutput querySsh(Realm realm, String username, String password, File keyFile, SshAuthMethod sshAuthMethod, String command) {
        assert command: "No command specified."
        if (!password && !keyFile) {
            throw new ProcessingException("Neither password nor key file for remote connection specified.")
        }
        if (!maxSshCalls) {
            synchronized (this) {
                if (!maxSshCalls) {
                    maxSshCalls = new Semaphore(processingOptionService.findOptionAsInteger(ProcessingOption.OptionName.MAXIMUM_PARALLEL_SSH_CALLS), true)
                }
            }
        }
        maxSshCalls.acquire()
        try {
            Session session = connectSshIfNeeded(realm, username, password, keyFile, sshAuthMethod)

            ChannelExec channel = (ChannelExec) session.openChannel("exec")
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
            synchronized (this) {
                sessionPerRealm.remove(realm)
            }
            throw new ProcessingException(e)
        } finally {
            maxSshCalls.release()
        }
    }

    private Session connectSshIfNeeded(Realm realm, String username, String password, File keyFile, SshAuthMethod sshAuthMethod) {
        Session session = sessionPerRealm[realm]
        if (session == null || !session.isConnected()) {
            session = createSessionAndJsch(realm, username, password, keyFile, sshAuthMethod)
        }
        return session
    }

    @Synchronized
    private Session createSessionAndJsch(Realm realm, String username, String password, File keyFile, SshAuthMethod sshAuthMethod) {
        Session session = sessionPerRealm[realm]
        if (session == null || !session.isConnected()) {
            log.info("create new session for ${username}")
            Properties config = new Properties()
            if (jsch == null) {
                log.info("create new jsch")
                jsch = new JSch()

                switch (sshAuthMethod) {
                    case SshAuthMethod.KEY_FILE:
                        jsch.addIdentity(keyFile.absolutePath)
                        config.put("PreferredAuthentications", "publickey")
                        break
                    case SshAuthMethod.SSH_AGENT:
                        Connector connector = ConnectorFactory.getDefault().createConnector()
                        if (connector != null) {
                            IdentityRepository repository = new RemoteIdentityRepository(connector)
                            jsch.setIdentityRepository(repository)
                        }
                        break
                }
            }

            session = jsch.getSession(username, realm.host, realm.port)
            if (sshAuthMethod == SshAuthMethod.PASSWORD) {
                session.setPassword(password)
            }
            session.setTimeout(realm.timeout)
            config.put("StrictHostKeyChecking", "no")
            session.setConfig(config)
            try {
                session.connect()
            } catch (JSchException e) {
                throw new ProcessingException("Connecting to ${realm.host}:${realm.port} with username ${username} failed.", e)
            }
            sessionPerRealm[realm] = session
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

        channel.connect(CHANNEL_TIMEOUT * 1000)
        while (!channel.isClosed()) {
            Thread.sleep(10)
        }
        return new ProcessOutput(
                outputStream.toString("UTF-8"),
                outputErrorStream.toString("UTF-8"),
                channel.getExitStatus()
        )
    }

    @Scheduled(fixedDelay = 60000l)
    void keepAlive() {
        sessionPerRealm.each { Realm realm, Session session ->
            log.debug("Send keep alive for ${realm}")
            try {
                session.sendKeepAliveMsg()
            } catch (Throwable e) {
                log.error("Send keep alive failed for ${realm} ${session}", e)
                synchronized (this) {
                    sessionPerRealm.remove(realm)
                }
            }
        }
    }

    private void logToJob(String message) {
        Log log = LogThreadLocal.getThreadLog()
        log?.debug message
    }
}
