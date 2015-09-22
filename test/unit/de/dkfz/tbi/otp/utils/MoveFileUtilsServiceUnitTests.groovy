package de.dkfz.tbi.otp.utils

import de.dkfz.tbi.otp.job.processing.ExecutionService
import de.dkfz.tbi.otp.ngsdata.Realm

import org.junit.After
import org.junit.Before
import grails.buildtestdata.mixin.Build
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 */
@Build([Realm])
@Deprecated
//TODO: OTP-1734 delete class
class MoveFileUtilsServiceUnitTests {

    final static String FILE_PERMISSION = "640"


    Realm realm

    MoveFileUtilsService moveFileUtilsService

    final String SOURCE_FILE = "sourceFile"
    final String SOURCE_FOLDER = "sourceFolder"
    final String TARGET_FILE = "targetFile"
    final String TARGET_FOLDER = "targetFolder"

    @Rule
    public TemporaryFolder testDirectory= new TemporaryFolder()

    @Before
    void setUp() {
        realm = Realm.build()
        moveFileUtilsService = new MoveFileUtilsService()
        moveFileUtilsService.executionService = new ExecutionService()
    }

    @After
    void tearDown() {
        realm = null
        moveFileUtilsService = null
        testDirectory = null
    }

    @Test
    void testMoveFileIfExists_InputRealmIsNull_ShouldFail() {
        assert shouldFail (AssertionError) {
            moveFileUtilsService.moveFileIfExists(null, new File(SOURCE_FILE), new File(TARGET_FILE))
        }.contains('realm')
    }

    @Test
    void testMoveFileIfExists_InputSourceIsNull_ShouldFail() {
        assert shouldFail (AssertionError) {
            moveFileUtilsService.moveFileIfExists(realm, null, new File(TARGET_FILE))
        }.contains('source')
    }

    @Test
    void testMoveFileIfExists_InputTargetIsNull_ShouldFail() {
        assert shouldFail (AssertionError) {
            moveFileUtilsService.moveFileIfExists(realm, new File(SOURCE_FILE), null)
        }.contains('target')
    }

    @Test
    void testMoveFileIfExists_SourceDoesNotExist_NoCopying() {
        File targetFile = testDirectory.newFile(TARGET_FILE)

        moveFileUtilsService.executionService.metaClass.executeCommand = {Realm realm, String cmd ->
            assert false : "should not call this method at all since no copying is needed"
        }

        moveFileUtilsService.moveFileIfExists(realm, new File("sourceFileDoesNotExist"), targetFile)
    }

    @Test
    void testMoveFileIfExists_TargetDoesNotExistsAfterCopying_ShouldFail() {
        File sourceFile = testDirectory.newFile(SOURCE_FILE)
        File targetFile = new File("targetFileDoesNotExist")

        moveFileUtilsService.executionService.metaClass.executeCommand = {Realm realm, String cmd ->
            assert cmd == createFileCmdString(sourceFile, targetFile, FILE_PERMISSION)
            sourceFile.delete()
        }
        assert shouldFail (AssertionError) {
            moveFileUtilsService.moveFileIfExists(realm, sourceFile, targetFile)
        }.contains("assert WaitingFileUtils.waitUntilExists(target)")
    }

    @Test
    void testMoveFileIfExists_SourceStillExistsAfterCopying_ShouldFail() {
        File sourceFile = testDirectory.newFile(SOURCE_FILE)
        File targetFile = testDirectory.newFile(TARGET_FILE)

        moveFileUtilsService.executionService.metaClass.executeCommand = {Realm realm, String cmd ->
            assert cmd == createFileCmdString(sourceFile, targetFile, FILE_PERMISSION)
        }
        assert shouldFail (AssertionError) {
            moveFileUtilsService.moveFileIfExists(realm, sourceFile, targetFile)
        }.contains("assert WaitingFileUtils.waitUntilDoesNotExist(source)")
    }

    @Test
    void testMoveFileIfExists_SourceExists_CopyingSuccessful() {
        File sourceFile = testDirectory.newFile(SOURCE_FILE)
        File targetFile = testDirectory.newFile(TARGET_FILE)
        targetFile.delete()

        moveFileUtilsService.executionService.metaClass.executeCommand = {Realm realm, String cmd ->
            assert cmd == createFileCmdString(sourceFile, targetFile, FILE_PERMISSION)
            executeCommandLocally(cmd)
        }
        moveFileUtilsService.moveFileIfExists(realm, sourceFile, targetFile)
        assert WaitingFileUtils.waitUntilExists(targetFile)
    }

