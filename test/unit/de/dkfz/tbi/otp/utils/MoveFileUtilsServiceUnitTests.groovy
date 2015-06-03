package de.dkfz.tbi.otp.utils

import de.dkfz.tbi.otp.job.processing.ExecutionService
import de.dkfz.tbi.otp.ngsdata.Realm
import groovy.io.FileType
import org.junit.After
import org.junit.Before
import grails.buildtestdata.mixin.Build
import org.junit.Rule
import org.junit.rules.TemporaryFolder

/**
 */
@Build([Realm])
class MoveFileUtilsServiceUnitTests {


    Realm realm

    MoveFileUtilsService moveFileUtilsService

    final String SOURCE_FILE = "sourceFile"
    final String SOURCE_FOLDER = "sourceFolder"
    final String TARGET_FILE = "targetFile"
    final String TARGET_FOLDER = "targetFolder"

    @Rule
    public TemporaryFolder testDirectory= new TemporaryFolder();

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

    void testMoveFileIfExists_InputRealmIsNull_ShouldFail() {
        shouldFail (AssertionError) {
            moveFileUtilsService.moveFileIfExists(null, new File(SOURCE_FILE), new File(TARGET_FILE))
        }
    }

    void testMoveFileIfExists_InputSourceIsNull_ShouldFail() {
        shouldFail (AssertionError) {
            moveFileUtilsService.moveFileIfExists(realm, null, new File(TARGET_FILE))
        }
    }

    void testMoveFileIfExists_InputTargetIsNull_ShouldFail() {
        shouldFail (AssertionError) {
            moveFileUtilsService.moveFileIfExists(realm, new File(SOURCE_FILE), null)
        }
    }

    void testMoveFileIfExists_SourceDoesNotExist_NoCopying() {
        File targetFile = testDirectory.newFile(TARGET_FILE)

        moveFileUtilsService.executionService.metaClass.executeCommand = {Realm realm, String cmd ->
            assert false : "should not call this method at all since no copying is needed"
        }

        moveFileUtilsService.moveFileIfExists(realm, new File("sourceFileDoesNotExist"), targetFile)
    }

    void testMoveFileIfExists_TargetDoesNotExistsAfterCopying_ShouldFail() {
        File sourceFile = testDirectory.newFile(SOURCE_FILE)
        File targetFile = new File("targetFileDoesNotExist")

        moveFileUtilsService.executionService.metaClass.executeCommand = {Realm realm, String cmd ->
            assert cmd == "mv ${sourceFile.path} ${targetFile.path}; chmod 640 ${targetFile}"
            sourceFile.delete()
        }
        shouldFail (AssertionError) {
            moveFileUtilsService.moveFileIfExists(realm, sourceFile, targetFile)
        }
    }

    void testMoveFileIfExists_SourceStillExistsAfterCopying_ShouldFail() {
        File sourceFile = testDirectory.newFile(SOURCE_FILE)
        File targetFile = testDirectory.newFile(TARGET_FILE)

        moveFileUtilsService.executionService.metaClass.executeCommand = {Realm realm, String cmd ->
            assert cmd == "mv ${sourceFile} ${targetFile}; chmod 640 ${targetFile}"
        }
        shouldFail (AssertionError) {
            moveFileUtilsService.moveFileIfExists(realm, sourceFile, targetFile)
        }
    }

    void testMoveFileIfExists_SourceExists_CopyingSuccessful() {
        File sourceFile = testDirectory.newFile(SOURCE_FILE)
        File targetFile = testDirectory.newFile(TARGET_FILE)
        targetFile.delete()

        moveFileUtilsService.executionService.metaClass.executeCommand = {Realm realm, String cmd ->
            assert cmd == "mkdir -m 2750 -p ${targetFile.parent}; mv -f ${sourceFile.path} ${targetFile.path}; chmod 640 ${targetFile}"
            executeCommandLocally(cmd)
        }
        moveFileUtilsService.moveFileIfExists(realm, sourceFile, targetFile)
        assert WaitingFileUtils.confirmExists(targetFile)
    }

    void testMoveFileIfExists_OlderTargetExistsAlready_OverwriteSuccessful() {
        File sourceFolder = testDirectory.newFolder(SOURCE_FOLDER)
        File sourceFile = new File(sourceFolder, SOURCE_FILE)
        sourceFile << SOURCE_FILE
        File targetFolder = testDirectory.newFolder(TARGET_FOLDER)
        File targetFile_allreadyExists = new File(targetFolder, SOURCE_FILE)
        targetFile_allreadyExists.text = TARGET_FILE

        moveFileUtilsService.executionService.metaClass.executeCommand = {Realm realm, String cmd ->
            assert cmd == "mkdir -m 2750 -p ${targetFile_allreadyExists.parent}; mv -f ${sourceFile.path} ${targetFile_allreadyExists.path}; chmod 640 ${targetFile_allreadyExists}"
            executeCommandLocally(cmd)
        }

        assert targetFile_allreadyExists.text == TARGET_FILE
        moveFileUtilsService.moveFileIfExists(realm, sourceFile, targetFile_allreadyExists)
        assert WaitingFileUtils.confirmExists(targetFile_allreadyExists)
        assert targetFile_allreadyExists.text == SOURCE_FILE
    }

