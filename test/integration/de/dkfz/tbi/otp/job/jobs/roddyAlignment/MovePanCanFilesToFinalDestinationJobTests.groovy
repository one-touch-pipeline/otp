package de.dkfz.tbi.otp.job.jobs.roddyAlignment

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.AbstractMergedBamFile
import de.dkfz.tbi.otp.dataprocessing.RoddyBamFile
import de.dkfz.tbi.otp.job.processing.ExecutionService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.MoveFileUtilsService
import grails.validation.ValidationException
import org.junit.After
import org.junit.Before
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext

/**
 */
class MovePanCanFilesToFinalDestinationJobTests extends GroovyTestCase {

    MovePanCanFilesToFinalDestinationJob movePanCanFilesToFinalDestinationJob

    RoddyBamFile roddyBamFile
    Realm realm
    File mergingBaseDir
    String bamFileName

    @Before
    void setUp() {
        roddyBamFile = DomainFactory.createRoddyBamFile()
        roddyBamFile.md5sum = null
        roddyBamFile.fileSize = -1
        roddyBamFile.fileOperationStatus = AbstractMergedBamFile.FileOperationStatus.NEEDS_PROCESSING
        realm = DomainFactory.createRealmDataManagementDKFZ()
        realm.rootPath = TestCase.createEmptyTestDirectory()
        assert realm.save(flush: true)
        roddyBamFile.project.realmName = realm.name
        assert roddyBamFile.project.save(flush: true)
        mergingBaseDir = new File("${realm.rootPath}/${roddyBamFile.project.dirName}/sequencing/${roddyBamFile.seqType.dirName}/view-by-pid/${roddyBamFile.individual.pid}/${roddyBamFile.sampleType.dirName}/${roddyBamFile.seqType.libraryLayoutDirName}/merged-alignment")
        assert mergingBaseDir.mkdirs()
        bamFileName = "${roddyBamFile.sampleType.dirName}_${roddyBamFile.individual.pid}_merged.mdup.bam"
    }

    @After
    void tearDown() {
        TestCase.removeMetaClass(MovePanCanFilesToFinalDestinationJob, movePanCanFilesToFinalDestinationJob)
        TestCase.removeMetaClass(ConfigService, movePanCanFilesToFinalDestinationJob.configService)
        TestCase.removeMetaClass(ExecutionService, movePanCanFilesToFinalDestinationJob.executionService)
        TestCase.removeMetaClass(MoveFileUtilsService, movePanCanFilesToFinalDestinationJob.moveFileUtilsService)
        assert new File(realm.rootPath).deleteDir()
    }


    void testExecute_AllFine() {
        String md5sum = "0123456789abcdef0123456789abcdef" // arbitrary md5sum

        movePanCanFilesToFinalDestinationJob.metaClass.moveResultFiles = { RoddyBamFile roddyBamFile -> }
        movePanCanFilesToFinalDestinationJob.metaClass.getProcessParameterObject = { -> roddyBamFile }
        movePanCanFilesToFinalDestinationJob.metaClass.deleteTemporaryDirectory = { RoddyBamFile roddyBamFile -> }
        File md5sumFile = roddyBamFile.finalMd5sumFile
        if (!md5sumFile.parentFile.exists()) {
            md5sumFile.parentFile.mkdirs()
        }
        md5sumFile << "${md5sum}  sampletype_individual_merged.mdup.bam"
        roddyBamFile.finalBamFile << "some content"
        movePanCanFilesToFinalDestinationJob.execute()
        assert roddyBamFile.fileOperationStatus == AbstractMergedBamFile.FileOperationStatus.PROCESSED
        assert roddyBamFile.md5sum == md5sum
        assert roddyBamFile.fileSize > 0
    }

    void testExecute_RoddyBamFileIsNotLatestBamFile_ShouldFail() {
        movePanCanFilesToFinalDestinationJob.metaClass.getProcessParameterObject = { -> roddyBamFile }
        roddyBamFile.metaClass.isMostRecentBamFile = { -> false}
        shouldFail (AssertionError) {
            movePanCanFilesToFinalDestinationJob.execute()
        }
    }

    void testExecute_Md5sumFileDoesNotExist_ShouldFail() {
        movePanCanFilesToFinalDestinationJob.metaClass.moveResultFiles = { RoddyBamFile roddyBamFile -> }
        movePanCanFilesToFinalDestinationJob.metaClass.getProcessParameterObject = { -> roddyBamFile }
        movePanCanFilesToFinalDestinationJob.metaClass.deleteTemporaryDirectory = { RoddyBamFile roddyBamFile -> }
        shouldFail (AssertionError) {
            movePanCanFilesToFinalDestinationJob.execute()
        }
    }