    @Test
    void testMoveFileIfExists_OlderTargetExistsAlready_OverwriteSuccessful() {
        File sourceFolder = testDirectory.newFolder(SOURCE_FOLDER)
        File sourceFile = new File(sourceFolder, SOURCE_FILE)
        sourceFile << SOURCE_FILE
        File targetFolder = testDirectory.newFolder(TARGET_FOLDER)
        File targetFile_allreadyExists = new File(targetFolder, SOURCE_FILE)
        targetFile_allreadyExists.text = TARGET_FILE

        moveFileUtilsService.executionService.metaClass.executeCommand = {Realm realm, String cmd ->
            assert cmd == createFileCmdString(sourceFile, targetFile_allreadyExists, FILE_PERMISSION)
            executeCommandLocally(cmd)
        }

        assert targetFile_allreadyExists.text == TARGET_FILE
        moveFileUtilsService.moveFileIfExists(realm, sourceFile, targetFile_allreadyExists)
        assert WaitingFileUtils.waitUntilExists(targetFile_allreadyExists)
        assert targetFile_allreadyExists.text == SOURCE_FILE
    }

    @Test
    void testMoveFileIfExists_CopyingSuccessful_ReadPermissionsForAll() {
        File sourceFile = testDirectory.newFile(SOURCE_FILE)
        File targetFile = testDirectory.newFile(TARGET_FILE)
        targetFile.delete()

        moveFileUtilsService.executionService.metaClass.executeCommand = {Realm realm, String cmd ->
            assert cmd ==createFileCmdString(sourceFile, targetFile, "644")
            executeCommandLocally(cmd)
        }
        moveFileUtilsService.moveFileIfExists(realm, sourceFile, targetFile, true)
        assert WaitingFileUtils.waitUntilExists(targetFile)
    }




    @Test
    void testMoveFileIfExists_WithPermissionMaskParameter_InputRealmIsNull_ShouldFail() {
        assert shouldFail (AssertionError) {
            moveFileUtilsService.moveFileIfExists(null, new File(SOURCE_FILE), new File(TARGET_FILE), FILE_PERMISSION)
        }.contains('realm')
    }

    @Test
    void testMoveFileIfExists_WithPermissionMaskParameter_InputSourceIsNull_ShouldFail() {
        assert shouldFail (AssertionError) {
            moveFileUtilsService.moveFileIfExists(realm, null, new File(TARGET_FILE), FILE_PERMISSION)
        }.contains('source')
    }

    @Test
    void testMoveFileIfExists_WithPermissionMaskParameter_InputTargetIsNull_ShouldFail() {
        assert shouldFail (AssertionError) {
            moveFileUtilsService.moveFileIfExists(realm, new File(SOURCE_FILE), null, FILE_PERMISSION)
        }.contains('target')
    }

    @Test
    void testMoveFileIfExists_WithPermissionMaskParameter_SourceDoesNotExist_NoMoving() {
        File targetFile = testDirectory.newFile(TARGET_FILE)

        moveFileUtilsService.executionService.metaClass.executeCommand = {Realm realm, String cmd ->
            assert false : "should not call this method at all since no copying is needed"
        }

        moveFileUtilsService.moveFileIfExists(realm, new File("sourceFileDoesNotExist"), targetFile, FILE_PERMISSION)
    }

    @Test
    void testMoveFileIfExists_WithPermissionMaskParameter_TargetDoesNotExistsAfterMoving_ShouldFail() {
        File sourceFile = testDirectory.newFile(SOURCE_FILE)
        File targetFile = new File("targetFileDoesNotExist")

        moveFileUtilsService.executionService.metaClass.executeCommand = {Realm realm, String cmd ->
            assert cmd == createFileCmdString(sourceFile, targetFile, "600")
            sourceFile.delete()
        }
        assert shouldFail (AssertionError) {
            moveFileUtilsService.moveFileIfExists(realm, sourceFile, targetFile, "600")
        }.contains(targetFile.name)
    }

    @Test
    void testMoveFileIfExists_WithPermissionMaskParameter_SourceStillExistsAfterMoving_ShouldFail() {
        File sourceFile = testDirectory.newFile(SOURCE_FILE)
        File targetFile = testDirectory.newFile(TARGET_FILE)

        moveFileUtilsService.executionService.metaClass.executeCommand = {Realm realm, String cmd ->
            assert cmd == createFileCmdString(sourceFile, targetFile, "600")
        }
        assert shouldFail (AssertionError) {
            moveFileUtilsService.moveFileIfExists(realm, sourceFile, targetFile, "600")
        }.contains(sourceFile.path)
    }

