/*
 * Copyright 2011-2019 The OTP authors
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

import com.jcraft.jsch.*
import com.jcraft.jsch.agentproxy.*
import groovy.transform.Synchronized
import groovy.util.logging.Slf4j
import org.slf4j.Logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.config.SshAuthMethod
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.ngsdata.Realm
import de.dkfz.tbi.otp.utils.*
import de.dkfz.tbi.otp.utils.logging.LogThreadLocal

import java.util.concurrent.Semaphore

/**
 * @short Helper class providing functionality for remote execution of jobs.
 *
 * Provides connection to a remote host via SSH
 *
 * @see LocalShellHelper
 */
@Component
@Slf4j
class RemoteShellHelper {

    static private final int FACTOR_10 = 10

    static private final int MILLI_SECONDS_PER_SECOND = 1000

    static private final int SECONDS_PER_MINUTE = 60

    static private final int TIME_FOR_RETRY_REMOTE_ACCESS = FACTOR_10 * SECONDS_PER_MINUTE

    static private final int CHANNEL_TIMEOUT = 5 * SECONDS_PER_MINUTE

    @Autowired
    ConfigService configService

    @Autowired
    ProcessingOptionService processingOptionService

    private JSch jsch

    private Map<Realm, Session> sessionPerRealm = [:]

    private static Semaphore maxSshCalls

    /**
     * Variable to use for synchronisation
     */
    private final Object synchronizeVariable = new Object()

    /**
     * Executes a command on a specified host and logs stdout and stderr
     *
     * @param realm The realm which identifies the host
     * @param command The command to be executed
     * @return standard output of the command executed
     * @deprecated use executeCommandReturnProcessOutput instead
     */
    @Deprecated
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
        String sshUser = configService.sshUser
        String password = configService.sshPassword
        File keyFile = configService.sshKeyFile
        SshAuthMethod sshAuthMethod = configService.sshAuthenticationMethod
        try {
            return querySsh(realm, sshUser, password, keyFile, sshAuthMethod, command)
        } catch (ProcessingException e) {
            if (e.cause && e.cause instanceof JSchException && e.cause.message.contains('channel is not opened.')) {
                logToJob("'channel is not opened' error occur, try again in ${TIME_FOR_RETRY_REMOTE_ACCESS} seconds")
                log.error("'channel is not opened' error occur, try again in ${TIME_FOR_RETRY_REMOTE_ACCESS} seconds")
                Thread.sleep(TIME_FOR_RETRY_REMOTE_ACCESS * MILLI_SECONDS_PER_SECOND)
                return querySsh(realm, sshUser, password, keyFile, sshAuthMethod, command)
            }
            throw e
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
    //maxSshCalls is initialized here, since during loading the class the database can not be accessed (not ready)
    @SuppressWarnings(['AssignmentToStaticFieldFromInstanceMethod', 'CatchException'])
    protected ProcessOutput querySsh(Realm realm, String username, String password, File keyFile, SshAuthMethod sshAuthMethod, String command) {
        assert command: "No command specified."
        if (!password && !keyFile) {
            throw new ProcessingException("Neither password nor key file for remote connection specified.")
        }
        if (!maxSshCalls) {
            synchronized (synchronizeVariable) {
                maxSshCalls = maxSshCalls ?: new Semaphore(
                        processingOptionService.findOptionAsInteger(ProcessingOption.OptionName.MAXIMUM_PARALLEL_SSH_CALLS), true
                )
            }
        }
        maxSshCalls.acquire()
        try {
            Session session = connectSshIfNeeded(realm, username, password, keyFile, sshAuthMethod)

            ChannelExec channel = (ChannelExec) session.openChannel("exec")
            logToJob("executed command: " + command)
            channel.command = command

            ProcessOutput processOutput = getOutput(channel)
            ExecutedCommandLogCallbackThreadLocalHolder.get()?.executedCommand(command, processOutput)

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
            synchronized (synchronizeVariable) {
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
                        Connector connector = ConnectorFactory.default.createConnector()
                        if (connector != null) {
                            IdentityRepository repository = new RemoteIdentityRepository(connector)
                            jsch.identityRepository = repository
                        }
                        break
                }
            }

            session = jsch.getSession(username, realm.host, realm.port)
            if (sshAuthMethod == SshAuthMethod.PASSWORD) {
                session.password = password
            }
            session.timeout = realm.timeout
            config.put("StrictHostKeyChecking", "no")
            session.config = config
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
    //the method needs to wait for the channel to end and a wait method is not provided.
    @SuppressWarnings('BusyWait')
    private static ProcessOutput getOutput(ChannelExec channel) {
        OutputStream outputErrorStream = new ByteArrayOutputStream()
        OutputStream outputStream = new ByteArrayOutputStream()
        channel.outputStream = outputStream
        channel.errStream = outputErrorStream

        channel.connect(CHANNEL_TIMEOUT * MILLI_SECONDS_PER_SECOND)
        while (!channel.isClosed()) {
            Thread.sleep(FACTOR_10)
        }
        return new ProcessOutput(
                outputStream.toString("UTF-8"),
                outputErrorStream.toString("UTF-8"),
                channel.exitStatus
        )
    }

    @Scheduled(fixedDelay = 60000L)
    @SuppressWarnings('CatchThrowable')
    void keepAlive() {
        sessionPerRealm.each { Realm realm, Session session ->
            log.debug("Send keep alive for ${realm}")
            try {
                session.sendKeepAliveMsg()
            } catch (Throwable e) {
                log.error("Send keep alive failed for ${realm} ${session}", e)
                synchronized (synchronizeVariable) {
                    sessionPerRealm.remove(realm)
                }
            }
        }
    }

    /**
     * Check, if currently cached remote session exist
     */
    boolean hasRemoteFileSystems() {
        return !sessionPerRealm.isEmpty()
    }

    /**
     * Close the used remote file systems.
     *
     * <b>Attention:</b>
     * This method is only for running workflow tests and should never be used in production.
     *
     * Since each workflow tests starts with an empty database, each test have other realms and therefore can not reuse the cached file system.
     * To avoid collecting more and more not needed cached file system and references to not valid realm, the cache are closed after each test.
     *
     * Since in production always the same realm is used, the cache shouldn't cleared and therefore this method also not used.
     */
    void closeFileSystem() {
        Map<Realm, Session> sessionPerRealmCopy = sessionPerRealm
        sessionPerRealm = [:]
        sessionPerRealmCopy.each { Realm realm, Session session ->
            log.debug("closing filesystems for realm ${realm}")
            session.disconnect()
        }
    }

    /**
     * @Deprecated Old workflow system
     */
    @Deprecated
    private void logToJob(String message) {
        Logger log = LogThreadLocal.threadLog
        log?.debug message
    }
}
