package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*
import grails.validation.*
import org.apache.commons.logging.impl.*
import org.junit.*
import org.junit.rules.*
import org.springframework.beans.factory.annotation.*


class LinkFilesToFinalDestinationServiceTests {

    @Autowired
    LinkFilesToFinalDestinationService linkFilesToFinalDestinationService

    RoddyBamFile roddyBamFile
    Realm realm


    final static String SOME_GROUP = "GROUP"

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder()

    @Before
    void setUp() {
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
    }

    @After
    void tearDown() {
        TestCase.removeMetaClass(LinkFilesToFinalDestinationService, linkFilesToFinalDestinationService)
        TestCase.removeMetaClass(ExecutionService, linkFilesToFinalDestinationService.executionService)
        TestCase.removeMetaClass(ExecutionHelperService, linkFilesToFinalDestinationService.executionHelperService)
        TestCase.removeMetaClass(ExecuteRoddyCommandService, linkFilesToFinalDestinationService.executeRoddyCommandService)
        TestCase.removeMetaClass(CreateClusterScriptService, linkFilesToFinalDestinationService.createClusterScriptService)
        TestCase.removeMetaClass(LinkFileUtils, linkFilesToFinalDestinationService.linkFileUtils)
        GroovySystem.metaClassRegistry.removeMetaClass(ProcessHelperService)
    }

    void setUp_allFine() {
        linkFilesToFinalDestinationService.metaClass.getProcessParameterObject = { -> roddyBamFile }
        linkFilesToFinalDestinationService.metaClass.cleanupWorkDirectory = { RoddyBamFile roddyBamFile, Realm realm -> }
        linkFilesToFinalDestinationService.metaClass.deleteOldLinks = { RoddyBamFile roddyBamFile, Realm realm -> }
        linkFilesToFinalDestinationService.metaClass.linkNewResults = { RoddyBamFile roddyBamFile, Realm realm -> }
        linkFilesToFinalDestinationService.metaClass.cleanupOldResults = { RoddyBamFile roddyBamFile, Realm realm -> }
        linkFilesToFinalDestinationService.executeRoddyCommandService.metaClass.correctGroups = { RoddyBamFile roddyBamFile, Realm realm -> }
        linkFilesToFinalDestinationService.executionHelperService.metaClass.setPermission = { Realm realm, File directory, String group -> }
        linkFilesToFinalDestinationService.executionHelperService.metaClass.getGroup = { File directory -> SOME_GROUP }
        linkFilesToFinalDestinationService.executionHelperService.metaClass.setGroup = { Realm realm, File directory, String group -> }
    }

    @Test
    void testCleanupWorkDirectory_allFine_withTmpFilesAndTmpDirs() {
        helper_testCleanupWorkDirectory_allFine(1, 1)
    }

    @Test
    void testCleanupWorkDirectory_allFine_withTmpFilesAndTmpDirs_WGBS() {
        helper_testCleanupWorkDirectory_allFine(1, 1, true)
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
        helper_testCleanupWorkDirectory_allFine(2, 3)
    }

    @Test
    void testCleanupWorkDirectory_bamFileIsNull_shouldFail() {
        TestCase.shouldFailWithMessageContaining(AssertionError, "roddyBamFile") {
            linkFilesToFinalDestinationService.cleanupWorkDirectory(null, realm)
        }
    }

    @Test
    void testCleanupWorkDirectory_realmIsNull_shouldFail() {
        TestCase.shouldFailWithMessageContaining(AssertionError, "realm") {
            linkFilesToFinalDestinationService.cleanupWorkDirectory(roddyBamFile, null)
        }
    }


    @Test
    void testCleanupWorkDirectory_bamHasOldStructure_shouldFail() {
        roddyBamFile.workDirectoryName = null
        roddyBamFile.save(flush: true, failOnError: true)

        TestCase.shouldFailWithMessageContaining(AssertionError, "isOldStructureUsed") {
            linkFilesToFinalDestinationService.cleanupWorkDirectory(roddyBamFile, realm)
        }
    }