    @Test
    void testMoveFileIfExists_WithPermissionMaskParameter_SourceExists_MovingSuccessful() {
        File sourceFile = testDirectory.newFile(SOURCE_FILE)
        File targetFile = testDirectory.newFile(TARGET_FILE)
        targetFile.delete()

        moveFileUtilsService.executionService.metaClass.executeCommand = {Realm realm, String cmd ->
            assert cmd == createFileCmdString(sourceFile, targetFile, "600")
            executeCommandLocally(cmd)
        }
        moveFileUtilsService.moveFileIfExists(realm, sourceFile, targetFile, "600")
        assert !sourceFile.exists()
        assert targetFile.exists()
    }

    @Test
    void testMoveFileIfExists_WithPermissionMaskParameter_MovingSuccessful_NoPermissionChange() {
        File sourceFile = testDirectory.newFile(SOURCE_FILE)
        File targetFile = testDirectory.newFile(TARGET_FILE)
        targetFile.delete()

        moveFileUtilsService.executionService.metaClass.executeCommand = {Realm realm, String cmd ->
            assert cmd == "umask 027; mkdir -m 2750 -p ${targetFile.parent}; mv -f ${sourceFile} ${targetFile}"
            executeCommandLocally(cmd)
        }
        moveFileUtilsService.moveFileIfExists(realm, sourceFile, targetFile, null)
        assert !sourceFile.exists()
        assert targetFile.exists()
    }



    @Test
    void testMoveDirContentIfExists_InputRealmIsNull_ShouldFail() {
        assert shouldFail (AssertionError) {
            moveFileUtilsService.moveDirContentIfExists(null, new File(SOURCE_FOLDER), new File(TARGET_FOLDER))
        }.contains("Input realm must not be null")
    }

    @Test
    void testMoveDirContentIfExists_InputSourceIsNull_ShouldFail() {
        assert shouldFail (AssertionError) {
            moveFileUtilsService.moveDirContentIfExists(realm, null, new File(TARGET_FOLDER))
        }.contains("Input sourceDir must not be null")
    }

    @Test
    void testMoveDirContentIfExists_InputTargetIsNull_ShouldFail() {
        assert shouldFail (AssertionError) {
            moveFileUtilsService.moveDirContentIfExists(realm, new File(SOURCE_FOLDER), null)
        }.contains("Input targetDir must not be null")
    }

    @Test
    void testMoveDirContentIfExists_SourceDirDoesNotExist_ShouldFail() {
        File targetFolder = testDirectory.newFolder(TARGET_FOLDER)
        File sourceFolder = testDirectory.newFolder(SOURCE_FOLDER)
        assert targetFolder.deleteDir()
        assert sourceFolder.deleteDir()

        moveFileUtilsService.executionService.metaClass.executeCommand = {Realm realm, String cmd ->
            assert false : "should not call this method at all since no copying is needed"
        }
        assert shouldFail (AssertionError) {
            moveFileUtilsService.moveDirContentIfExists(realm, sourceFolder, targetFolder)
        } ==~ /The source directory .* does not exist.*/
    }

    @Test
    void testMoveDirContentIfExists_FolderIsEmpty_NoCopying() {
        File targetFolder = testDirectory.newFolder(TARGET_FOLDER)
        File sourceFolder = testDirectory.newFolder(SOURCE_FOLDER)

        moveFileUtilsService.executionService.metaClass.executeCommand = {Realm realm, String cmd ->
            assert false : "should not call this method at all since no copying is needed"
        }

        moveFileUtilsService.moveDirContentIfExists(realm, sourceFolder, targetFolder)
    }

    @Test
    void testMoveDirContentIfExists_TargetDirWasNotCreated_ShouldFail() {
        File targetFolder = testDirectory.newFolder(TARGET_FOLDER)
        File sourceFolder = testDirectory.newFolder(SOURCE_FOLDER)
        File sourceFile = new File(sourceFolder, SOURCE_FILE)
        assert sourceFile.createNewFile()
        assert targetFolder.delete()

        moveFileUtilsService.executionService.metaClass.executeCommand = {Realm realm, String cmd ->
            assert cmd == createDirectoryCmdString(sourceFolder, targetFolder)
        }

        assert shouldFail (AssertionError) {
            moveFileUtilsService.moveDirContentIfExists(realm, sourceFolder, targetFolder)
        }.contains("WaitingFileUtils.waitUntilExists(targetDir)")
    }

