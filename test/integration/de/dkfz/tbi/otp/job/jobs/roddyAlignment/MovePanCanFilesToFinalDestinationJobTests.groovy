package de.dkfz.tbi.otp.job.jobs.roddyAlignment

import org.apache.commons.logging.impl.NoOpLog

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.AbstractMergedBamFile
import de.dkfz.tbi.otp.dataprocessing.RoddyBamFile
import de.dkfz.tbi.otp.job.processing.CreateClusterScriptService
import de.dkfz.tbi.otp.job.processing.ExecutionHelperService
import de.dkfz.tbi.otp.job.processing.ExecutionService
import de.dkfz.tbi.otp.ngsdata.ConfigService
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.ngsdata.Realm
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.utils.CreateRoddyFileHelper
import de.dkfz.tbi.otp.utils.ExecuteRoddyCommandService
import de.dkfz.tbi.otp.utils.HelperUtils
import de.dkfz.tbi.otp.utils.ProcessHelperService
import grails.validation.ValidationException
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 */
class MovePanCanFilesToFinalDestinationJobTests {

    final static int COUNT_FILES = 2
    final static int COUNT_DIRS = 3

    MovePanCanFilesToFinalDestinationJob movePanCanFilesToFinalDestinationJob

    RoddyBamFile roddyBamFile
    Realm realm

    final static String WRONG_COMMAND = "The command is wrong"

    final static String SOME_GROUP = "GROUP"

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder()

    @Before
    void setUp() {
        temporaryFolder.create() //BUG in JUnit, remove after grails update
        roddyBamFile = DomainFactory.createRoddyBamFile()
        roddyBamFile.md5sum = null
        roddyBamFile.fileSize = -1
        roddyBamFile.fileOperationStatus = AbstractMergedBamFile.FileOperationStatus.NEEDS_PROCESSING
        roddyBamFile.roddyExecutionDirectoryNames = ["exec_123456_123456789_test_test"]
        assert roddyBamFile.save(flush: true, failOnError: true)
        realm = DomainFactory.createRealmDataManagementDKFZ()
        realm.rootPath = temporaryFolder.newFolder()
        assert realm.save(flush: true, failOnError: true)
        roddyBamFile.project.realmName = realm.name
        assert roddyBamFile.project.save(flush: true, failOnError: true)
        SeqTrack seqTrack = roddyBamFile.seqTracks.iterator()[0]
        DomainFactory.createRoddyProcessingOptions(temporaryFolder.newFolder())

        ProcessHelperService.metaClass.static.executeCommandAndAssertExistCodeAndReturnProcessOutput = {String cmd ->
            assert cmd ==~ "cd /tmp && sudo -u OtherUnixUser ${temporaryFolder.getRoot()}/.*/correctPathPermissionsOtherUnixUserRemoteWrapper.sh ${temporaryFolder.getRoot()}/.*/merged-alignment"
            return new ProcessHelperService.ProcessOutput('', '', 0)
        }
    }

    @After
    void tearDown() {
        TestCase.removeMetaClass(MovePanCanFilesToFinalDestinationJob, movePanCanFilesToFinalDestinationJob)
        TestCase.removeMetaClass(ConfigService, movePanCanFilesToFinalDestinationJob.configService)
        TestCase.removeMetaClass(ExecutionService, movePanCanFilesToFinalDestinationJob.executionService)
        TestCase.removeMetaClass(ExecutionHelperService, movePanCanFilesToFinalDestinationJob.executionHelperService)
        TestCase.removeMetaClass(ExecuteRoddyCommandService, movePanCanFilesToFinalDestinationJob.executeRoddyCommandService)
        TestCase.removeMetaClass(CreateClusterScriptService, movePanCanFilesToFinalDestinationJob.createClusterScriptService)
        GroovySystem.metaClassRegistry.removeMetaClass(ProcessHelperService)
    }