    void testMoveFileIfExists_CopyingSuccessful_ReadPermissionsForAll() {
        File sourceFile = testDirectory.newFile(SOURCE_FILE)
        File targetFile = testDirectory.newFile(TARGET_FILE)
        targetFile.delete()

        moveFileUtilsService.executionService.metaClass.executeCommand = {Realm realm, String cmd ->
            assert cmd == "mkdir -m 2750 -p ${targetFile.parent}; mv -f ${sourceFile.path} ${targetFile.path}; chmod 644 ${targetFile}"
            executeCommandLocally(cmd)
        }
        moveFileUtilsService.moveFileIfExists(realm, sourceFile, targetFile, true)
        assert WaitingFileUtils.confirmExists(targetFile)
    }



    void testMoveDirContentIfExists_InputRealmIsNull_ShouldFail() {
        shouldFail (AssertionError) {
            moveFileUtilsService.moveDirContentIfExists(null, new File(SOURCE_FOLDER), new File(TARGET_FOLDER))
        }
    }

    void testMoveDirContentIfExists_InputSourceIsNull_ShouldFail() {
        shouldFail (AssertionError) {
            moveFileUtilsService.moveDirContentIfExists(realm, null, new File(TARGET_FOLDER))
        }
    }

    void testMoveDirContentIfExists_InputTargetIsNull_ShouldFail() {
        shouldFail (AssertionError) {
            moveFileUtilsService.moveDirContentIfExists(realm, new File(SOURCE_FOLDER), null)
        }
    }

    void testMoveDirContentIfExists_SourceDirDoesNotExist_ShouldFail() {
        File targetFolder = testDirectory.newFolder(TARGET_FOLDER)
        File sourceFolder = testDirectory.newFolder(SOURCE_FOLDER)
        assert targetFolder.deleteDir()
        assert sourceFolder.deleteDir()

        moveFileUtilsService.executionService.metaClass.executeCommand = {Realm realm, String cmd ->
            assert false : "should not call this method at all since no copying is needed"
        }
        shouldFail (AssertionError) {
            moveFileUtilsService.moveDirContentIfExists(realm, sourceFolder, targetFolder)
        }
    }

    void testMoveDirContentIfExists_FolderIsEmpty_NoCopying() {
        File targetFolder = testDirectory.newFolder(TARGET_FOLDER)
        File sourceFolder = testDirectory.newFolder(SOURCE_FOLDER)

        moveFileUtilsService.executionService.metaClass.executeCommand = {Realm realm, String cmd ->
            assert false : "should not call this method at all since no copying is needed"
        }

        moveFileUtilsService.moveDirContentIfExists(realm, sourceFolder, targetFolder)
    }

    void testMoveDirContentIfExists_FilesInSourceWhereNotCopiedToTarget_ShouldFail() {
        File targetFolder = testDirectory.newFolder(TARGET_FOLDER)
        File sourceFolder = testDirectory.newFolder(SOURCE_FOLDER)
        File sourceFile = new File(sourceFolder, SOURCE_FILE)
        sourceFile.createNewFile()

        moveFileUtilsService.executionService.metaClass.executeCommand = {Realm realm, String cmd ->
            assert cmd == "mv ${sourceFolder}/* ${targetFolder}"
        }

        shouldFail (AssertionError) {
            moveFileUtilsService.moveDirContentIfExists(realm, sourceFolder, targetFolder)
        }
    }

    void testMoveDirContentIfExists_FilesAreStillInSourceAfterCopying_ShouldFail() {
        File targetFolder = testDirectory.newFolder(TARGET_FOLDER)
        File sourceFolder = testDirectory.newFolder(SOURCE_FOLDER)
        File sourceFile = new File(sourceFolder, SOURCE_FILE)
        sourceFile.createNewFile()

        moveFileUtilsService.executionService.metaClass.executeCommand = {Realm realm, String cmd ->
            assert cmd == "mv ${sourceFolder}/* ${targetFolder}"
            File targetFile = new File(targetFolder, SOURCE_FILE)
            targetFile.createNewFile()
        }

        shouldFail (AssertionError) {
            moveFileUtilsService.moveDirContentIfExists(realm, sourceFolder, targetFolder)
        }
    }

    void testMoveDirContentIfExists_SourceExists_CopyingSuccessful() {
        File targetFolder = testDirectory.newFolder(TARGET_FOLDER)
        File sourceFolder = testDirectory.newFolder(SOURCE_FOLDER)
        File sourceFile = new File(sourceFolder, SOURCE_FILE)
        sourceFile.createNewFile()

        moveFileUtilsService.executionService.metaClass.executeCommand = {Realm realm, String cmd ->
            assert cmd == "mkdir -m 2750 -p ${targetFolder}; mv -f ${sourceFolder}/* ${targetFolder}"
            executeCommandLocally(cmd)
        }

        moveFileUtilsService.moveDirContentIfExists(realm, sourceFolder, targetFolder)
    }

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
            assert cmd == "mkdir -m 2750 -p ${targetFolder}; mv -f ${sourceFolder}/* ${targetFolder}"
            executeCommandLocally(cmd)
        }

        assert targetFile_ExistsAlready.text == TARGET_FILE
        moveFileUtilsService.moveDirContentIfExists(realm, sourceFolder, targetFolder)
        assert targetFile_ExistsAlready.text == SOURCE_FILE
        WaitingFileUtils.confirmExists(targetFile)
    }


    private executeCommandLocally(String cmd) {
        [ 'bash', '-c', cmd ].execute().waitFor()
    }

}
