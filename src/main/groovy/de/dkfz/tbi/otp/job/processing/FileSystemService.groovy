/*
 * Copyright 2011-2023 The OTP authors
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

import com.github.robtimus.filesystems.sftp.*
import com.jcraft.jsch.IdentityRepository
import com.jcraft.jsch.agentproxy.*
import grails.core.GrailsApplication
import grails.gorm.transactions.Transactional
import grails.util.Environment
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.config.SshAuthMethod
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.infrastructure.LoginFailedRemoteFileSystemException

import java.nio.file.*

import static com.github.robtimus.filesystems.sftp.Identity.fromFiles
import static de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName.MAXIMUM_SFTP_CONNECTIONS

@Slf4j
@Component
@Transactional
class FileSystemService {

    @Autowired
    ConfigService configService

    @Autowired
    GrailsApplication grailsApplication

    @Autowired
    ProcessingOptionService processingOptionService

    private FileSystem fileSystem = null

    /**
     * Creates a SFTP-backed FileSystem
     * If the method is called multiple times, the same FileSystem is returned.
     * The same authentication values as in RemoteShellHelper are used.
     */
    FileSystem getRemoteFileSystem() throws Throwable {
        if (fileSystem == null || !fileSystem.isOpen()) {
            SFTPEnvironment env = new SFTPEnvironment()
                    .withPoolConfig(poolConfig)
                    .withUsername(configService.sshUser)
            Properties config = new Properties()

            switch (configService.sshAuthenticationMethod) {
                case SshAuthMethod.KEY_FILE:
                    env.withIdentity(fromFiles(configService.sshKeyFile))
                    config.put("PreferredAuthentications", "publickey")
                    break
                case SshAuthMethod.SSH_AGENT:
                    Connector connector = ConnectorFactory.default.createConnector()
                    if (connector != null) {
                        IdentityRepository repository = new RemoteIdentityRepository(connector)
                        env.withIdentityRepository(repository)
                    }
                    break
                case SshAuthMethod.PASSWORD:
                    env.withPassword(configService.sshPassword.toCharArray())
                    break
            }

            env.withTimeout(configService.sshTimeout)
            config.put("StrictHostKeyChecking", "no")
            env.withConfig(config)

            try {
                fileSystem = FileSystems.newFileSystem(URI.create("sftp://${configService.sshHost}:${configService.sshPort}"),
                        env, grailsApplication.classLoader)
            } catch (FileSystemException exception) {
                throw new LoginFailedRemoteFileSystemException("Fail to login ${configService.sshUser}@${configService.sshHost}:${configService.sshPort} " +
                        "using authentication method ${configService.sshAuthenticationMethod}", exception)
            }
        }
        return fileSystem
    }

    @Scheduled(fixedDelay = 30000L)
    void keepAlive() {
        if (fileSystem) {
            log.debug("Send keep alive for remote sftp file system")
            SFTPFileSystemProvider.keepAlive(fileSystem)
        }
    }

    /**
     * Close the used remote file systems.
     *
     * <b>Attention:</b>
     * This method is only for development and should never be used in production.
     *
     * It is necessary during development to support reloading by spring-devtools.
     */
    void closeFileSystem() {
        assert Environment.current == Environment.DEVELOPMENT
        log.debug("start closing sftp filesystems")
        FileSystem fileSystemCopy = fileSystem
        fileSystem = null
        if (fileSystemCopy) {
            log.debug("closing sftp filesystem")
            fileSystemCopy.close()
        }
    }

    // Return the static connection pool configs
    private SFTPPoolConfig getPoolConfig() {
        return SFTPPoolConfig.custom()
                .withMaxSize(processingOptionService.findOptionAsInteger(MAXIMUM_SFTP_CONNECTIONS))
                .build()
    }
}