    void setUp_allFine() {
        movePanCanFilesToFinalDestinationJob.metaClass.getProcessParameterObject = { -> roddyBamFile }
        movePanCanFilesToFinalDestinationJob.metaClass.cleanupWorkDirectory = { RoddyBamFile roddyBamFile, Realm realm -> }
        movePanCanFilesToFinalDestinationJob.metaClass.deleteOldLinks = { RoddyBamFile roddyBamFile, Realm realm -> }
        movePanCanFilesToFinalDestinationJob.metaClass.linkNewResults = { RoddyBamFile roddyBamFile, Realm realm -> }
        movePanCanFilesToFinalDestinationJob.metaClass.cleanupOldResults = { RoddyBamFile roddyBamFile, Realm realm -> }
        movePanCanFilesToFinalDestinationJob.executeRoddyCommandService.metaClass.correctGroups = { RoddyBamFile roddyBamFile -> }
        movePanCanFilesToFinalDestinationJob.executionHelperService.metaClass.setPermission = {Realm realm, File directory, String group -> }
        movePanCanFilesToFinalDestinationJob.executionHelperService.metaClass.getGroup = {File directory -> SOME_GROUP }
        movePanCanFilesToFinalDestinationJob.executionHelperService.metaClass.setGroup = {Realm realm, File directory, String group -> }
    }

    @Test
    void testCleanupWorkDirectory_allFine_withTmpFilesAndTmpDirs() {
        helper_testCleanupWorkDirectory_allFine(1, 1)
    }

    @Test
    void testCleanupWorkDirectory_allFine_withTmpFilesAndNoTmpDirs() {
        helper_testCleanupWorkDirectory_allFine(1, 0)
    }

    @Test
    void testCleanupWorkDirectory_allFine_withNoTmpFilesAndSomeTmpDirs() {
        helper_testCleanupWorkDirectory_allFine(0, 1)
    }

    @Test
    void testCleanupWorkDirectory_allFine_withNoTmpFilesAndNoTmpDirs() {
        helper_testCleanupWorkDirectory_allFine(0, 0)
    }

    @Test
    void testCleanupWorkDirectory_allFine_withManyTmpFilesAndAndManyTmpDirs() {
        helper_testCleanupWorkDirectory_allFine(COUNT_FILES, COUNT_DIRS)
    }

    @Test
    void testCleanupWorkDirectory_bamFileIsNull_shouldFail() {
        TestCase.shouldFailWithMessageContaining(AssertionError, "roddyBamFile") {
            movePanCanFilesToFinalDestinationJob.cleanupWorkDirectory(null, realm)
        }
    }

    @Test
    void testCleanupWorkDirectory_realmIsNull_shouldFail() {
        TestCase.shouldFailWithMessageContaining(AssertionError, "realm") {
            movePanCanFilesToFinalDestinationJob.cleanupWorkDirectory(roddyBamFile, null)
        }
    }


    @Test
    void testCleanupWorkDirectory_bamHasOldStructure_shouldFail() {
        roddyBamFile.workDirectoryName = null
        roddyBamFile.save(flush: true, failOnError: true)

        TestCase.shouldFailWithMessageContaining(AssertionError, "isOldStructureUsed") {
            movePanCanFilesToFinalDestinationJob.cleanupWorkDirectory(roddyBamFile, realm)
        }
    }

    @Test
    void testCleanupWorkDirectory_deleteContentOfOtherUnixUserDirectoryThrowsException_shouldFail() {
        final String FAIL_MESSAGE = HelperUtils.uniqueString

        movePanCanFilesToFinalDestinationJob.executeRoddyCommandService.metaClass.deleteContentOfOtherUnixUserDirectory = {File basePath ->
            assert false: FAIL_MESSAGE
        }
        File file = new File(roddyBamFile.workDirectory, HelperUtils.uniqueString)
        assert file.mkdirs()

        TestCase.shouldFailWithMessageContaining(AssertionError, FAIL_MESSAGE) {
            movePanCanFilesToFinalDestinationJob.cleanupWorkDirectory(roddyBamFile, realm)
        }
    }

    @Test
    void testCleanupWorkDirectory_removeDirsThrowsException_shouldFail() {
        assert roddyBamFile.workDirectory.mkdirs()
        assert new File(roddyBamFile.workDirectory, HelperUtils.uniqueString).createNewFile()
        final String FAIL_MESSAGE = HelperUtils.uniqueString

        movePanCanFilesToFinalDestinationJob.createClusterScriptService.metaClass.removeDirs = {Collection<File> dirs, CreateClusterScriptService.RemoveOption option ->
            assert false: FAIL_MESSAGE
        }

        TestCase.shouldFailWithMessageContaining(AssertionError, FAIL_MESSAGE) {
            movePanCanFilesToFinalDestinationJob.cleanupWorkDirectory(roddyBamFile, realm)
        }
    }

