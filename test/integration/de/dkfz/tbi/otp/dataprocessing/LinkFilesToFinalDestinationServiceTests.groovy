package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.*
import de.dkfz.tbi.otp.*
import de.dkfz.tbi.otp.config.OtpProperty
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.*
import de.dkfz.tbi.otp.dataprocessing.AbstractMergedBamFile.QcTrafficLightStatus
import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*
import org.apache.commons.logging.impl.*
import org.junit.*
import org.junit.rules.*
import org.springframework.beans.factory.annotation.*

class LinkFilesToFinalDestinationServiceTests implements DomainFactoryCore {

    @Autowired
    LinkFilesToFinalDestinationService linkFilesToFinalDestinationService

    RoddyBamFile roddyBamFile
    Realm realm
    TestConfigService configService

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder()

    @Before
    void setUp() {
        final int numberOfReads = DomainFactory.counter++

        roddyBamFile = DomainFactory.createRoddyBamFile()
        roddyBamFile.md5sum = null
        roddyBamFile.fileSize = -1
        roddyBamFile.fileOperationStatus = AbstractMergedBamFile.FileOperationStatus.NEEDS_PROCESSING
        roddyBamFile.roddyExecutionDirectoryNames = ["exec_123456_123456789_test_test"]
        assert roddyBamFile.save(flush: true, failOnError: true)

        realm = roddyBamFile.project.realm
        configService = new TestConfigService([(OtpProperty.PATH_PROJECT_ROOT): temporaryFolder.newFolder().path])

        SeqTrack seqTrack = roddyBamFile.seqTracks.iterator()[0]
        seqTrack.fastqcState = SeqTrack.DataProcessingState.FINISHED
        assert seqTrack.save(flush: true, failOnError: true)

        DataFile.findAllBySeqTrack(seqTrack).each {
            it.nReads = numberOfReads
            assert it.save(flush: true)
        }
        DomainFactory.createRoddyMergedBamQa(roddyBamFile, [pairedRead1: numberOfReads, pairedRead2: numberOfReads])

        DomainFactory.createRoddyProcessingOptions(temporaryFolder.newFolder())
    }

    @After
    void tearDown() {
        TestCase.removeMetaClass(LinkFilesToFinalDestinationService, linkFilesToFinalDestinationService)
        TestCase.removeMetaClass(RemoteShellHelper, linkFilesToFinalDestinationService.remoteShellHelper)
        TestCase.removeMetaClass(ExecuteRoddyCommandService, linkFilesToFinalDestinationService.executeRoddyCommandService)
        TestCase.removeMetaClass(CreateClusterScriptService, linkFilesToFinalDestinationService.lsdfFilesService.createClusterScriptService)
        TestCase.removeMetaClass(LinkFileUtils, linkFilesToFinalDestinationService.linkFileUtils)
        GroovySystem.metaClassRegistry.removeMetaClass(LocalShellHelper)
        configService.clean()
    }

