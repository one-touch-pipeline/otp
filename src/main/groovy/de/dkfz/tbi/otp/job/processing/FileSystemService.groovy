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

import com.github.robtimus.filesystems.sftp.SFTPEnvironment
import com.github.robtimus.filesystems.sftp.SFTPFileSystemProvider
import com.jcraft.jsch.IdentityRepository
import com.jcraft.jsch.agentproxy.*
import grails.core.GrailsApplication
import grails.gorm.transactions.Transactional
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Scope
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.config.SshAuthMethod
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.infrastructure.LoginFailedRemoteFileSystemException
import de.dkfz.tbi.otp.ngsdata.Realm

import java.nio.file.FileSystem
import java.nio.file.FileSystemException
import java.nio.file.FileSystems

import static com.github.robtimus.filesystems.sftp.Identity.fromFiles
import static de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName.*

@Scope("singleton")
@Component
@Transactional
class FileSystemService {

    @Autowired
    ConfigService configService

    @Autowired
    GrailsApplication grailsApplication

    @Autowired
    ProcessingOptionService processingOptionService

    private Map<Realm, FileSystem> createdFileSystems = [:]

    /**
     * Creates a SFTP-backed FileSystem based on the connection information in the Realm passed
     * If the method is called multiple times with the same Realm, the same FileSystem is returned.
     * The same authentication values as in RemoteShellHelper are used.
     */
    private FileSystem getFilesystem(Realm realm) throws Throwable {
        FileSystem fileSystem = createdFileSystems[realm]
        if (fileSystem == null || !fileSystem.isOpen()) {
            SFTPEnvironment env = new SFTPEnvironment()
                    .withUsername(configService.sshUser)
                    .withClientConnectionCount(processingOptionService.findOptionAsInteger(MAXIMUM_SFTP_CONNECTIONS))
            Properties config = new Properties()

            switch (configService.sshAuthenticationMethod) {
                case SshAuthMethod.KEY_FILE:
                    env.withIdentity(fromFiles(configService.sshKeyFile))
                    config.put("PreferredAuthentications", "publickey")
                    break
                case SshAuthMethod.SSH_AGENT:
                    Connector connector = ConnectorFactory.getDefault().createConnector()
                    if (connector != null) {
                        IdentityRepository repository = new RemoteIdentityRepository(connector)
                        env.withIdentityRepository(repository)
                    }
                    break
                case SshAuthMethod.PASSWORD:
                    env.withPassword(configService.getSshPassword().toCharArray())
                    break
            }

            env.withTimeout(realm.timeout)
            config.put("StrictHostKeyChecking", "no")
            env.withConfig(config)

            try {
                fileSystem = FileSystems.newFileSystem(URI.create("sftp://${realm.host}:${realm.port}"), env, grailsApplication.classLoader)
            } catch (FileSystemException exception) {
                throw new LoginFailedRemoteFileSystemException("Fail to login ${configService.sshUser}@${realm.host}:${realm.port} using authentication " +
                        "method ${configService.sshAuthenticationMethod}", exception)
            }
            createdFileSystems[realm] = fileSystem
        }
        return fileSystem
    }

    FileSystem getRemoteFileSystemOnDefaultRealm() throws Throwable {
        String realmName = processingOptionService.findOptionAsString(REALM_DEFAULT_VALUE)
        Realm realm = Realm.findByName(realmName)
        assert realm: "Default realm could not be resolved"
        return getFilesystem(realm)
    }

    FileSystem getRemoteFileSystem(Realm realm) throws Throwable {
        assert realm
        return getFilesystem(realm)
    }

    FileSystem getRemoteOrLocalFileSystemByProcessingOption(ProcessingOption.OptionName optionName) throws Throwable {
        boolean useRemote = processingOptionService.findOptionAsBoolean(optionName)
        if (useRemote) {
            return remoteFileSystemOnDefaultRealm
        } else {
            return FileSystems.default
        }
    }

    FileSystem getRealmOrLocalFileSystemByProcessingOption(ProcessingOption.OptionName optionName) throws Throwable {
        String realmName = processingOptionService.findOptionAsString(optionName)
        if (realmName) {
            Realm realm = Realm.findByName(realmName)
            assert realm
            getFilesystem(realm)
        } else {
            return FileSystems.default
        }
    }

    FileSystem getFilesystemForProcessingForRealm() throws Throwable {
        return getRemoteOrLocalFileSystemByProcessingOption(FILESYSTEM_PROCESSING_USE_REMOTE)
    }

    FileSystem getFilesystemForConfigFileChecksForRealm() throws Throwable {
        return getRemoteOrLocalFileSystemByProcessingOption(FILESYSTEM_CONFIG_FILE_CHECKS_USE_REMOTE)
    }

    FileSystem getFilesystemForFastqImport() throws Throwable {
        return getRealmOrLocalFileSystemByProcessingOption(FILESYSTEM_FASTQ_IMPORT)
    }

    FileSystem getFilesystemForBamImport() throws Throwable {
        return getRealmOrLocalFileSystemByProcessingOption(FILESYSTEM_BAM_IMPORT)
    }

    @Scheduled(fixedDelay = 30000L)
    void keepAlive() {
        createdFileSystems.each { Realm realm, FileSystem fileSystem ->
            SFTPFileSystemProvider.keepAlive(fileSystem)
        }
    }
}