    @Test
    void testCleanupWorkDirectory_executeCommandThrowsException_shouldFail() {
        assert roddyBamFile.workDirectory.mkdirs()
        assert new File(roddyBamFile.workDirectory, HelperUtils.uniqueString).createNewFile()
        final String FAIL_MESSAGE = HelperUtils.uniqueString

        movePanCanFilesToFinalDestinationJob.executionService.metaClass.executeCommand = {Realm realm, String command ->
            assert false: FAIL_MESSAGE
        }

        TestCase.shouldFailWithMessageContaining(AssertionError, FAIL_MESSAGE) {
            movePanCanFilesToFinalDestinationJob.cleanupWorkDirectory(roddyBamFile, realm)
        }
    }

    private void helper_testCleanupWorkDirectory_allFine(int countTmpFiles, int countTmpDir) {
        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(realm, roddyBamFile)
        int callDeletedRoddy = 0
        boolean callDeleted = false
        List<File> filesNotToBeCalledFor = [
                roddyBamFile.workBamFile,
                roddyBamFile.workBaiFile,
                roddyBamFile.workMd5sumFile,
                roddyBamFile.workQADirectory,
                roddyBamFile.workExecutionStoreDirectory,
        ]

        List<File> tmpFiles = []
        countTmpFiles.times {
            tmpFiles << File.createTempFile("tmp", ".tmp", roddyBamFile.workDirectory)
        }
        assert countTmpFiles == tmpFiles.size()

        List<File> tmpDirectories = []
        countTmpDir.times {
            File file = new File(roddyBamFile.workDirectory, HelperUtils.uniqueString)
            assert file.mkdir()
            tmpDirectories << file
        }
        assert countTmpDir == tmpDirectories.size()

        movePanCanFilesToFinalDestinationJob.executeRoddyCommandService.metaClass.deleteContentOfOtherUnixUserDirectory = { File file ->
            assert roddyBamFile.workDirectory == file.parentFile
            assert !filesNotToBeCalledFor.contains(file)
            assert tmpDirectories.contains(file)
            callDeletedRoddy++
        }

        movePanCanFilesToFinalDestinationJob.executionService.metaClass.executeCommand = { Realm realm, String command ->
            assert filesNotToBeCalledFor.every{
                !command.contains(it.path)
            }
            assert tmpDirectories.every {
                command.contains(it.path)
            }
            assert tmpFiles.every {
                command.contains(it.path)
            }
            String stdout = ProcessHelperService.executeAndAssertExitCodeAndErrorOutAndReturnStdout(command)
            assert stdout.trim() == '0'
            callDeleted = true
            return stdout
        }
        assert filesNotToBeCalledFor.size() + tmpFiles.size() + tmpDirectories.size() == roddyBamFile.workDirectory.listFiles().size()

        movePanCanFilesToFinalDestinationJob.cleanupWorkDirectory(roddyBamFile, realm)

        assert callDeleted  == (countTmpDir + countTmpFiles > 0)
        assert countTmpDir == callDeletedRoddy
        tmpDirectories.each {
            assert !it.exists()
        }
        filesNotToBeCalledFor.each {
            assert it.exists()
        }
    }



    @Test
    void testLinkNewResults_allFine() {
        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(realm, roddyBamFile)

        List<File> linkedFiles = [
                roddyBamFile.finalBamFile,
                roddyBamFile.finalBaiFile,
                roddyBamFile.finalMd5sumFile,
                roddyBamFile.finalMergedQADirectory,
                roddyBamFile.finalMergedQAJsonFile,
                roddyBamFile.getFinalExecutionDirectories(),
                roddyBamFile.finalSingleLaneQADirectories.values(),
                roddyBamFile.finalSingleLaneQAJsonFiles.values(),
        ].flatten()

        movePanCanFilesToFinalDestinationJob.executionService.metaClass.executeCommand = { Realm realm, String command ->
            return ProcessHelperService.executeAndAssertExitCodeAndErrorOutAndReturnStdout(command)
        }

        linkedFiles.each {
            assert !it.exists()
        }

        movePanCanFilesToFinalDestinationJob.linkNewResults(roddyBamFile, realm)

        linkedFiles.each {
            assert it.exists()
        }
    }


    @Test
    void testLinkNewResults_bamFileIsNull_shouldFail() {
        TestCase.shouldFailWithMessageContaining(AssertionError, "roddyBamFile") {
            movePanCanFilesToFinalDestinationJob.linkNewResults(null, realm)
        }
    }

    @Test
    void testLinkNewResults_realmIsNull_shouldFail() {
        TestCase.shouldFailWithMessageContaining(AssertionError, "realm") {
            movePanCanFilesToFinalDestinationJob.linkNewResults(roddyBamFile, null)
        }
    }

