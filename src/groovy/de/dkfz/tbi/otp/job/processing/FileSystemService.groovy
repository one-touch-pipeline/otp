package de.dkfz.tbi.otp.job.processing

import com.github.robtimus.filesystems.sftp.*
import com.jcraft.jsch.*
import com.jcraft.jsch.agentproxy.*
import de.dkfz.tbi.otp.config.*
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.*
import org.codehaus.groovy.grails.commons.*
import org.springframework.scheduling.annotation.*
import org.springframework.beans.factory.annotation.*
import org.springframework.context.annotation.*
import org.springframework.stereotype.*
import grails.transaction.*

import java.nio.file.*

import static com.github.robtimus.filesystems.sftp.Identity.*
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
            } catch (Throwable throwable) {
                if (throwable.cause) {
                    throw throwable.cause
                }
                throw throwable
            }
            createdFileSystems[realm] = fileSystem
        }
        return fileSystem
    }

    FileSystem getFilesystemForProcessingForRealm(Realm realm) throws Throwable {
        assert realm
        if (processingOptionService.findOptionAsBoolean(FILESYSTEM_PROCESSING_USE_REMOTE)) {
            getFilesystem(realm)
        } else {
            return FileSystems.default
        }
    }

    FileSystem getFilesystemForConfigFileChecksForRealm(Realm realm) throws Throwable {
        assert realm
        if (processingOptionService.findOptionAsBoolean(FILESYSTEM_CONFIG_FILE_CHECKS_USE_REMOTE)) {
            getFilesystem(realm)
        } else {
            return FileSystems.default
        }
    }

    FileSystem getFilesystemForFastqImport() throws Throwable {
        String realmName = processingOptionService.findOptionAsString(FILESYSTEM_FASTQ_IMPORT)
        if (realmName) {
            Realm realm = Realm.findByName(realmName)
            assert realm
            getFilesystem(realm)
        } else {
            return FileSystems.default
        }
    }

    FileSystem getFilesystemForBamImport() throws Throwable {
        String realmName = processingOptionService.findOptionAsString(FILESYSTEM_BAM_IMPORT)
        if (realmName) {
            Realm realm = Realm.findByName(realmName)
            assert realm
            getFilesystem(realm)
        } else {
            return FileSystems.default
        }
    }

    @Scheduled(fixedDelay = 30000L)
    void keepAlive() {
        createdFileSystems.each { Realm realm, FileSystem fileSystem ->
            SFTPFileSystemProvider.keepAlive(fileSystem)
        }
    }
}