    void setUp_allFine() {
        linkFilesToFinalDestinationService.metaClass.cleanupWorkDirectory = { RoddyBamFile roddyBamFile, Realm realm -> }
        linkFilesToFinalDestinationService.metaClass.linkNewResults = { RoddyBamFile roddyBamFile, Realm realm -> }
        linkFilesToFinalDestinationService.metaClass.informResultsAreBlocked = { RoddyBamFile roddyBamFile-> }
        linkFilesToFinalDestinationService.metaClass.cleanupOldResults = { RoddyBamFile roddyBamFile, Realm realm -> }
        linkFilesToFinalDestinationService.executeRoddyCommandService.metaClass.correctPermissionsAndGroups = { RoddyResult roddyResult, Realm realm -> }
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
    void testCleanupWorkDirectory_removeDirsThrowsException_shouldFail() {
        assert roddyBamFile.workDirectory.mkdirs()
        assert new File(roddyBamFile.workDirectory, HelperUtils.uniqueString).createNewFile()
        final String FAIL_MESSAGE = HelperUtils.uniqueString

        linkFilesToFinalDestinationService.lsdfFilesService.createClusterScriptService.metaClass.removeDirs = { Collection<File> dirs, CreateClusterScriptService.RemoveOption option ->
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

        linkFilesToFinalDestinationService.remoteShellHelper.metaClass.executeCommand = { Realm realm, String command ->
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

        linkFilesToFinalDestinationService.remoteShellHelper.metaClass.executeCommand = { Realm realm, String command ->
            assert filesNotToBeCalledFor.every{
                !command.contains(it.path)
            }
            assert tmpDirectories.every {
                command.contains(it.path)
            }
            assert tmpFiles.every {
                command.contains(it.path)
            }
            String stdout = LocalShellHelper.executeAndAssertExitCodeAndErrorOutAndReturnStdout(command)
            assert stdout.trim() == '0'
            callDeleted = true
            return stdout
        }
        assert (filesNotToBeCalledFor + tmpFiles + tmpDirectories) as Set == roddyBamFile.workDirectory.listFiles() as Set

        linkFilesToFinalDestinationService.cleanupWorkDirectory(roddyBamFile, realm)

        assert callDeleted  == (countTmpDir + countTmpFiles > 0)
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

        linkedFiles.each {
            assert !it.exists()
        }

        TestCase.withMockedremoteShellHelper(linkFilesToFinalDestinationService.remoteShellHelper, {
            linkFilesToFinalDestinationService.linkNewResults(roddyBamFile, realm)
        })

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

    private void testLinkNewResults_methylation_setup(){
        SeqType seqType = roddyBamFile.mergingWorkPackage.seqType
        seqType.name = SeqTypeNames.WHOLE_GENOME_BISULFITE.seqTypeName
        seqType.save(flush: true, failOnError: true)

        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(roddyBamFile)
    }

    private void testLinkNewResults_helper(List<File> linkedFiles){
        linkedFiles.each {
            assert !it.exists()
        }

        TestCase.withMockedremoteShellHelper(linkFilesToFinalDestinationService.remoteShellHelper, {
            linkFilesToFinalDestinationService.linkNewResults(roddyBamFile, realm)
        })

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

        TestCase.withMockedremoteShellHelper(linkFilesToFinalDestinationService.remoteShellHelper, {
            linkFilesToFinalDestinationService.cleanupOldResults(roddyBamFile2, realm)
        })

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

        linkFilesToFinalDestinationService.remoteShellHelper.metaClass.executeCommand = { Realm realm, String command ->
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
        assert roddyBamFile.workDirectory.exists()

        TestCase.withMockedremoteShellHelper(linkFilesToFinalDestinationService.remoteShellHelper, {
            linkFilesToFinalDestinationService.cleanupOldResults(roddyBamFile2, realm)
        })

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

        linkFilesToFinalDestinationService.remoteShellHelper.metaClass.executeCommand = { Realm realm, String command ->
            assert latestIsOld == !command.contains(RoddyBamFile.WORK_DIR_PREFIX)
            return LocalShellHelper.executeAndAssertExitCodeAndErrorOutAndReturnStdout(command)
        }

        linkFilesToFinalDestinationService.cleanupOldResults(roddyBamFile2, realm)
        assert roddyBamFile2.workDirectory.exists()
        roddyBamFile.finalExecutionDirectories.each {
            assert !it.exists()
        }
        roddyBamFile.finalSingleLaneQADirectories.values().each {
            assert !it.exists()
        }
    }

    @Test
    void testCleanupOldResults_withoutBaseBamFileAndWithoutOtherBamFilesOfTheSameWorkPackage_allFine() {
        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(roddyBamFile)

        linkFilesToFinalDestinationService.remoteShellHelper.metaClass.executeCommand = { Realm realm, String command ->
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

        linkFilesToFinalDestinationService.lsdfFilesService.createClusterScriptService.metaClass.removeDirs = { Collection<File> dirs, CreateClusterScriptService.RemoveOption option ->
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

        linkFilesToFinalDestinationService.remoteShellHelper.metaClass.executeCommand = { Realm realm, String command ->
            assert false: FAIL_MESSAGE
        }

        TestCase.shouldFailWithMessageContaining(AssertionError, FAIL_MESSAGE) {
            linkFilesToFinalDestinationService.cleanupOldResults(roddyBamFile2, realm)
        }
    }



    @Test
    void testLinkToFinalDestinationAndCleanup_AllFine() {
        setUp_allFine()
        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(roddyBamFile)

        linkFilesToFinalDestinationService.linkToFinalDestinationAndCleanup(roddyBamFile, realm)

        assertBamFileIsFine()
    }

    void assertBamFileIsFine() {
        assert roddyBamFile.fileOperationStatus == AbstractMergedBamFile.FileOperationStatus.PROCESSED
        assert roddyBamFile.md5sum == DomainFactory.DEFAULT_MD5_SUM
        assert roddyBamFile.fileSize > 0
        assert roddyBamFile.fileExists
        assert roddyBamFile.dateFromFileSystem != null  && roddyBamFile.dateFromFileSystem instanceof Date
    }

    @Test
    void testLinkToFinalDestinationAndCleanup_roddyBamFileHasMoreNumberOfReadsThanAllSeqTracksTogether_ShouldBeFine() {
        setUp_allFine()
        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(roddyBamFile)
        RoddyQualityAssessment qa = roddyBamFile.overallQualityAssessment
        qa.pairedRead1++
        assert qa.save(flush: true)
        assert roddyBamFile.numberOfReadsFromQa > roddyBamFile.numberOfReadsFromFastQc

        linkFilesToFinalDestinationService.linkToFinalDestinationAndCleanup(roddyBamFile, realm)

        assertBamFileIsFine()
    }

    @Test
    void testLinkToFinalDestinationAndCleanup_wgbsRoddyBamFileHasLessNumberOfReadsThanAllSeqTracksTogether_ShouldBeFine() {
        setUp_allFine()
        roddyBamFile.mergingWorkPackage.seqType = DomainFactory.createWholeGenomeSeqType()
        assert roddyBamFile.mergingWorkPackage.save(flush: true)
        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(roddyBamFile)
        RoddyQualityAssessment qa = roddyBamFile.overallQualityAssessment
        qa.pairedRead1--
        assert qa.save(flush: true)
        assert roddyBamFile.numberOfReadsFromQa < roddyBamFile.numberOfReadsFromFastQc

        linkFilesToFinalDestinationService.linkToFinalDestinationAndCleanup(roddyBamFile, realm)

        assertBamFileIsFine()
    }

    @Test
    void testLinkToFinalDestinationAndCleanup_QcStatusEqualPassed() {
        setUp_allFine()
        roddyBamFile.qcTrafficLightStatus = QcTrafficLightStatus.QC_PASSED
        assert roddyBamFile.save(flush: true)
        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(roddyBamFile)
        linkFilesToFinalDestinationService.metaClass.informResultsAreBlocked = { RoddyBamFile roddyBamFile ->
            assert false : "should not reach this part"
        }

        linkFilesToFinalDestinationService.linkToFinalDestinationAndCleanup(roddyBamFile, realm)

        assertBamFileIsFine()
    }

    @Test
    void testLinkToFinalDestinationAndCleanup_QcStatusEqualBlockedDoNotifyUser() {
        setUp_allFine()
        roddyBamFile.individual.project = createProject(qcThresholdHandling: QcThresholdHandling.CHECK_AND_NOTIFY)
        roddyBamFile.comment = DomainFactory.createComment()
        roddyBamFile.qcTrafficLightStatus = QcTrafficLightStatus.BLOCKED
        assert roddyBamFile.save(flush: true)
        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(roddyBamFile)
        linkFilesToFinalDestinationService.metaClass.linkNewResults = { RoddyBamFile roddyBamFile, Realm realm ->
            assert false : "should not reach this part"
        }
        DomainFactory.createProcessingOptionForNotificationRecipient()

        linkFilesToFinalDestinationService.linkToFinalDestinationAndCleanup(roddyBamFile, realm)

        assertBamFileIsFine()
    }

    @Test
    void testLinkToFinalDestinationAndCleanup_QcStatusEqualBlockedDoNotNotifyUser() {
        setUp_allFine()
        roddyBamFile.individual.project = createProject(qcThresholdHandling: QcThresholdHandling.NO_CHECK)
        roddyBamFile.comment = DomainFactory.createComment()
        roddyBamFile.qcTrafficLightStatus = QcTrafficLightStatus.BLOCKED
        assert roddyBamFile.save(flush: true)
        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(roddyBamFile)
        linkFilesToFinalDestinationService.metaClass.informResultsAreBlocked = { RoddyBamFile roddyBamFile, Realm realm ->
            assert false : "should not reach this part"
        }

        linkFilesToFinalDestinationService.linkToFinalDestinationAndCleanup(roddyBamFile, realm)

        assertBamFileIsFine()
    }

    @Test
    void testLinkToFinalDestinationAndCleanup_QcStatusEqualUnchecked() {
        setUp_allFine()
        roddyBamFile.qcTrafficLightStatus = QcTrafficLightStatus.UNCHECKED
        assert roddyBamFile.save(flush: true)
        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(roddyBamFile)
        linkFilesToFinalDestinationService.metaClass.informResultsAreBlocked = { RoddyBamFile roddyBamFile, Realm realm ->
            assert false : "should not reach this part"
        }

        linkFilesToFinalDestinationService.linkToFinalDestinationAndCleanup(roddyBamFile, realm)

        assertBamFileIsFine()
    }

    @Test
    void testLinkToFinalDestinationAndCleanup_QcStatusEqualAutoAccepted() {
        setUp_allFine()
        roddyBamFile.qcTrafficLightStatus = QcTrafficLightStatus.AUTO_ACCEPTED
        assert roddyBamFile.save(flush: true)
        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(roddyBamFile)
        linkFilesToFinalDestinationService.metaClass.informResultsAreBlocked = { RoddyBamFile roddyBamFile, Realm realm ->
            assert false : "should not reach this part"
        }

        linkFilesToFinalDestinationService.linkToFinalDestinationAndCleanup(roddyBamFile, realm)

        assertBamFileIsFine()
    }

    @Test
    void testLinkToFinalDestinationAndCleanup_QcStatusEqualAccepted_ShouldFail() {
        setUp_allFine()
        roddyBamFile.comment = DomainFactory.createComment()
        roddyBamFile.qcTrafficLightStatus = QcTrafficLightStatus.ACCEPTED
        assert roddyBamFile.save(flush: true)
        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(roddyBamFile)
        TestCase.shouldFailWithMessageContaining(RuntimeException,
                linkFilesToFinalDestinationService.getInvalidQcTrafficLightStatusMessageForStatus(QcTrafficLightStatus.ACCEPTED)) {
            linkFilesToFinalDestinationService.linkToFinalDestinationAndCleanup(roddyBamFile, realm)
        }
    }

    @Test
    void testLinkToFinalDestinationAndCleanup_QcStatusEqualRejected_ShouldFail() {
        setUp_allFine()
        roddyBamFile.comment = DomainFactory.createComment()
        roddyBamFile.qcTrafficLightStatus = QcTrafficLightStatus.REJECTED
        assert roddyBamFile.save(flush: true)
        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(roddyBamFile)
        TestCase.shouldFailWithMessageContaining(RuntimeException,
                linkFilesToFinalDestinationService.getInvalidQcTrafficLightStatusMessageForStatus(QcTrafficLightStatus.REJECTED)) {
            linkFilesToFinalDestinationService.linkToFinalDestinationAndCleanup(roddyBamFile, realm)
        }
    }

    @Test
    void testExecute_adapterTrimmingRoddyBamFileHasLessNumberOfReadsThenAllSeqTracksTogether_ShouldBeFine() {
        setUp_allFine()
        roddyBamFile.config.adapterTrimmingNeeded = true
        assert roddyBamFile.config.save(flush: true)
        CreateRoddyFileHelper.createRoddyAlignmentWorkResultFiles(roddyBamFile)
        RoddyQualityAssessment qa = roddyBamFile.overallQualityAssessment
        qa.pairedRead1--
        assert qa.save(flush: true)
        assert roddyBamFile.numberOfReadsFromQa < roddyBamFile.numberOfReadsFromFastQc

        linkFilesToFinalDestinationService.prepareRoddyBamFile(roddyBamFile)
    }

    @Test
    void testExecute_roddyBamFileHasLessNumberOfReadsThenAllSeqTracksTogether_ShouldFail() {
        setUp_allFine()
        RoddyQualityAssessment qa = roddyBamFile.overallQualityAssessment
        qa.pairedRead1--
        assert qa.save(flush: true)
        assert roddyBamFile.numberOfReadsFromQa < roddyBamFile.numberOfReadsFromFastQc

        assert shouldFail (AssertionError) {
            linkFilesToFinalDestinationService.prepareRoddyBamFile(roddyBamFile)
        } ==~ /.*bam file (.*) has less number of reads than the sum of all fastqc (.*).*/
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