    @Test
    void testLinkNewResults_bamHasOldStructure_shouldFail() {
        roddyBamFile.workDirectoryName = null
        roddyBamFile.save(flush: true, failOnError: true)

        TestCase.shouldFailWithMessageContaining(AssertionError, "isOldStructureUsed") {
            movePanCanFilesToFinalDestinationJob.linkNewResults(roddyBamFile, realm)
        }
    }

    @Test
    void testLinkNewResults_deleteContentOfOtherUnixUserDirectoryThrowsException_shouldFail() {
        final String FAIL_MESSAGE = HelperUtils.uniqueString

        movePanCanFilesToFinalDestinationJob.linkFileUtils.metaClass.createAndValidateLinks = { Map<File, File> sourceLinkMap, Realm realm ->
            assert false: FAIL_MESSAGE
        }

        TestCase.shouldFailWithMessageContaining(AssertionError, FAIL_MESSAGE) {
            movePanCanFilesToFinalDestinationJob.linkNewResults(roddyBamFile, realm)
        }
    }



    @Test
    void testCleanupOldResults_withBaseBamFile_allFine() {
        finishOperationStateOfRoddyBamFile(roddyBamFile)
        RoddyBamFile roddyBamFile2 = DomainFactory.createRoddyBamFile(roddyBamFile)
        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(realm, roddyBamFile)
        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(realm, roddyBamFile2)
        boolean hasCalled_deleteContentOfOtherUnixUserDirectory = false

        movePanCanFilesToFinalDestinationJob.executionService.metaClass.executeCommand = { Realm realm, String command ->
            return ProcessHelperService.executeAndAssertExitCodeAndErrorOutAndReturnStdout(command)
        }

        movePanCanFilesToFinalDestinationJob.executeRoddyCommandService.metaClass.deleteContentOfOtherUnixUserDirectory = { File basePath ->
            hasCalled_deleteContentOfOtherUnixUserDirectory = true
            assert basePath.exists()
            if (basePath.isDirectory()) {
                assert basePath.deleteDir()
            } else {
                assert false: 'Method may only called for directories'
            }
        }

        List<File> filesToDelete = [
                roddyBamFile.workBamFile,
                roddyBamFile.workBaiFile,
                roddyBamFile.workMergedQADirectory,
                ]
        List<File> filesToKeep = [
                roddyBamFile.workMd5sumFile,
                roddyBamFile.workExecutionDirectories,
                roddyBamFile.workSingleLaneQADirectories.values(),
        ].flatten()
        [filesToKeep, filesToDelete].flatten().each {
            assert it.exists()
        }

        movePanCanFilesToFinalDestinationJob.cleanupOldResults(roddyBamFile2, realm)

        assert hasCalled_deleteContentOfOtherUnixUserDirectory

        filesToDelete.each {
            assert !it.exists()
        }
        filesToKeep.each {
            assert it.exists()
        }
    }

    @Test
    void testCleanupOldResults_withBaseBamFileOfOldStructure_allFine() {
        finishOperationStateOfRoddyBamFile(roddyBamFile)
        roddyBamFile.workDirectoryName = null
        roddyBamFile.save(flush: true, failOnError: true)
        RoddyBamFile roddyBamFile2 = DomainFactory.createRoddyBamFile(roddyBamFile)
        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(realm, roddyBamFile2)

        movePanCanFilesToFinalDestinationJob.executionService.metaClass.executeCommand = { Realm realm, String command ->
            assert false: 'should not be called'
        }

        movePanCanFilesToFinalDestinationJob.executeRoddyCommandService.metaClass.deleteContentOfOtherUnixUserDirectory = { File basePath ->
            assert false: 'should not be called'
        }

        movePanCanFilesToFinalDestinationJob.cleanupOldResults(roddyBamFile2, realm)
    }

