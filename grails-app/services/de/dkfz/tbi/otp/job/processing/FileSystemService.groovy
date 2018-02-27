package de.dkfz.tbi.otp.job.processing

import com.github.robtimus.filesystems.sftp.*
import com.jcraft.jsch.*
import com.jcraft.jsch.agentproxy.*
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.*
import org.codehaus.groovy.grails.commons.*
import org.springframework.scheduling.annotation.*

import java.nio.file.*

import static com.github.robtimus.filesystems.sftp.Identity.*
import static de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName.*

class FileSystemService {

    ConfigService configService
    GrailsApplication grailsApplication

    private Map<Realm, FileSystem> createdFileSystems = [:]

    /**
     * Creates a SFTP-backed FileSystem based on the connection information in the Realm passed
     * If the method is called multiple times with the same Realm, the same FileSystem is returned.
     * The same authentication values as in ExecutionService are used.
     */
    private FileSystem getFilesystem(Realm realm) throws Throwable {
        FileSystem fileSystem = createdFileSystems[realm]
        if (fileSystem == null || !fileSystem.isOpen()) {
            SFTPEnvironment env = new SFTPEnvironment()
                    .withUsername(configService.sshUser)
                    .withClientConnectionCount(ProcessingOptionService.findOptionAsNumber(MAXIMUM_SFTP_CONNECTIONS, null, null, 5).toInteger())
            if (configService.sshKeyFile) {
                env.withIdentity(fromFiles(configService.sshKeyFile))

                if (configService.useSshAgent()) {
                    Connector connector = ConnectorFactory.getDefault().createConnector()
                    if (connector != null) {
                        IdentityRepository repository = new RemoteIdentityRepository(connector)
                        env.withIdentityRepository(repository)
                    }
                }

            } else {
                env.withPassword(configService.getPbsPassword().toCharArray())
            }
            env.withTimeout(realm.timeout)
            Properties config = new Properties()
            config.put("StrictHostKeyChecking", "no")
            if (configService.sshKeyFile) {
                config.put("PreferredAuthentications", "publickey")
            }
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
        if (ProcessingOptionService.findOptionAsBoolean(FILESYSTEM_PROCESSING_USE_REMOTE, null, null)) {
            getFilesystem(realm)
        } else {
            return FileSystems.default
        }
    }

    FileSystem getFilesystemForConfigFileChecksForRealm(Realm realm) throws Throwable {
        assert realm
        if (ProcessingOptionService.findOptionAsBoolean(FILESYSTEM_CONFIG_FILE_CHECKS_USE_REMOTE, null, null)) {
            getFilesystem(realm)
        } else {
            return FileSystems.default
        }
    }

    FileSystem getFilesystemForFastqImport() throws Throwable {
        String realmName = ProcessingOptionService.findOptionSafe(FILESYSTEM_FASTQ_IMPORT, null, null)
        if (realmName) {
            Realm realm = Realm.findByName(realmName)
            assert realm
            getFilesystem(realm)
        } else {
            return FileSystems.default
        }
    }

    FileSystem getFilesystemForBamImport() throws Throwable {
        String realmName = ProcessingOptionService.findOptionSafe(FILESYSTEM_BAM_IMPORT, null, null)
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