    @Test
    void testMoveDirContentIfExists_FilesInSourceWhereNotCopiedToTarget_ShouldFail() {
        File targetFolder = testDirectory.newFolder(TARGET_FOLDER)
        File sourceFolder = testDirectory.newFolder(SOURCE_FOLDER)
        File sourceFile = new File(sourceFolder, SOURCE_FILE)
        sourceFile.createNewFile()

        moveFileUtilsService.executionService.metaClass.executeCommand = {Realm realm, String cmd ->
            assert cmd == createDirectoryCmdString(sourceFolder, targetFolder)
        }

        assert shouldFail (AssertionError) {
            moveFileUtilsService.moveDirContentIfExists(realm, sourceFolder, targetFolder)
        }.contains("assert ThreadUtils.waitFor({ (targetDir.list() as Set).containsAll(sourceDirContent) }")
    }

    @Test
    void testMoveDirContentIfExists_FilesAreStillInSourceAfterCopying_ShouldFail() {
        File targetFolder = testDirectory.newFolder(TARGET_FOLDER)
        File sourceFolder = testDirectory.newFolder(SOURCE_FOLDER)
        File sourceFile = new File(sourceFolder, SOURCE_FILE)
        sourceFile.createNewFile()

        moveFileUtilsService.executionService.metaClass.executeCommand = {Realm realm, String cmd ->
            assert cmd == createDirectoryCmdString(sourceFolder, targetFolder)
            File targetFile = new File(targetFolder, SOURCE_FILE)
            targetFile.createNewFile()
        }

        assert shouldFail (AssertionError) {
            moveFileUtilsService.moveDirContentIfExists(realm, sourceFolder, targetFolder)
        }.contains("assert ThreadUtils.waitFor({ sourceDir.list().size() == 0 }")
    }

    @Test
    void testMoveDirContentIfExists_SourceExists_CopyingSuccessful() {
        File targetFolder = testDirectory.newFolder(TARGET_FOLDER)
        File sourceFolder = testDirectory.newFolder(SOURCE_FOLDER)
        File sourceFile = new File(sourceFolder, SOURCE_FILE)
        sourceFile.createNewFile()

        moveFileUtilsService.executionService.metaClass.executeCommand = {Realm realm, String cmd ->
            assert cmd == createDirectoryCmdString(sourceFolder, targetFolder)
            executeCommandLocally(cmd)
        }

        moveFileUtilsService.moveDirContentIfExists(realm, sourceFolder, targetFolder)
    }

    @Test
    void testMoveDirContentIfExists_TargetDirAlreadyExistedWithDifferentContent() {
        File targetFolder = testDirectory.newFolder(TARGET_FOLDER)
        File sourceFolder = testDirectory.newFolder(SOURCE_FOLDER)
        File sourceFile = new File(sourceFolder, SOURCE_FILE)
        sourceFile << SOURCE_FILE
        File targetFile = new File(targetFolder, TARGET_FILE)
        targetFile << TARGET_FILE
        File targetFile_ExistsAlready = new File(targetFolder, SOURCE_FILE)
        targetFile_ExistsAlready << TARGET_FILE

        moveFileUtilsService.executionService.metaClass.executeCommand = {Realm realm, String cmd ->
            assert cmd == createDirectoryCmdString(sourceFolder, targetFolder)
            executeCommandLocally(cmd)
        }

        assert targetFile_ExistsAlready.text == TARGET_FILE
        moveFileUtilsService.moveDirContentIfExists(realm, sourceFolder, targetFolder)
        assert targetFile_ExistsAlready.text == SOURCE_FILE
        assert WaitingFileUtils.waitUntilExists(targetFile)
    }


    private executeCommandLocally(String cmd) {
        [ 'bash', '-c', cmd ].execute().waitFor()
    }

    private String createFileCmdString(File source, File target, String filePermission) {
        return "umask 027; mkdir -m 2750 -p ${target.parent}; mv -f ${source} ${target}; chmod ${filePermission} ${target}"
    }

    private String createDirectoryCmdString(File source, File target) {
        return "umask 027; mkdir -m 2750 -p ${target}; mv -f ${source}/* ${target}"
    }
}
