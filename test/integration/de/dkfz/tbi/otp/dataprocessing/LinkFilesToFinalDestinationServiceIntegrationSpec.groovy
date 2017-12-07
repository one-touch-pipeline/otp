package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.*
import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.dataprocessing.rnaAlignment.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*
import grails.test.spock.*
import org.junit.*
import org.junit.rules.*


class LinkFilesToFinalDestinationServiceIntegrationSpec extends IntegrationSpec {
    LinkFilesToFinalDestinationService service

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder()

    RnaRoddyBamFile roddyBamFile
    Realm realm
    TestConfigService configService
    String fileName

    void setup() {
        service = new LinkFilesToFinalDestinationService()
        service.executionService = new ExecutionService()
        service.linkFileUtils = new LinkFileUtils()
        service.lsdfFilesService = new LsdfFilesService()
        service.lsdfFilesService.createClusterScriptService = new CreateClusterScriptService()
        service.lsdfFilesService.executionService = service.executionService
        service.linkFileUtils.createClusterScriptService = new CreateClusterScriptService()
        service.linkFileUtils.lsdfFilesService = service.lsdfFilesService
        service.linkFileUtils.executionService = service.executionService

        roddyBamFile = DomainFactory.createRoddyBamFile([:], RnaRoddyBamFile)

        realm = DomainFactory.createRealmDataManagement([name: roddyBamFile.project.realmName])
        configService = new TestConfigService(['otp.root.path': temporaryFolder.newFolder().path])
    }

    void cleanup() {
        configService.clean()
    }

    void "test linkNewRnaResults"() {
        given:
        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(roddyBamFile)

        when:
        TestCase.withMockedExecutionService(service.executionService, {
            service.linkNewRnaResults(roddyBamFile, realm)
        })

        then:
        [
                roddyBamFile.finalBamFile,
                roddyBamFile.finalBaiFile,
                roddyBamFile.finalMd5sumFile,
                roddyBamFile.finalExecutionStoreDirectory,
                roddyBamFile.finalQADirectory,
                new File(roddyBamFile.baseDirectory, "additionalArbitraryFile"),
        ].each {
            assert it.exists()
        }
    }

    void "test cleanupOldRnaResults"() {
        given:
        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(roddyBamFile)

        RnaRoddyBamFile roddyBamFile2 = DomainFactory.createRoddyBamFile([workPackage: roddyBamFile.workPackage, config: roddyBamFile.config], RnaRoddyBamFile)
        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(roddyBamFile2)
        TestCase.withMockedExecutionService(service.executionService, {
            service.linkNewRnaResults(roddyBamFile2, realm)
        })
        assert roddyBamFile.workDirectory.exists()

        when:
        TestCase.withMockedExecutionService(service.executionService, {
            service.cleanupOldRnaResults(roddyBamFile, realm)
        })

        then:
        !roddyBamFile2.workDirectory.exists()
        !roddyBamFile2.finalExecutionStoreDirectory.exists()
        !roddyBamFile2.finalQADirectory.exists()
        !roddyBamFile2.finalBamFile.exists()
        !roddyBamFile2.finalBaiFile.exists()
        !roddyBamFile2.finalMd5sumFile.exists()
        !new File(roddyBamFile.baseDirectory, "additionalArbitraryFile").exists()

        roddyBamFile.workDirectory.exists()
        !roddyBamFile.finalExecutionStoreDirectory.exists()
        !roddyBamFile.finalQADirectory.exists()
        !roddyBamFile.finalBamFile.exists()
        !roddyBamFile.finalBaiFile.exists()
        !roddyBamFile.finalMd5sumFile.exists()
    }
}