    @Test
    void testCleanupWorkDirectory_deleteContentOfOtherUnixUserDirectoryThrowsException_shouldFail() {
        final String FAIL_MESSAGE = HelperUtils.uniqueString

        linkFilesToFinalDestinationService.executeRoddyCommandService.metaClass.deleteContentOfOtherUnixUserDirectory = { File basePath, Realm realm ->
            assert false: FAIL_MESSAGE
        }
        File file = new File(roddyBamFile.workDirectory, HelperUtils.uniqueString)
        assert file.mkdirs()

        TestCase.shouldFailWithMessageContaining(AssertionError, FAIL_MESSAGE) {
            linkFilesToFinalDestinationService.cleanupWorkDirectory(roddyBamFile, realm)
        }
    }

    @Test
    void testCleanupWorkDirectory_removeDirsThrowsException_shouldFail() {
        assert roddyBamFile.workDirectory.mkdirs()
        assert new File(roddyBamFile.workDirectory, HelperUtils.uniqueString).createNewFile()
        final String FAIL_MESSAGE = HelperUtils.uniqueString

        linkFilesToFinalDestinationService.createClusterScriptService.metaClass.removeDirs = { Collection<File> dirs, CreateClusterScriptService.RemoveOption option ->
            assert false: FAIL_MESSAGE
        }

        TestCase.shouldFailWithMessageContaining(AssertionError, FAIL_MESSAGE) {
            linkFilesToFinalDestinationService.cleanupWorkDirectory(roddyBamFile, realm)
        }
    }

    @Test
    void testCleanupWorkDirectory_executeCommandThrowsException_shouldFail() {
        assert roddyBamFile.workDirectory.mkdirs()
        assert new File(roddyBamFile.workDirectory, HelperUtils.uniqueString).createNewFile()
        final String FAIL_MESSAGE = HelperUtils.uniqueString

        linkFilesToFinalDestinationService.executionService.metaClass.executeCommand = { Realm realm, String command ->
            assert false: FAIL_MESSAGE
        }

        TestCase.shouldFailWithMessageContaining(AssertionError, FAIL_MESSAGE) {
            linkFilesToFinalDestinationService.cleanupWorkDirectory(roddyBamFile, realm)
        }
    }