    @Test
    void testCleanupOldResults_withoutBaseBamFile_butBamFilesOfWorkPackageExist_allFine() {
        DomainFactory.createRoddyBamFile(workPackage:  roddyBamFile.workPackage)
        RoddyBamFile roddyBamFile2 = DomainFactory.createRoddyBamFile(workPackage:  roddyBamFile.workPackage)
        CreateRoddyFileHelper.createRoddyAlignmentFinalResultFiles(realm, roddyBamFile)
        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(realm, roddyBamFile)
        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(realm, roddyBamFile2)
        boolean hasCalled_deleteContentOfOtherUnixUserDirectory = false
        assert roddyBamFile.workDirectory.exists()

        movePanCanFilesToFinalDestinationJob.executionService.metaClass.executeCommand = { Realm realm, String command ->
            return ProcessHelperService.executeAndAssertExitCodeAndErrorOutAndReturnStdout(command)
        }

        movePanCanFilesToFinalDestinationJob.executeRoddyCommandService.metaClass.deleteContentOfOtherUnixUserDirectory = { File basePath ->
            hasCalled_deleteContentOfOtherUnixUserDirectory = true
            assert basePath.exists()
            assert basePath.isDirectory()
            assert basePath.deleteDir()
        }

        movePanCanFilesToFinalDestinationJob.cleanupOldResults(roddyBamFile2, realm)

        assert hasCalled_deleteContentOfOtherUnixUserDirectory
        assert !roddyBamFile.workDirectory.exists()
        assert roddyBamFile2.workDirectory.exists()
        assert roddyBamFile.finalExecutionStoreDirectory.listFiles().length == 0
        assert [null, roddyBamFile.finalMergedQADirectory].contains(CollectionUtils.atMostOneElement(roddyBamFile.finalQADirectory.listFiles() as List))
    }

    @Test
    void testCleanupOldResults_withoutBaseBamFile_butBamFilesOfWorkPackageExistInOldStructure_latestIsOld_allFine() {
        DomainFactory.createRoddyBamFile(workPackage:  roddyBamFile.workPackage, workDirectoryName: null)
        helper_testCleanupOldResults_withoutBaseBamFile_butBamFilesOfWorkPackageExistInOldStructure(true)
    }

    @Test
    void testCleanupOldResults_withoutBaseBamFile_butBamFilesOfWorkPackageExistInOldAndNewStructure_latestIsNew_allFine() {
        DomainFactory.createRoddyBamFile(workPackage:  roddyBamFile.workPackage)
        helper_testCleanupOldResults_withoutBaseBamFile_butBamFilesOfWorkPackageExistInOldStructure(false)
    }

    void helper_testCleanupOldResults_withoutBaseBamFile_butBamFilesOfWorkPackageExistInOldStructure(boolean latestIsOld) {
        roddyBamFile.workDirectoryName = null
        roddyBamFile.save(flush: true, failOnError: true)
        RoddyBamFile roddyBamFile2 = DomainFactory.createRoddyBamFile(workPackage:  roddyBamFile.workPackage)
        CreateRoddyFileHelper.createRoddyAlignmentFinalResultFiles(realm, roddyBamFile)
        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(realm, roddyBamFile2)
        assert roddyBamFile2.workDirectory.exists()
        boolean isCalledForMergedQaDirectory = false

        movePanCanFilesToFinalDestinationJob.executionService.metaClass.executeCommand = { Realm realm, String command ->
            assert latestIsOld == !command.contains(RoddyBamFile.WORK_DIR_PREFIX)
            return ProcessHelperService.executeAndAssertExitCodeAndErrorOutAndReturnStdout(command)
        }

        movePanCanFilesToFinalDestinationJob.executeRoddyCommandService.metaClass.deleteContentOfOtherUnixUserDirectory = { File basePath ->
            assert !basePath.absolutePath.contains(RoddyBamFile.WORK_DIR_PREFIX)
            assert basePath.deleteDir()
            if (basePath.path.contains("${RoddyBamFile.QUALITY_CONTROL_DIR}/${RoddyBamFile.MERGED_DIR}")) {
                isCalledForMergedQaDirectory = true
            }
        }

        movePanCanFilesToFinalDestinationJob.cleanupOldResults(roddyBamFile2, realm)
        assert roddyBamFile2.workDirectory.exists()
        roddyBamFile.finalExecutionDirectories.each {
            assert !it.exists()
        }
        roddyBamFile.finalSingleLaneQADirectories.values().each {
            assert !it.exists()
        }
        assert latestIsOld == isCalledForMergedQaDirectory
    }

    @Test
    void testCleanupOldResults_withoutBaseBamFileAndWithoutOtherBamFilesOfTheSameWorkPackage_allFine() {
        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(realm, roddyBamFile)

        movePanCanFilesToFinalDestinationJob.executionService.metaClass.executeCommand = { Realm realm, String command ->
            assert false: 'should not be called'
        }

        movePanCanFilesToFinalDestinationJob.executeRoddyCommandService.metaClass.deleteContentOfOtherUnixUserDirectory = { File basePath ->
            assert false: 'should not be called'
        }

        movePanCanFilesToFinalDestinationJob.cleanupOldResults(roddyBamFile, realm)

        assert roddyBamFile.workDirectory.exists()
    }

