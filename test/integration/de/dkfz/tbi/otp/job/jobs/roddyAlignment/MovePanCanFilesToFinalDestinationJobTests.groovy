package de.dkfz.tbi.otp.job.jobs.roddyAlignment

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.AbstractMergedBamFile
import de.dkfz.tbi.otp.dataprocessing.RoddyBamFile
import de.dkfz.tbi.otp.job.processing.ExecutionService
import de.dkfz.tbi.otp.ngsdata.ConfigService
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.ngsdata.Realm
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.utils.CreateRoddyFileHelper
import de.dkfz.tbi.otp.utils.ExecuteRoddyCommandService
import de.dkfz.tbi.otp.utils.MoveFileUtilsService
import de.dkfz.tbi.otp.utils.ProcessHelperService
import de.dkfz.tbi.otp.utils.WaitingFileUtils
import de.dkfz.tbi.otp.utils.logging.LogThreadLocal
import grails.validation.ValidationException
import org.apache.commons.logging.impl.SimpleLog
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 */
class MovePanCanFilesToFinalDestinationJobTests extends GroovyTestCase {

    MovePanCanFilesToFinalDestinationJob movePanCanFilesToFinalDestinationJob

    RoddyBamFile roddyBamFile
    Realm realm
    File mergingBaseDir

    final String WRONG_COMMAND = "The command is wrong"

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder()

    @Before
    void setUp() {
        temporaryFolder.create() //BUG in JUnit, remove after grails update
        roddyBamFile = DomainFactory.createRoddyBamFile()
        roddyBamFile.md5sum = null
        roddyBamFile.fileSize = -1
        roddyBamFile.fileOperationStatus = AbstractMergedBamFile.FileOperationStatus.NEEDS_PROCESSING
        realm = DomainFactory.createRealmDataManagementDKFZ()
        realm.rootPath = temporaryFolder.newFolder()
        assert realm.save(flush: true)
        roddyBamFile.project.realmName = realm.name
        assert roddyBamFile.project.save(flush: true)
        SeqTrack seqTrack = roddyBamFile.seqTracks.iterator()[0]
        mergingBaseDir = new File("${realm.rootPath}/${roddyBamFile.project.dirName}/sequencing/${roddyBamFile.seqType.dirName}/view-by-pid/${roddyBamFile.individual.pid}/${roddyBamFile.sampleType.dirName}/${roddyBamFile.seqType.libraryLayoutDirName}/merged-alignment")
        assert mergingBaseDir.mkdirs()
        DomainFactory.createRoddyProcessingOptions(temporaryFolder.newFolder())

        ProcessHelperService.metaClass.static.executeCommandAndAssertExistCodeAndReturnStdout.executeCommandAndAssertExistCodeAndReturnStdout = {String cmd ->
            assert cmd ==~ "cd /tmp && sudo -u OtherUnixUser ${temporaryFolder.getRoot()}/.*/correctPathPermissionsOtherUnixUserRemoteWrapper.sh ${temporaryFolder.getRoot()}/.*/merged-alignment"
            return ''
        }
        LogThreadLocal.setThreadLog(new SimpleLog())
    }

    @After
    void tearDown() {
        LogThreadLocal.removeThreadLog()
        TestCase.removeMetaClass(MovePanCanFilesToFinalDestinationJob, movePanCanFilesToFinalDestinationJob)
        TestCase.removeMetaClass(ConfigService, movePanCanFilesToFinalDestinationJob.configService)
        TestCase.removeMetaClass(ExecutionService, movePanCanFilesToFinalDestinationJob.executionService)
        TestCase.removeMetaClass(MoveFileUtilsService, movePanCanFilesToFinalDestinationJob.moveFileUtilsService)
        TestCase.removeMetaClass(ExecuteRoddyCommandService, movePanCanFilesToFinalDestinationJob.executeRoddyCommandService)
        GroovySystem.metaClassRegistry.removeMetaClass(ProcessHelperService)
    }