    private void helper_testCleanupWorkDirectory_allFine(int countTmpFiles, int countTmpDir, boolean wgbs = false) {
        if (wgbs) {
            SeqType seqType = roddyBamFile.mergingWorkPackage.seqType
            seqType.name = SeqTypeNames.WHOLE_GENOME_BISULFITE.seqTypeName
            seqType.save(flush: true, failOnError: true)
        }

        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(roddyBamFile)
        int callDeletedRoddy = 0
        boolean callDeleted = false
        List<File> filesNotToBeCalledFor = [
                roddyBamFile.workBamFile,
                roddyBamFile.workBaiFile,
                roddyBamFile.workMd5sumFile,
                roddyBamFile.workQADirectory,
                roddyBamFile.workExecutionStoreDirectory,
        ]

        if (wgbs) {
            filesNotToBeCalledFor += [roddyBamFile.workMethylationDirectory,
                                      roddyBamFile.workMetadataTableFile,
            ]
        }

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

        linkFilesToFinalDestinationService.executeRoddyCommandService.metaClass.deleteContentOfOtherUnixUserDirectory = { File file, Realm realm ->
            assert roddyBamFile.workDirectory == file.parentFile
            assert !filesNotToBeCalledFor.contains(file)
            assert tmpDirectories.contains(file)
            callDeletedRoddy++
        }

        linkFilesToFinalDestinationService.executionService.metaClass.executeCommand = { Realm realm, String command ->
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
        assert (filesNotToBeCalledFor + tmpFiles + tmpDirectories) as Set == roddyBamFile.workDirectory.listFiles() as Set

        linkFilesToFinalDestinationService.cleanupWorkDirectory(roddyBamFile, realm)

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
        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(roddyBamFile)

        List<File> linkedFiles = createLinkedFilesList()

        linkFilesToFinalDestinationService.executionService.metaClass.executeCommand = { Realm realm, String command ->
            return ProcessHelperService.executeAndAssertExitCodeAndErrorOutAndReturnStdout(command)
        }

        linkedFiles.each {
            assert !it.exists()
        }

        linkFilesToFinalDestinationService.linkNewResults(roddyBamFile, realm)

        linkedFiles.each {
            assert it.exists()
        }
    }

    @Test
    void testLinkNewResults_methylation_OneLibrary_AllFine() {
        testLinkNewResults_methylation_setup()

        List<File> linkedFiles = createLinkedFilesList()
        linkedFiles.addAll(roddyBamFile.finalMergedMethylationDirectory)
        linkedFiles.addAll(roddyBamFile.finalMetadataTableFile)

        testLinkNewResults_helper(linkedFiles)
    }

    @Test
    void testLinkNewResults_methylation_TwoLibraries_AllFine() {
        MergingWorkPackage workPackage = roddyBamFile.mergingWorkPackage

        SeqTrack seqTrack = DomainFactory.createSeqTrackWithDataFiles(workPackage, [libraryName: 'library14', normalizedLibraryName: SeqTrack.normalizeLibraryName('library14')])
        assert seqTrack.save(flush: true)

        roddyBamFile.seqTracks.add(seqTrack)
        roddyBamFile.numberOfMergedLanes = 2
        assert roddyBamFile.save(flush: true)

        testLinkNewResults_methylation_setup()

        List<File> linkedFiles = createLinkedFilesList()
        linkedFiles.addAll(roddyBamFile.finalMergedMethylationDirectory)
        linkedFiles.addAll(roddyBamFile.finalMetadataTableFile)
        linkedFiles.addAll(roddyBamFile.finalLibraryMethylationDirectories.values())
        linkedFiles.addAll(roddyBamFile.finalLibraryQADirectories.values())

        testLinkNewResults_helper(linkedFiles)
    }

    @Test
    void testLinkNewResults_bamFileIsNull_shouldFail() {
        TestCase.shouldFailWithMessageContaining(AssertionError, "roddyBamFile") {
            linkFilesToFinalDestinationService.linkNewResults(null, realm)
        }
    }

    @Test
    void testLinkNewResults_realmIsNull_shouldFail() {
        TestCase.shouldFailWithMessageContaining(AssertionError, "realm") {
            linkFilesToFinalDestinationService.linkNewResults(roddyBamFile, null)
        }
    }

    @Test
    void testLinkNewResults_bamHasOldStructure_shouldFail() {
        roddyBamFile.workDirectoryName = null
        roddyBamFile.save(flush: true, failOnError: true)

        TestCase.shouldFailWithMessageContaining(AssertionError, "isOldStructureUsed") {
            linkFilesToFinalDestinationService.linkNewResults(roddyBamFile, realm)
        }
    }

    @Test
    void testLinkNewResults_deleteContentOfOtherUnixUserDirectoryThrowsException_shouldFail() {
        final String FAIL_MESSAGE = HelperUtils.uniqueString

        linkFilesToFinalDestinationService.linkFileUtils.metaClass.createAndValidateLinks = { Map<File, File> sourceLinkMap, Realm realm ->
            assert false: FAIL_MESSAGE
        }

        TestCase.shouldFailWithMessageContaining(AssertionError, FAIL_MESSAGE) {
            linkFilesToFinalDestinationService.linkNewResults(roddyBamFile, realm)
        }
    }

    private void testLinkNewResults_methylation_setup(){
        SeqType seqType = roddyBamFile.mergingWorkPackage.seqType
        seqType.name = SeqTypeNames.WHOLE_GENOME_BISULFITE.seqTypeName
        seqType.save(flush: true, failOnError: true)

        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(roddyBamFile)
    }

    private void testLinkNewResults_helper(List<File> linkedFiles){
        linkFilesToFinalDestinationService.executionService.metaClass.executeCommand = { Realm realm, String command ->
            return ProcessHelperService.executeAndAssertExitCodeAndErrorOutAndReturnStdout(command)
        }

        linkedFiles.each {
            assert !it.exists()
        }

        linkFilesToFinalDestinationService.linkNewResults(roddyBamFile, realm)

        linkedFiles.each {
            assert it.exists()
        }
    }

    private List<File> createLinkedFilesList(){
        return [
                roddyBamFile.finalBamFile,
                roddyBamFile.finalBaiFile,
                roddyBamFile.finalMd5sumFile,
                roddyBamFile.finalMergedQADirectory,
                roddyBamFile.finalMergedQAJsonFile,
                roddyBamFile.getFinalExecutionDirectories(),
                roddyBamFile.finalSingleLaneQADirectories.values(),
                roddyBamFile.finalSingleLaneQAJsonFiles.values(),
        ].flatten()
    }

    @Test
    void testCleanupOldResults_withBaseBamFile_allFine() {
        finishOperationStateOfRoddyBamFile(roddyBamFile)
        RoddyBamFile roddyBamFile2 = DomainFactory.createRoddyBamFile(roddyBamFile)
        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(roddyBamFile)
        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(roddyBamFile2)

        linkFilesToFinalDestinationService.executionService.metaClass.executeCommand = { Realm realm, String command ->
            return ProcessHelperService.executeAndAssertExitCodeAndErrorOutAndReturnStdout(command)
        }

        List<File> filesToDelete = [
                roddyBamFile.workBamFile,
                roddyBamFile.workBaiFile,
        ]
        List<File> filesToKeep = [
                roddyBamFile.workMd5sumFile,
                roddyBamFile.workExecutionDirectories,
                roddyBamFile.workMergedQADirectory,
                roddyBamFile.workSingleLaneQADirectories.values(),
        ].flatten()
        [filesToKeep, filesToDelete].flatten().each {
            assert it.exists()
        }

        linkFilesToFinalDestinationService.cleanupOldResults(roddyBamFile2, realm)

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
        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(roddyBamFile2)

        linkFilesToFinalDestinationService.executionService.metaClass.executeCommand = { Realm realm, String command ->
            assert false: 'should not be called'
        }

        linkFilesToFinalDestinationService.executeRoddyCommandService.metaClass.deleteContentOfOtherUnixUserDirectory = { File basePath, Realm realm ->
            assert false: 'should not be called'
        }

        linkFilesToFinalDestinationService.cleanupOldResults(roddyBamFile2, realm)
    }

    @Test
    void testCleanupOldResults_withoutBaseBamFile_butBamFilesOfWorkPackageExist_allFine() {
        DomainFactory.createRoddyBamFile(workPackage: roddyBamFile.workPackage, config: roddyBamFile.config)
        RoddyBamFile roddyBamFile2 = DomainFactory.createRoddyBamFile(workPackage: roddyBamFile.workPackage, config: roddyBamFile.config)
        CreateRoddyFileHelper.createRoddyAlignmentFinalResultFiles(roddyBamFile)
        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(roddyBamFile)
        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(roddyBamFile2)
        boolean hasCalled_deleteContentOfOtherUnixUserDirectory = false
        assert roddyBamFile.workDirectory.exists()

        linkFilesToFinalDestinationService.executionService.metaClass.executeCommand = { Realm realm, String command ->
            return ProcessHelperService.executeAndAssertExitCodeAndErrorOutAndReturnStdout(command)
        }

        linkFilesToFinalDestinationService.executeRoddyCommandService.metaClass.deleteContentOfOtherUnixUserDirectory = { File basePath, Realm realm ->
            hasCalled_deleteContentOfOtherUnixUserDirectory = true
            assert basePath.exists()
            assert basePath.isDirectory()
            assert basePath.deleteDir()
        }

        linkFilesToFinalDestinationService.cleanupOldResults(roddyBamFile2, realm)

        assert hasCalled_deleteContentOfOtherUnixUserDirectory
        assert !roddyBamFile.workDirectory.exists()
        assert roddyBamFile2.workDirectory.exists()
        assert !roddyBamFile.finalExecutionStoreDirectory.exists()
        assert !roddyBamFile.finalQADirectory.exists()
    }

    @Test
    void testCleanupOldResults_withoutBaseBamFile_butBamFilesOfWorkPackageExistInOldStructure_latestIsOld_allFine() {
        DomainFactory.createRoddyBamFile(workPackage:  roddyBamFile.workPackage, workDirectoryName: null, config: roddyBamFile.config)
        helper_testCleanupOldResults_withoutBaseBamFile_butBamFilesOfWorkPackageExistInOldStructure(true)
    }

    @Test
    void testCleanupOldResults_withoutBaseBamFile_butBamFilesOfWorkPackageExistInOldAndNewStructure_latestIsNew_allFine() {
        DomainFactory.createRoddyBamFile(workPackage:  roddyBamFile.workPackage, config: roddyBamFile.config)
        helper_testCleanupOldResults_withoutBaseBamFile_butBamFilesOfWorkPackageExistInOldStructure(false)
    }

    void helper_testCleanupOldResults_withoutBaseBamFile_butBamFilesOfWorkPackageExistInOldStructure(boolean latestIsOld) {
        roddyBamFile.workDirectoryName = null
        roddyBamFile.save(flush: true, failOnError: true)
        RoddyBamFile roddyBamFile2 = DomainFactory.createRoddyBamFile(workPackage:  roddyBamFile.workPackage, config: roddyBamFile.config)
        CreateRoddyFileHelper.createRoddyAlignmentFinalResultFiles(roddyBamFile)
        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(roddyBamFile2)
        assert roddyBamFile2.workDirectory.exists()
        boolean isCalledForMergedQaDirectory = false

        linkFilesToFinalDestinationService.executionService.metaClass.executeCommand = { Realm realm, String command ->
            assert latestIsOld == !command.contains(RoddyBamFile.WORK_DIR_PREFIX)
            return ProcessHelperService.executeAndAssertExitCodeAndErrorOutAndReturnStdout(command)
        }

        linkFilesToFinalDestinationService.executeRoddyCommandService.metaClass.deleteContentOfOtherUnixUserDirectory = { File basePath, Realm realm ->
            assert !basePath.absolutePath.contains(RoddyBamFile.WORK_DIR_PREFIX)
            assert basePath.deleteDir()
            if (basePath.path.contains("${RoddyBamFile.QUALITY_CONTROL_DIR}/${RoddyBamFile.MERGED_DIR}")) {
                isCalledForMergedQaDirectory = true
            }
        }

        linkFilesToFinalDestinationService.cleanupOldResults(roddyBamFile2, realm)
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
        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(roddyBamFile)

        linkFilesToFinalDestinationService.executionService.metaClass.executeCommand = { Realm realm, String command ->
            assert false: 'should not be called'
        }

        linkFilesToFinalDestinationService.executeRoddyCommandService.metaClass.deleteContentOfOtherUnixUserDirectory = { File basePath, Realm realm ->
            assert false: 'should not be called'
        }

        linkFilesToFinalDestinationService.cleanupOldResults(roddyBamFile, realm)

        assert roddyBamFile.workDirectory.exists()
    }

    @Test
    void testCleanupOldResults_bamFileIsNull_shouldFail() {
        TestCase.shouldFailWithMessageContaining(AssertionError, "roddyBamFile") {
            linkFilesToFinalDestinationService.cleanupOldResults(null, realm)
        }
    }

    @Test
    void testCleanupOldResults_realmIsNull_shouldFail() {
        TestCase.shouldFailWithMessageContaining(AssertionError, "realm") {
            linkFilesToFinalDestinationService.cleanupOldResults(roddyBamFile, null)
        }
    }

    @Test
    void testCleanupOldResults_bamHasOldStructure_shouldFail() {
        roddyBamFile.workDirectoryName = null
        roddyBamFile.save(flush: true, failOnError: true)

        TestCase.shouldFailWithMessageContaining(AssertionError, "isOldStructureUsed") {
            linkFilesToFinalDestinationService.cleanupOldResults(roddyBamFile, realm)
        }
    }

    @Test
    void testCleanupOldResults_removeDirsThrowsException_shouldFail() {
        final String FAIL_MESSAGE = HelperUtils.uniqueString
        finishOperationStateOfRoddyBamFile(roddyBamFile)
        RoddyBamFile roddyBamFile2 = DomainFactory.createRoddyBamFile(roddyBamFile)
        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(roddyBamFile)
        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(roddyBamFile2)

        linkFilesToFinalDestinationService.executeRoddyCommandService.metaClass.deleteContentOfOtherUnixUserDirectory = { File basePath, Realm realm -> }

        linkFilesToFinalDestinationService.createClusterScriptService.metaClass.removeDirs = { Collection<File> dirs, CreateClusterScriptService.RemoveOption option ->
            assert false: FAIL_MESSAGE
        }

        TestCase.shouldFailWithMessageContaining(AssertionError, FAIL_MESSAGE) {
            linkFilesToFinalDestinationService.cleanupOldResults(roddyBamFile2, realm)
        }
    }

    @Test
    void testCleanupOldResults_executeCommandThrowsException_shouldFail() {
        final String FAIL_MESSAGE = HelperUtils.uniqueString
        finishOperationStateOfRoddyBamFile(roddyBamFile)
        RoddyBamFile roddyBamFile2 = DomainFactory.createRoddyBamFile(roddyBamFile)
        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(roddyBamFile)
        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(roddyBamFile2)

        linkFilesToFinalDestinationService.executeRoddyCommandService.metaClass.deleteContentOfOtherUnixUserDirectory = { File basePath, Realm realm -> }

        linkFilesToFinalDestinationService.executionService.metaClass.executeCommand = { Realm realm, String command ->
            assert false: FAIL_MESSAGE
        }

        TestCase.shouldFailWithMessageContaining(AssertionError, FAIL_MESSAGE) {
            linkFilesToFinalDestinationService.cleanupOldResults(roddyBamFile2, realm)
        }
    }



    @Test
    void testExecute_AllFine() {
        setUp_allFine()
        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(roddyBamFile)

        linkFilesToFinalDestinationService.linkToFinalDestinationAndCleanup(roddyBamFile, realm)
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
            linkFilesToFinalDestinationService.prepareRoddyBamFile(roddyBamFile)
        } ==~ /.*The BamFile .* is not the most recent one.*/
    }