    @Test
    void testCleanupOldResults_bamFileIsNull_shouldFail() {
        TestCase.shouldFailWithMessageContaining(AssertionError, "roddyBamFile") {
            movePanCanFilesToFinalDestinationJob.cleanupOldResults(null, realm)
        }
    }

    @Test
    void testCleanupOldResults_realmIsNull_shouldFail() {
        TestCase.shouldFailWithMessageContaining(AssertionError, "realm") {
            movePanCanFilesToFinalDestinationJob.cleanupOldResults(roddyBamFile, null)
        }
    }

    @Test
    void testCleanupOldResults_bamHasOldStructure_shouldFail() {
        roddyBamFile.workDirectoryName = null
        roddyBamFile.save(flush: true, failOnError: true)

        TestCase.shouldFailWithMessageContaining(AssertionError, "isOldStructureUsed") {
            movePanCanFilesToFinalDestinationJob.cleanupOldResults(roddyBamFile, realm)
        }
    }

    @Test
    void testCleanupOldResults_deleteContentOfOtherUnixUserDirectoryThrowsException_shouldFail() {
        final String FAIL_MESSAGE = HelperUtils.uniqueString
        finishOperationStateOfRoddyBamFile(roddyBamFile)
        RoddyBamFile roddyBamFile2 = DomainFactory.createRoddyBamFile(roddyBamFile)
        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(realm, roddyBamFile)
        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(realm, roddyBamFile2)

        movePanCanFilesToFinalDestinationJob.executeRoddyCommandService.metaClass.deleteContentOfOtherUnixUserDirectory = { File basePath ->
            assert false: FAIL_MESSAGE
        }

        TestCase.shouldFailWithMessageContaining(AssertionError, FAIL_MESSAGE) {
            movePanCanFilesToFinalDestinationJob.cleanupOldResults(roddyBamFile2, realm)
        }
    }

    @Test
    void testCleanupOldResults_removeDirsThrowsException_shouldFail() {
        final String FAIL_MESSAGE = HelperUtils.uniqueString
        finishOperationStateOfRoddyBamFile(roddyBamFile)
        RoddyBamFile roddyBamFile2 = DomainFactory.createRoddyBamFile(roddyBamFile)
        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(realm, roddyBamFile)
        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(realm, roddyBamFile2)

        movePanCanFilesToFinalDestinationJob.executeRoddyCommandService.metaClass.deleteContentOfOtherUnixUserDirectory = { File basePath -> }

        movePanCanFilesToFinalDestinationJob.createClusterScriptService.metaClass.removeDirs = { Collection<File> dirs, CreateClusterScriptService.RemoveOption option ->
            assert false: FAIL_MESSAGE
        }

        TestCase.shouldFailWithMessageContaining(AssertionError, FAIL_MESSAGE) {
            movePanCanFilesToFinalDestinationJob.cleanupOldResults(roddyBamFile2, realm)
        }
    }

    @Test
    void testCleanupOldResults_executeCommandThrowsException_shouldFail() {
        final String FAIL_MESSAGE = HelperUtils.uniqueString
        finishOperationStateOfRoddyBamFile(roddyBamFile)
        RoddyBamFile roddyBamFile2 = DomainFactory.createRoddyBamFile(roddyBamFile)
        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(realm, roddyBamFile)
        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(realm, roddyBamFile2)

        movePanCanFilesToFinalDestinationJob.executeRoddyCommandService.metaClass.deleteContentOfOtherUnixUserDirectory = { File basePath -> }

        movePanCanFilesToFinalDestinationJob.executionService.metaClass.executeCommand = { Realm realm, String command ->
            assert false: FAIL_MESSAGE
        }

        TestCase.shouldFailWithMessageContaining(AssertionError, FAIL_MESSAGE) {
            movePanCanFilesToFinalDestinationJob.cleanupOldResults(roddyBamFile2, realm)
        }
    }



    @Test
    void testExecute_AllFine() {
        setUp_allFine()
        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(realm, roddyBamFile)

        movePanCanFilesToFinalDestinationJob.execute()
        assert roddyBamFile.fileOperationStatus == AbstractMergedBamFile.FileOperationStatus.PROCESSED
        assert roddyBamFile.md5sum == DomainFactory.DEFAULT_MD5_SUM
        assert roddyBamFile.fileSize > 0
        assert roddyBamFile.fileExists
        assert roddyBamFile.dateFromFileSystem != null  && roddyBamFile.dateFromFileSystem instanceof Date
    }