    void testExecute_Md5sumIsNotCorrect_ShouldFail() {
        String md5sum = "0123--6789ab##ef0123456789abcdef" // arbitrary wrong md5sum

        movePanCanFilesToFinalDestinationJob.metaClass.moveResultFiles = { RoddyBamFile roddyBamFile -> }
        movePanCanFilesToFinalDestinationJob.metaClass.getProcessParameterObject = { -> roddyBamFile }
        movePanCanFilesToFinalDestinationJob.metaClass.deleteTemporaryDirectory = { RoddyBamFile roddyBamFile -> }
        File md5sumFile = roddyBamFile.finalMd5sumFile
        if (!md5sumFile.parentFile.exists()) {
            md5sumFile.parentFile.mkdirs()
        }
        md5sumFile << "${md5sum}  sampletype_individual_merged.mdup.bam"
        roddyBamFile.finalBamFile << "some content"
        shouldFail(ValidationException) {
            movePanCanFilesToFinalDestinationJob.execute()
        }
    }

    void testExecute_Md5sumFileIsEmpty_ShouldFail() {
        movePanCanFilesToFinalDestinationJob.metaClass.moveResultFiles = { RoddyBamFile roddyBamFile -> }
        movePanCanFilesToFinalDestinationJob.metaClass.getProcessParameterObject = { -> roddyBamFile }
        movePanCanFilesToFinalDestinationJob.metaClass.deleteTemporaryDirectory = { RoddyBamFile roddyBamFile -> }
        File md5sumFile = roddyBamFile.finalMd5sumFile
        if (!md5sumFile.parentFile.exists()) {
            md5sumFile.parentFile.mkdirs()
        }
        md5sumFile.createNewFile()
        roddyBamFile.finalBamFile << "some content"
        movePanCanFilesToFinalDestinationJob.execute()
    }


    void testExecute_RoddyBamFileIsWithdrawn_ShouldNotBeCopied() {
        roddyBamFile.withdrawn = true
        assert roddyBamFile.save(flush: true)

        movePanCanFilesToFinalDestinationJob.metaClass.getProcessParameterObject = { -> roddyBamFile }

        movePanCanFilesToFinalDestinationJob.metaClass.moveResultFiles = { RoddyBamFile roddyBamFile ->
            throw new Exception("Should not reach this method")
        }
        movePanCanFilesToFinalDestinationJob.log = log
        movePanCanFilesToFinalDestinationJob.execute()
        assert roddyBamFile.fileOperationStatus == AbstractMergedBamFile.FileOperationStatus.NEEDS_PROCESSING
    }


    void testMoveResultFiles_InputIsNull_ShouldFail() {
        shouldFail (AssertionError) {
            movePanCanFilesToFinalDestinationJob.moveResultFiles(null)
        }
    }

    void testMoveResultFiles_RealmIsNull_ShouldFail() {
        realm.delete()

        shouldFail (AssertionError) {
            movePanCanFilesToFinalDestinationJob.moveResultFiles(roddyBamFile)
        }
    }


    void testMoveResultFiles_AllFine() {
        createSourceFiles(roddyBamFile)

        movePanCanFilesToFinalDestinationJob.moveFileUtilsService.metaClass.moveFileIfExists = { Realm realm, File source, File target ->
            assert target.createNewFile()
            assert source.delete()
        }
        movePanCanFilesToFinalDestinationJob.moveFileUtilsService.metaClass.moveDirContentIfExists = { Realm realm, File source, File target ->
            assert target.mkdir()
            assert source.deleteDir()
        }

        movePanCanFilesToFinalDestinationJob.moveResultFiles(roddyBamFile)
    }

    void testMoveResultFiles_SourceFileExistsInTargetAlready_ShouldFail() {
        createSourceFiles(roddyBamFile)
        File finalRoddyExecutionStoreDir  = new File(mergingBaseDir, "${RoddyBamFile.RODDY_EXECUTION_STORE_DIR}/execution1")
        assert finalRoddyExecutionStoreDir.mkdirs()

        movePanCanFilesToFinalDestinationJob.moveFileUtilsService.metaClass.moveFileIfExists = { Realm realm, File source, File target -> }
        movePanCanFilesToFinalDestinationJob.moveFileUtilsService.metaClass.moveDirContentIfExists = { Realm realm, File source, File target -> }

        shouldFail (AssertionError) {
            movePanCanFilesToFinalDestinationJob.moveResultFiles(roddyBamFile)
        }
    }