    @Test
    void testExecute_RoddyBamFileHasWrongState_ShouldFail() {
        setUp_allFine()
        roddyBamFile.fileOperationStatus = AbstractMergedBamFile.FileOperationStatus.DECLARED
        roddyBamFile.save(flush: true, failOnError: true)

        assert shouldFail (AssertionError) {
            linkFilesToFinalDestinationService.prepareRoddyBamFile(roddyBamFile)
        }.contains('assert [NEEDS_PROCESSING, INPROGRESS].contains(roddyBamFile.fileOperationStatus)')
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
                config: roddyBamFile.config,
        ])

        assert shouldFail (AssertionError) {
            linkFilesToFinalDestinationService.prepareRoddyBamFile(roddyBamFile)
        }.contains('Collection contains 2 elements. Expected 1.')
    }

    @Test
    void testExecute_FailInCleanupWorkDirectory_ShouldFail() {
        setUp_allFine()
        final String FAIL_MESSAGE = HelperUtils.uniqueString
        linkFilesToFinalDestinationService.metaClass.cleanupWorkDirectory = { RoddyBamFile roddyBamFile, Realm realm -> assert false: FAIL_MESSAGE }

        TestCase.shouldFailWithMessageContaining (AssertionError, FAIL_MESSAGE) {
            linkFilesToFinalDestinationService.linkToFinalDestinationAndCleanup(roddyBamFile, realm)
        }
    }

    @Test
    void testExecute_FailInSetPermission_ShouldFail() {
        setUp_allFine()
        final String FAIL_MESSAGE = HelperUtils.uniqueString
        linkFilesToFinalDestinationService.executionHelperService.metaClass.setPermission = { Realm realm, File directory, String group -> assert false: FAIL_MESSAGE  }

        TestCase.shouldFailWithMessageContaining (AssertionError, FAIL_MESSAGE) {
            linkFilesToFinalDestinationService.linkToFinalDestinationAndCleanup(roddyBamFile, realm)
        }
    }
    @Test
    void testExecute_FailInGetGroup_ShouldFail() {
        setUp_allFine()
        final String FAIL_MESSAGE = HelperUtils.uniqueString
        linkFilesToFinalDestinationService.executionHelperService.metaClass.getGroup = { File directory -> assert false: FAIL_MESSAGE  }

        TestCase.shouldFailWithMessageContaining (AssertionError, FAIL_MESSAGE) {
            linkFilesToFinalDestinationService.linkToFinalDestinationAndCleanup(roddyBamFile, realm)
        }
    }

    @Test
    void testExecute_FailInSetGroup_ShouldFail() {
        setUp_allFine()
        final String FAIL_MESSAGE = HelperUtils.uniqueString
        linkFilesToFinalDestinationService.executionHelperService.metaClass.setGroup = { Realm realm, File directory, String group -> assert false: FAIL_MESSAGE }

        TestCase.shouldFailWithMessageContaining (AssertionError, FAIL_MESSAGE) {
            linkFilesToFinalDestinationService.linkToFinalDestinationAndCleanup(roddyBamFile, realm)
        }
    }

    @Test
    void testExecute_FailInCorrectGroups_ShouldFail() {
        setUp_allFine()
        final String FAIL_MESSAGE = HelperUtils.uniqueString
        linkFilesToFinalDestinationService.executeRoddyCommandService.metaClass.correctGroups = { RoddyBamFile roddyBamFile, Realm realm -> assert false: FAIL_MESSAGE }

        TestCase.shouldFailWithMessageContaining (AssertionError, FAIL_MESSAGE) {
            linkFilesToFinalDestinationService.linkToFinalDestinationAndCleanup(roddyBamFile, realm)
        }
    }

    @Test
    void testExecute_FailLinkNewResults_ShouldFail() {
        setUp_allFine()
        final String FAIL_MESSAGE = HelperUtils.uniqueString
        linkFilesToFinalDestinationService.metaClass.linkNewResults = { RoddyBamFile roddyBamFile, Realm realm -> assert false: FAIL_MESSAGE }

        TestCase.shouldFailWithMessageContaining (AssertionError, FAIL_MESSAGE) {
            linkFilesToFinalDestinationService.linkToFinalDestinationAndCleanup(roddyBamFile, realm)
        }
    }

    @Test
    void testExecute_FailInCleanupOldResults_ShouldFail() {
        setUp_allFine()
        final String FAIL_MESSAGE = HelperUtils.uniqueString
        linkFilesToFinalDestinationService.metaClass.cleanupOldResults = { RoddyBamFile roddyBamFile, Realm realm -> assert false: FAIL_MESSAGE }

        TestCase.shouldFailWithMessageContaining (AssertionError, FAIL_MESSAGE) {
            linkFilesToFinalDestinationService.linkToFinalDestinationAndCleanup(roddyBamFile, realm)
        }
    }

    @Test
    void testExecute_Md5sumFileDoesNotExist_ShouldFail() {
        setUp_allFine()

        assert shouldFail (AssertionError) {
            linkFilesToFinalDestinationService.linkToFinalDestinationAndCleanup(roddyBamFile, realm)
        } ==~ /The md5sum file of .* does not exist.*/
    }

    @Test
    void testExecute_Md5sumIsNotCorrect_ShouldFail() {
        setUp_allFine()
        String md5sum = "0123--6789ab##ef0123456789abcdef" // arbitrary wrong md5sum
        assert roddyBamFile.workDirectory.mkdirs()
        roddyBamFile.workMd5sumFile.text = md5sum

        assert shouldFail(ValidationException) {
            linkFilesToFinalDestinationService.linkToFinalDestinationAndCleanup(roddyBamFile, realm)
        }.contains(md5sum)
    }

    @Test
    void testExecute_Md5sumFileIsEmpty_ShouldFail() {
        setUp_allFine()
        assert roddyBamFile.workDirectory.mkdirs()
        roddyBamFile.workMd5sumFile.setText("")

        assert shouldFail(AssertionError) {
            linkFilesToFinalDestinationService.linkToFinalDestinationAndCleanup(roddyBamFile, realm)
        } ==~ /.*The md5sum file of .* is empty.*/
    }

    @Test
    void testExecute_RoddyBamFileIsWithdrawn_ShouldNotBeCopied() {
        setUp_allFine()
        roddyBamFile.withdrawn = true
        assert roddyBamFile.save(flush: true)

        linkFilesToFinalDestinationService.metaClass.cleanupWorkDirectory = { RoddyBamFile roddyBamFile, Realm realm ->
            throw new Exception("Should not reach this method")
        }
        linkFilesToFinalDestinationService.executeRoddyCommandService.metaClass.correctGroups = { RoddyBamFile roddyBamFile, Realm realm ->
            throw new Exception("Should not reach this method")
        }
        linkFilesToFinalDestinationService.metaClass.deleteOldLinks = { RoddyBamFile roddyBamFile, Realm realm ->
            throw new Exception("Should not reach this method")
        }
        linkFilesToFinalDestinationService.metaClass.linkNewResults = { RoddyBamFile roddyBamFile, Realm realm ->
            throw new Exception("Should not reach this method")
        }
        linkFilesToFinalDestinationService.metaClass.cleanupOldResults = { RoddyBamFile roddyBamFile, Realm realm ->
            throw new Exception("Should not reach this method")
        }
        linkFilesToFinalDestinationService.log = new NoOpLog()

        linkFilesToFinalDestinationService.linkToFinalDestinationAndCleanup(roddyBamFile, realm)
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