    @Test
    void testExecute_RoddyBamFileIsNotLatestBamFile_ShouldFail() {
        setUp_allFine()
        roddyBamFile.metaClass.isMostRecentBamFile = { -> false}

        assert shouldFail (AssertionError) {
            movePanCanFilesToFinalDestinationJob.execute()
        } ==~ /.*The BamFile .* is not the most recent one.*/
    }

    @Test
    void testExecute_RoddyBamFileHasWrongState_ShouldFail() {
        setUp_allFine()
        roddyBamFile.fileOperationStatus = AbstractMergedBamFile.FileOperationStatus.DECLARED
        roddyBamFile.save(flush: true, failOnError: true)

        assert shouldFail (AssertionError) {
            movePanCanFilesToFinalDestinationJob.execute()
        }.contains('assert [FileOperationStatus.NEEDS_PROCESSING, FileOperationStatus.INPROGRESS].contains(roddyBamFile.fileOperationStatus)')
    }

    @Test
    void testExecute_RoddyBamFileHasSecondCandidate_ShouldFail() {
        setUp_allFine()
        DomainFactory.createRoddyBamFile([
                workPackage: roddyBamFile.workPackage,
                withdrawn: false,
                fileOperationStatus: AbstractMergedBamFile.FileOperationStatus.INPROGRESS,
                md5sum: null,
                fileSize: -1,
                identifier: roddyBamFile.identifier - 1,
        ])

        assert shouldFail (AssertionError) {
            movePanCanFilesToFinalDestinationJob.execute()
        }.contains('Collection contains 2 elements. Expected 1.')
    }

    @Test
    void testExecute_FailInCleanupWorkDirectory_ShouldFail() {
        setUp_allFine()
        final String FAIL_MESSAGE = HelperUtils.uniqueString
        movePanCanFilesToFinalDestinationJob.metaClass.cleanupWorkDirectory = { RoddyBamFile roddyBamFile, Realm realm -> assert false: FAIL_MESSAGE }

        TestCase.shouldFailWithMessageContaining (AssertionError, FAIL_MESSAGE) {
            movePanCanFilesToFinalDestinationJob.execute()
        }
    }

    @Test
    void testExecute_FailInSetPermission_ShouldFail() {
        setUp_allFine()
        final String FAIL_MESSAGE = HelperUtils.uniqueString
        movePanCanFilesToFinalDestinationJob.executionHelperService.metaClass.setPermission = {Realm realm, File directory, String group -> assert false: FAIL_MESSAGE  }

        TestCase.shouldFailWithMessageContaining (AssertionError, FAIL_MESSAGE) {
            movePanCanFilesToFinalDestinationJob.execute()
        }
    }
    @Test
    void testExecute_FailInGetGroup_ShouldFail() {
        setUp_allFine()
        final String FAIL_MESSAGE = HelperUtils.uniqueString
        movePanCanFilesToFinalDestinationJob.executionHelperService.metaClass.getGroup = {File directory -> assert false: FAIL_MESSAGE  }

        TestCase.shouldFailWithMessageContaining (AssertionError, FAIL_MESSAGE) {
            movePanCanFilesToFinalDestinationJob.execute()
        }
    }

    @Test
    void testExecute_FailInSetGroup_ShouldFail() {
        setUp_allFine()
        final String FAIL_MESSAGE = HelperUtils.uniqueString
        movePanCanFilesToFinalDestinationJob.executionHelperService.metaClass.setGroup = {Realm realm, File directory, String group -> assert false: FAIL_MESSAGE }

        TestCase.shouldFailWithMessageContaining (AssertionError, FAIL_MESSAGE) {
            movePanCanFilesToFinalDestinationJob.execute()
        }
    }

    @Test
    void testExecute_FailInCorrectGroups_ShouldFail() {
        setUp_allFine()
        final String FAIL_MESSAGE = HelperUtils.uniqueString
        movePanCanFilesToFinalDestinationJob.executeRoddyCommandService.metaClass.correctGroups = { RoddyBamFile roddyBamFile -> assert false: FAIL_MESSAGE }

        TestCase.shouldFailWithMessageContaining (AssertionError, FAIL_MESSAGE) {
            movePanCanFilesToFinalDestinationJob.execute()
        }
    }