    void testMoveResultFiles_DifferentFileExistsInTargetAlready_AllFine() {
        createSourceFiles(roddyBamFile)
        File finalRoddyExecutionStoreDir  = new File(mergingBaseDir, "${RoddyBamFile.RODDY_EXECUTION_STORE_DIR}/execution2")
        assert finalRoddyExecutionStoreDir.mkdirs()

        movePanCanFilesToFinalDestinationJob.moveFileUtilsService.metaClass.moveFileIfExists = { Realm realm, File source, File target -> }
        movePanCanFilesToFinalDestinationJob.moveFileUtilsService.metaClass.moveDirContentIfExists = { Realm realm, File source, File target -> }

        movePanCanFilesToFinalDestinationJob.moveResultFiles(roddyBamFile)
    }

    void testMoveResultFiles_SourceFileWasTransferredAlready_NoMovement() {
        File finalRoddyExecutionStoreDir  = new File(mergingBaseDir, "${RoddyBamFile.RODDY_EXECUTION_STORE_DIR}/execution1")
        assert finalRoddyExecutionStoreDir.mkdirs()

        movePanCanFilesToFinalDestinationJob.moveFileUtilsService.metaClass.moveFileIfExists = { Realm realm, File source, File target -> }
        movePanCanFilesToFinalDestinationJob.moveFileUtilsService.metaClass.moveDirContentIfExists = { Realm realm, File source, File target -> }

        movePanCanFilesToFinalDestinationJob.moveResultFiles(roddyBamFile)
    }


    void testDeleteTemporaryDirectory_InputBamFileIsNull_ShouldFail() {
        shouldFail (AssertionError) {
            movePanCanFilesToFinalDestinationJob.deleteTemporaryDirectory(null)
        }
    }


    void testDeleteTemporaryDirectory_RealmIsNull_ShouldFail() {
        realm.delete()
        shouldFail (AssertionError) {
            movePanCanFilesToFinalDestinationJob.deleteTemporaryDirectory(roddyBamFile)
        }
    }

    void testDeleteTemporaryDirectory_DirectoryStillExists_ShouldFail() {
        File baseTempDir = new File(mergingBaseDir, "${RoddyBamFile.TMP_DIR}_${roddyBamFile.id}")
        assert baseTempDir.mkdir()

        movePanCanFilesToFinalDestinationJob.executionService.metaClass.executeCommand = { Realm realm, String cmd ->
            assert cmd == "rm -rf ${baseTempDir.path}"
        }
        shouldFail (AssertionError) {
            movePanCanFilesToFinalDestinationJob.deleteTemporaryDirectory(roddyBamFile)
        }
    }

    void testDeleteTemporaryDirectory_AllFine() {
        File baseTempDir = new File(mergingBaseDir, "${RoddyBamFile.TMP_DIR}_${roddyBamFile.id}")
        assert baseTempDir.mkdir()

        movePanCanFilesToFinalDestinationJob.executionService.metaClass.executeCommand = { Realm realm, String cmd ->
            assert cmd == "rm -rf ${baseTempDir.path}"
            baseTempDir.deleteDir()
        }
        movePanCanFilesToFinalDestinationJob.deleteTemporaryDirectory(roddyBamFile)
    }



    private void createSourceFiles(RoddyBamFile roddyBamFile) {
        File baseTempDir = new File(mergingBaseDir, "${RoddyBamFile.TMP_DIR}_${roddyBamFile.id}")
        assert baseTempDir.mkdir()
        File tmpQADir = new File(baseTempDir, RoddyBamFile.QUALITY_CONTROL_DIR)
        assert tmpQADir.mkdir()
        File qaFile = new File(tmpQADir, "qa.txt")
        qaFile.createNewFile()
        File tmpRoddyExecutionStoreDir  = new File(baseTempDir, RoddyBamFile.RODDY_EXECUTION_STORE_DIR)
        assert tmpRoddyExecutionStoreDir.mkdir()
        File execution1Dir = new File(tmpRoddyExecutionStoreDir, "execution1")
        execution1Dir.mkdir()
        File tmpRoddyBamFile  = new File(baseTempDir, bamFileName)
        assert tmpRoddyBamFile.createNewFile()
        File tmpRoddyBaiFile  = new File(baseTempDir, "${bamFileName}.bai")
        assert tmpRoddyBaiFile.createNewFile()
        File tmpRoddyMd5sumFile = new File(baseTempDir, "${bamFileName}.md5sum")
        assert tmpRoddyMd5sumFile.createNewFile()
    }

}