    @Test
    void testExecute_AllFine() {
        String md5sum = "0123456789abcdef0123456789abcdef" // arbitrary md5sum

        movePanCanFilesToFinalDestinationJob.metaClass.moveResultFiles = { RoddyBamFile roddyBamFile, Realm realm -> }
        movePanCanFilesToFinalDestinationJob.metaClass.deletePreviousMergedBamResultFiles = { RoddyBamFile roddyBamFile, Realm realm -> }
        movePanCanFilesToFinalDestinationJob.metaClass.getProcessParameterObject = { -> roddyBamFile }
        movePanCanFilesToFinalDestinationJob.metaClass.deleteTemporaryDirectory = { RoddyBamFile roddyBamFile, Realm realm -> }
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

    @Test
    void testExecute_RoddyBamFileIsNotLatestBamFile_ShouldFail() {
        movePanCanFilesToFinalDestinationJob.metaClass.getProcessParameterObject = { -> roddyBamFile }
        roddyBamFile.metaClass.isMostRecentBamFile = { -> false}
        shouldFail (AssertionError) {
            movePanCanFilesToFinalDestinationJob.execute()
        }
    }

    @Test
    void testExecute_Md5sumFileDoesNotExist_ShouldFail() {
        movePanCanFilesToFinalDestinationJob.metaClass.moveResultFiles = { RoddyBamFile roddyBamFile, Realm realm -> }
        movePanCanFilesToFinalDestinationJob.metaClass.deletePreviousMergedBamResultFiles = { RoddyBamFile roddyBamFile, Realm realm -> }
        movePanCanFilesToFinalDestinationJob.metaClass.getProcessParameterObject = { -> roddyBamFile }
        movePanCanFilesToFinalDestinationJob.metaClass.deleteTemporaryDirectory = { RoddyBamFile roddyBamFile, Realm realm -> }
        shouldFail (AssertionError) {
            movePanCanFilesToFinalDestinationJob.execute()
        }
    }

    @Test
    void testExecute_Md5sumIsNotCorrect_ShouldFail() {
        String md5sum = "0123--6789ab##ef0123456789abcdef" // arbitrary wrong md5sum

        movePanCanFilesToFinalDestinationJob.metaClass.moveResultFiles = { RoddyBamFile roddyBamFile, Realm realm -> }
        movePanCanFilesToFinalDestinationJob.metaClass.deletePreviousMergedBamResultFiles = { RoddyBamFile roddyBamFile, Realm realm -> }
        movePanCanFilesToFinalDestinationJob.metaClass.getProcessParameterObject = { -> roddyBamFile }
        movePanCanFilesToFinalDestinationJob.metaClass.deleteTemporaryDirectory = { RoddyBamFile roddyBamFile, Realm realm -> }
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

    @Test
    void testExecute_Md5sumFileIsEmpty_ShouldFail() {
        movePanCanFilesToFinalDestinationJob.metaClass.moveResultFiles = { RoddyBamFile roddyBamFile, Realm realm -> }
        movePanCanFilesToFinalDestinationJob.metaClass.deletePreviousMergedBamResultFiles = { RoddyBamFile roddyBamFile, Realm realm -> }
        movePanCanFilesToFinalDestinationJob.metaClass.getProcessParameterObject = { -> roddyBamFile }
        movePanCanFilesToFinalDestinationJob.metaClass.deleteTemporaryDirectory = { RoddyBamFile roddyBamFile, Realm realm -> }
        File md5sumFile = roddyBamFile.finalMd5sumFile
        if (!md5sumFile.parentFile.exists()) {
            md5sumFile.parentFile.mkdirs()
        }
        md5sumFile.createNewFile()
        roddyBamFile.finalBamFile << "some content"
        movePanCanFilesToFinalDestinationJob.execute()
    }


    @Test
    void testExecute_RoddyBamFileIsWithdrawn_ShouldNotBeCopied() {
        roddyBamFile.withdrawn = true
        assert roddyBamFile.save(flush: true)

        movePanCanFilesToFinalDestinationJob.metaClass.getProcessParameterObject = { -> roddyBamFile }

        movePanCanFilesToFinalDestinationJob.metaClass.moveResultFiles = { RoddyBamFile roddyBamFile, Realm realm ->
            throw new Exception("Should not reach this method")
        }
        movePanCanFilesToFinalDestinationJob.metaClass.deletePreviousMergedBamResultFiles = { RoddyBamFile roddyBamFile, Realm realm ->
            throw new Exception("Should not reach this method")
        }
        movePanCanFilesToFinalDestinationJob.metaClass.deleteTemporaryDirectory = { RoddyBamFile roddyBamFile, Realm realm -> }
        movePanCanFilesToFinalDestinationJob.log = log
        movePanCanFilesToFinalDestinationJob.execute()
        assert roddyBamFile.fileOperationStatus == AbstractMergedBamFile.FileOperationStatus.NEEDS_PROCESSING
    }


    @Test
    void testMoveResultFiles_InputBamFileIsNull_ShouldFail() {
        shouldFail (AssertionError) {
            movePanCanFilesToFinalDestinationJob.moveResultFiles(null, realm)
        }
    }

    @Test
    void testMoveResultFiles_InputRealmIsNull_ShouldFail() {
        shouldFail (AssertionError) {
            movePanCanFilesToFinalDestinationJob.moveResultFiles(roddyBamFile, null)
        }
    }

    @Test
    void testMoveResultFiles_AllFine() {
        CreateRoddyFileHelper.createRoddyAlignmentTempResultFiles(realm, roddyBamFile)

        movePanCanFilesToFinalDestinationJob.moveFileUtilsService.metaClass.moveFileIfExists = { Realm realm, File source, File target, String permissionMask ->
            assert target.createNewFile()
            assert source.delete()
        }
        movePanCanFilesToFinalDestinationJob.moveFileUtilsService.metaClass.moveDirContentIfExists = { Realm realm, File source, File target ->
            assert target.mkdirs()
            assert source.deleteDir()
        }

        movePanCanFilesToFinalDestinationJob.moveResultFiles(roddyBamFile, realm)
    }

    @Test
    void testMoveResultFiles_SourceFileExistsInTargetAlready_ShouldFail() {
        CreateRoddyFileHelper.createRoddyAlignmentTempResultFiles(realm, roddyBamFile)
        File finalRoddyExecutionStoreDir  = new File(mergingBaseDir, "${RoddyBamFile.RODDY_EXECUTION_STORE_DIR}/execution1")
        assert finalRoddyExecutionStoreDir.mkdirs()

        movePanCanFilesToFinalDestinationJob.moveFileUtilsService.metaClass.moveFileIfExists = { Realm realm, File source, File target, String permissionMask -> }
        movePanCanFilesToFinalDestinationJob.moveFileUtilsService.metaClass.moveDirContentIfExists = { Realm realm, File source, File target -> }

        shouldFail (AssertionError) {
            movePanCanFilesToFinalDestinationJob.moveResultFiles(roddyBamFile, realm)
        }
    }

    @Test
    void testMoveResultFiles_DifferentFileExistsInTargetAlready_AllFine() {
        CreateRoddyFileHelper.createRoddyAlignmentTempResultFiles(realm, roddyBamFile)
        File finalRoddyExecutionStoreDir  = new File(mergingBaseDir, "${RoddyBamFile.RODDY_EXECUTION_STORE_DIR}/execution2")
        assert finalRoddyExecutionStoreDir.mkdirs()

        movePanCanFilesToFinalDestinationJob.moveFileUtilsService.metaClass.moveFileIfExists = { Realm realm, File source, File target, String permissionMask -> }
        movePanCanFilesToFinalDestinationJob.moveFileUtilsService.metaClass.moveDirContentIfExists = { Realm realm, File source, File target -> }

        movePanCanFilesToFinalDestinationJob.moveResultFiles(roddyBamFile, realm)
    }

    @Test
    void testMoveResultFiles_SourceFileWasTransferredAlready_NoMovement() {
        File finalRoddyExecutionStoreDir  = new File(mergingBaseDir, "${RoddyBamFile.RODDY_EXECUTION_STORE_DIR}/execution1")
        assert finalRoddyExecutionStoreDir.mkdirs()

        movePanCanFilesToFinalDestinationJob.moveFileUtilsService.metaClass.moveFileIfExists = { Realm realm, File source, File target, String permissionMask -> }
        movePanCanFilesToFinalDestinationJob.moveFileUtilsService.metaClass.moveDirContentIfExists = { Realm realm, File source, File target -> }

        movePanCanFilesToFinalDestinationJob.moveResultFiles(roddyBamFile, realm)
    }


    @Test
    void testDeleteTemporaryDirectory_InputBamFileIsNull_ShouldFail() {
        shouldFail (AssertionError) {
            movePanCanFilesToFinalDestinationJob.deleteTemporaryDirectory(null, realm)
        }
    }


    @Test
    void testDeleteTemporaryDirectory_InputRealmIsNull_ShouldFail() {
        shouldFail (AssertionError) {
            movePanCanFilesToFinalDestinationJob.deleteTemporaryDirectory(roddyBamFile, null)
        }
    }

    @Test
    void testDeleteTemporaryDirectory_DirectoryStillExists_ShouldFail() {
        File baseTempDir = new File(mergingBaseDir, "${RoddyBamFile.TMP_DIR}_${roddyBamFile.id}")
        assert baseTempDir.mkdir()

        movePanCanFilesToFinalDestinationJob.executionService.metaClass.executeCommand = { Realm realm, String cmd ->
            assert cmd == "rm -rf ${baseTempDir.path}"
        }
        shouldFail (AssertionError) {
            movePanCanFilesToFinalDestinationJob.deleteTemporaryDirectory(roddyBamFile, realm)
        }
    }

    @Test
    void testDeleteTemporaryDirectory_AllFine() {
        File baseTempDir = new File(mergingBaseDir, "${RoddyBamFile.TMP_DIR}_${roddyBamFile.id}")
        assert baseTempDir.mkdir()

        movePanCanFilesToFinalDestinationJob.executionService.metaClass.executeCommand = { Realm realm, String cmd ->
            assert cmd == "rm -rf ${baseTempDir.path}"
            baseTempDir.deleteDir()
        }
        movePanCanFilesToFinalDestinationJob.deleteTemporaryDirectory(roddyBamFile, realm)
    }



    @Test
    void testDeletePreviousMergedBamResultFiles_NoBaseBamFile_NothingToDelete() {
        movePanCanFilesToFinalDestinationJob.executionService.metaClass.executeCommand = { Realm realm, String cmd ->
            throw new Exception("Should not reach this method")
        }
        movePanCanFilesToFinalDestinationJob.deletePreviousMergedBamResultFiles(roddyBamFile, realm)
    }


    @Test
    void testDeletePreviousMergedBamResultFiles_LatestBamFileNotInTempFolder_NothingToDelete() {
        RoddyBamFile roddyBamFile2 = createBamFileSetupAndReturnBamFileToWorkOn()
        assert roddyBamFile2.tmpRoddyBamFile.delete()

        movePanCanFilesToFinalDestinationJob.executionService.metaClass.executeCommand = { Realm realm, String cmd ->
            throw new Exception("Should not reach this method")
        }
        movePanCanFilesToFinalDestinationJob.deletePreviousMergedBamResultFiles(roddyBamFile2, realm)
    }


    @Test
    void testDeletePreviousMergedBamResultFiles_BaseBamDeletionFailed_ShouldFail() {
        RoddyBamFile roddyBamFile2 = createBamFileSetupAndReturnBamFileToWorkOn()

        movePanCanFilesToFinalDestinationJob.executionService.metaClass.executeCommand = { Realm realm, String cmd ->
            assert cmd == "rm -rf ${roddyBamFile.finalBamFile} ${roddyBamFile.finalBaiFile} ${roddyBamFile.finalMd5sumFile} ${roddyBamFile.finalMergedQADirectory}" : WRONG_COMMAND
        }
        assert !shouldFail(AssertionError) {
            movePanCanFilesToFinalDestinationJob.deletePreviousMergedBamResultFiles(roddyBamFile2, realm)
        }.contains(WRONG_COMMAND)
    }


    @Test
    void testDeletePreviousMergedBamResultFiles_BaseBamFileDeletionSuccessful_QAFilesDeletionFailed_ShouldFail() {
        RoddyBamFile roddyBamFile2 = createBamFileSetupAndReturnBamFileToWorkOn()
        File qaPath = roddyBamFile.finalMergedQADirectory
        assert qaPath.mkdirs()
        assert WaitingFileUtils.waitUntilExists(qaPath)

        movePanCanFilesToFinalDestinationJob.executionService.metaClass.executeCommand = { Realm realm, String cmd ->
            assert cmd == "rm -rf ${roddyBamFile.finalBamFile} ${roddyBamFile.finalBaiFile} ${roddyBamFile.finalMd5sumFile} ${roddyBamFile.finalMergedQADirectory}" : WRONG_COMMAND
            roddyBamFile.finalBamFile.delete()
        }
        assert !shouldFail(AssertionError) {
            movePanCanFilesToFinalDestinationJob.deletePreviousMergedBamResultFiles(roddyBamFile2, realm)
        }.contains(WRONG_COMMAND)

    }

    @Test
    void testDeletePreviousMergedBamResultFiles_QAFilesWhereDeleted_AllFine() {
        RoddyBamFile roddyBamFile2 = createBamFileSetupAndReturnBamFileToWorkOn()

        File roddyBaiFilePath = roddyBamFile.finalBaiFile
        File roddyMd5SumFilePath = roddyBamFile.finalMd5sumFile
        File qaPath = roddyBamFile.finalMergedQADirectory
        assert roddyBaiFilePath.createNewFile()
        assert roddyMd5SumFilePath.createNewFile()
        assert qaPath.mkdirs()
        assert WaitingFileUtils.waitUntilExists(qaPath)

        movePanCanFilesToFinalDestinationJob.executionService.metaClass.executeCommand = { Realm realm, String cmd ->
            assert cmd == "rm -rf ${roddyBamFile.finalBamFile} ${roddyBamFile.finalBaiFile} ${roddyBamFile.finalMd5sumFile} ${roddyBamFile.finalMergedQADirectory}" : WRONG_COMMAND
            roddyBamFile.finalBamFile.delete()
            roddyBaiFilePath.delete()
            roddyMd5SumFilePath.delete()
            qaPath.deleteDir()
        }

        movePanCanFilesToFinalDestinationJob.deletePreviousMergedBamResultFiles(roddyBamFile2, realm)
    }

    @Test
    void testCorrectPermission_AllFine() {
        movePanCanFilesToFinalDestinationJob.correctPermissions(roddyBamFile)
    }

    @Test
    void testCorrectPermission_BamFileIsNull_shouldFail() {
        assert shouldFail(AssertionError) {
            movePanCanFilesToFinalDestinationJob.correctPermissions(null)
        }.contains('RoddyBamFile')
    }



    private void finishOperationStateOfRoddyBamFile(RoddyBamFile roddyBamFile) {
        roddyBamFile.md5sum = DomainFactory.DEFAULT_MD5_SUM
        roddyBamFile.fileOperationStatus = AbstractMergedBamFile.FileOperationStatus.PROCESSED
        roddyBamFile.fileSize = 1000
        assert roddyBamFile.save(flush: true)
    }

    private RoddyBamFile createBamFileSetupAndReturnBamFileToWorkOn() {
        finishOperationStateOfRoddyBamFile(roddyBamFile)
        RoddyBamFile roddyBamFile2 = DomainFactory.createRoddyBamFile(roddyBamFile)

        File roddyBamFile1Path = roddyBamFile.finalBamFile
        File roddyBamFile2Path = roddyBamFile2.tmpRoddyBamFile
        assert roddyBamFile1Path.createNewFile()
        assert roddyBamFile2Path.parentFile.mkdirs()
        assert roddyBamFile2Path.createNewFile()
        return roddyBamFile2
    }

}