    @Test
    void testExecute_FailLinkNewResults_ShouldFail() {
        setUp_allFine()
        final String FAIL_MESSAGE = HelperUtils.uniqueString
        movePanCanFilesToFinalDestinationJob.metaClass.linkNewResults = { RoddyBamFile roddyBamFile, Realm realm -> assert false: FAIL_MESSAGE }

        TestCase.shouldFailWithMessageContaining (AssertionError, FAIL_MESSAGE) {
            movePanCanFilesToFinalDestinationJob.execute()
        }
    }

    @Test
    void testExecute_FailInCleanupOldResults_ShouldFail() {
        setUp_allFine()
        final String FAIL_MESSAGE = HelperUtils.uniqueString
        movePanCanFilesToFinalDestinationJob.metaClass.cleanupOldResults = { RoddyBamFile roddyBamFile, Realm realm -> assert false: FAIL_MESSAGE }

        TestCase.shouldFailWithMessageContaining (AssertionError, FAIL_MESSAGE) {
            movePanCanFilesToFinalDestinationJob.execute()
        }
    }

    @Test
    void testExecute_Md5sumFileDoesNotExist_ShouldFail() {
        setUp_allFine()

        assert shouldFail (AssertionError) {
            movePanCanFilesToFinalDestinationJob.execute()
        } ==~ /The md5sum file of .* does not exist.*/
    }

    @Test
    void testExecute_Md5sumIsNotCorrect_ShouldFail() {
        setUp_allFine()
        String md5sum = "0123--6789ab##ef0123456789abcdef" // arbitrary wrong md5sum
        assert roddyBamFile.workDirectory.mkdirs()
        roddyBamFile.workMd5sumFile.text = md5sum

        assert shouldFail(ValidationException) {
            movePanCanFilesToFinalDestinationJob.execute()
        }.contains(md5sum)
    }

    @Test
    void testExecute_Md5sumFileIsEmpty_ShouldFail() {
        setUp_allFine()
        assert roddyBamFile.workDirectory.mkdirs()
        roddyBamFile.workMd5sumFile.setText("")

        assert shouldFail(AssertionError) {
            movePanCanFilesToFinalDestinationJob.execute()
        } ==~ /.*The md5sum file of .* is empty.*/
    }

    @Test
    void testExecute_RoddyBamFileIsWithdrawn_ShouldNotBeCopied() {
        setUp_allFine()
        roddyBamFile.withdrawn = true
        assert roddyBamFile.save(flush: true)

        movePanCanFilesToFinalDestinationJob.metaClass.cleanupWorkDirectory = { RoddyBamFile roddyBamFile, Realm realm ->
            throw new Exception("Should not reach this method")
        }
        movePanCanFilesToFinalDestinationJob.executeRoddyCommandService.metaClass.correctGroups = { RoddyBamFile roddyBamFile, Realm realm ->
            throw new Exception("Should not reach this method")
        }
        movePanCanFilesToFinalDestinationJob.metaClass.deleteOldLinks = { RoddyBamFile roddyBamFile, Realm realm ->
            throw new Exception("Should not reach this method")
        }
        movePanCanFilesToFinalDestinationJob.metaClass.linkNewResults = { RoddyBamFile roddyBamFile, Realm realm ->
            throw new Exception("Should not reach this method")
        }
        movePanCanFilesToFinalDestinationJob.metaClass.cleanupOldResults = { RoddyBamFile roddyBamFile, Realm realm ->
            throw new Exception("Should not reach this method")
        }
        movePanCanFilesToFinalDestinationJob.log = new NoOpLog()

        movePanCanFilesToFinalDestinationJob.execute()
        assert roddyBamFile.fileOperationStatus == AbstractMergedBamFile.FileOperationStatus.NEEDS_PROCESSING
    }



    private void finishOperationStateOfRoddyBamFile(RoddyBamFile roddyBamFile) {
        roddyBamFile.md5sum = HelperUtils.randomMd5sum
        roddyBamFile.fileOperationStatus = AbstractMergedBamFile.FileOperationStatus.PROCESSED
        roddyBamFile.fileSize = 1000
        assert roddyBamFile.save(flush: true)
    }

    private RoddyBamFile createBamFileSetupAndReturnBamFileToWorkOn() {
        finishOperationStateOfRoddyBamFile(roddyBamFile)
        RoddyBamFile roddyBamFile2 = DomainFactory.createRoddyBamFile(roddyBamFile)

        assert roddyBamFile.workDirectory.mkdirs()
        roddyBamFile2.workMd5sumFile << "${HelperUtils.randomMd5sum}\n"
        roddyBamFile2.workBamFile << "some content"
        roddyBamFile2.workBaiFile << "some content"
        return roddyBamFile2
    }

}
