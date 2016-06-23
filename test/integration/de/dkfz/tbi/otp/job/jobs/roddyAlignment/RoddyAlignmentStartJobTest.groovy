package de.dkfz.tbi.otp.job.jobs.roddyAlignment

import de.dkfz.tbi.*
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.*
import de.dkfz.tbi.otp.job.jobs.*
import de.dkfz.tbi.otp.job.plan.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.tracking.*
import org.junit.*
import org.springframework.beans.factory.annotation.*

import static de.dkfz.tbi.otp.utils.CollectionUtils.*

class RoddyAlignmentStartJobTest {

    @Autowired
    TestRoddyAlignmentStartJob testRoddyAlignmentStartJob

    @After
    void tearDown() {
        TestCase.cleanTestDirectory()
    }

    @Test
    void testFindProcessableMergingWorkPackages_WhenSeveralMergingWorkPackages_ShouldReturnOrderedMergingWorkPackageList() {
        MergingWorkPackage tooLowPriority = createMergingWorkPackage()
        tooLowPriority.project.processingPriority = 5
        assert tooLowPriority.save(flush: true, failOnError: true)

        MergingWorkPackage lowPriority = createMergingWorkPackage()
        lowPriority.project.processingPriority = 100
        assert lowPriority.save(flush: true, failOnError: true)

        MergingWorkPackage highPriority = createMergingWorkPackage()
        highPriority.project.processingPriority = 1000
        assert highPriority.save(flush: true, failOnError: true)

        MergingWorkPackage doesNotNeedProcessing = createMergingWorkPackage()
        doesNotNeedProcessing.project.processingPriority = 1000
        doesNotNeedProcessing.needsProcessing = false
        assert doesNotNeedProcessing.save(flush: true, failOnError: true)

        assert [highPriority, lowPriority] == testRoddyAlignmentStartJob.findProcessableMergingWorkPackages(10 as short)
    }

    @Test
    void testFindProcessableMergingWorkPackages_WhenRoddyBamFileIsNotProcessedAndNotWithdrawn_ShouldReturnEmptyList() {
        DomainFactory.createRoddyBamFile([
                workPackage: createMergingWorkPackage(),
                fileOperationStatus: AbstractMergedBamFile.FileOperationStatus.DECLARED,
                md5sum: null,
                withdrawn: false,
        ])

        assert [] == testRoddyAlignmentStartJob.findProcessableMergingWorkPackages(Short.MIN_VALUE)
    }

    @Test
    void testFindProcessableMergingWorkPackages_WhenRoddyBamFileIsNotProcessedButWithdrawn_ShouldReturnMergingWorkPackage() {
        RoddyBamFile roddyBamFile = DomainFactory.createRoddyBamFile([
                workPackage: createMergingWorkPackage(),
                fileOperationStatus: AbstractMergedBamFile.FileOperationStatus.NEEDS_PROCESSING,
                md5sum: null,
                withdrawn: true,
        ])

        assert [roddyBamFile.workPackage] == testRoddyAlignmentStartJob.findProcessableMergingWorkPackages(Short.MIN_VALUE)
    }

    @Test
    void testFindProcessableMergingWorkPackages_WhenRoddyBamFileIsProcessed_ShouldReturnMergingWorkPackage() {
        RoddyBamFile roddyBamFile = DomainFactory.createRoddyBamFile([
                workPackage: createMergingWorkPackage(),
                fileOperationStatus: AbstractMergedBamFile.FileOperationStatus.PROCESSED,
                withdrawn: false,
        ])

        assert [roddyBamFile.workPackage] == testRoddyAlignmentStartJob.findProcessableMergingWorkPackages(Short.MIN_VALUE)
    }

    @Test
    void testIsDataInstallationWFInProgress_WhenFilesCorrect_ShouldReturnFalse() {
        MergingWorkPackage mwp = createMergingWorkPackageWithRunFileStatus(RunSegment.FilesStatus.FILES_CORRECT)

        assert false == RoddyAlignmentStartJob.isDataInstallationWFInProgress(mwp)
    }

    @Test
    void testIsDataInstallationWFInProgress_WhenFilesNotCorrect_ShouldReturnTrue() {
        MergingWorkPackage mwp = createMergingWorkPackageWithRunFileStatus(RunSegment.FilesStatus.FILES_MISSING)

        assert RoddyAlignmentStartJob.isDataInstallationWFInProgress(mwp)
    }

    @Test
    void testFindBamFileInProjectFolder_WhenNoRoddyBamFile_ShouldReturnNull() {
        MergingWorkPackage mwp = createMergingWorkPackage()

        assert null == RoddyAlignmentStartJob.findBamFileInProjectFolder(mwp)
    }

    @Test
    void testFindBamFileInProjectFolder_WhenNoRoddyBamFileInProgressOrProcessed_ShouldReturnNull() {
        MergingWorkPackage mwp = createMergingWorkPackage()
        DomainFactory.createRoddyBamFile([
                workPackage: mwp,
                fileOperationStatus: AbstractMergedBamFile.FileOperationStatus.DECLARED,
                md5sum: null,
                withdrawn: true,
        ])
        DomainFactory.createRoddyBamFile([
                workPackage: mwp,
                fileOperationStatus: AbstractMergedBamFile.FileOperationStatus.NEEDS_PROCESSING,
                md5sum: null,
                withdrawn: true,
        ])

        assert null == RoddyAlignmentStartJob.findBamFileInProjectFolder(mwp)
    }

    @Test
    void testFindBamFileInProjectFolder_WhenLatestBamFileIsInProgress_ShouldReturnNull() {
        MergingWorkPackage mwp = createMergingWorkPackage()
        DomainFactory.createRoddyBamFile([
                workPackage: mwp,
                fileOperationStatus: AbstractMergedBamFile.FileOperationStatus.PROCESSED,
        ])
        RoddyBamFile roddyBamFile = DomainFactory.createRoddyBamFile([
                workPackage: mwp,
                fileOperationStatus: AbstractMergedBamFile.FileOperationStatus.INPROGRESS,
                md5sum: null,
                withdrawn: true,
        ])

        mwp.bamFileInProjectFolder = roddyBamFile

        assert null == RoddyAlignmentStartJob.findBamFileInProjectFolder(mwp)
    }

    @Test
    void testFindBamFileInProjectFolder_WhenLatestBamFileNeitherInProgressNorProcessed_ShouldReturnEarlierFile() {
        MergingWorkPackage mwp = createMergingWorkPackage()
        RoddyBamFile roddyBamFile = DomainFactory.createRoddyBamFile([
                workPackage: mwp,
                fileOperationStatus: AbstractMergedBamFile.FileOperationStatus.PROCESSED,
        ])
        DomainFactory.createRoddyBamFile([
                workPackage: mwp,
                fileOperationStatus: AbstractMergedBamFile.FileOperationStatus.DECLARED,
                md5sum: null,
                withdrawn: true,
        ])

        mwp.bamFileInProjectFolder = roddyBamFile

        assert roddyBamFile == RoddyAlignmentStartJob.findBamFileInProjectFolder(mwp)
    }

    @Test
    void testFindUsableBaseBamFile_WhenMergingWorkPackageHasNoBamFile_ShouldReturnNull() {
        assert null == testRoddyAlignmentStartJob.findUsableBaseBamFile(MergingWorkPackage.build())
    }

    @Test
    void testFindUsableBaseBamFile_WhenBamFileInProjectFolderIsWithdrawn_ShouldReturnNull() {
        MergingWorkPackage mwp = createMergingWorkPackage()
        RoddyBamFile bamFile = DomainFactory.createRoddyBamFile([
                workPackage: mwp,
                withdrawn: true
        ])

        mwp.bamFileInProjectFolder = bamFile

        assert null == testRoddyAlignmentStartJob.findUsableBaseBamFile(bamFile.mergingWorkPackage)
    }

    @Test
    void testFindUsableBaseBamFile_WhenBamFileInProjectFolderIsUsable_ShouldReturnIt() {
        MergingWorkPackage mwp = createMergingWorkPackage()
        RoddyBamFile bamFile = DomainFactory.createRoddyBamFile([
                workPackage: mwp
        ])

        mwp.bamFileInProjectFolder = bamFile

        // TODO OTP-2192: incremental merging is currently not supported by Roddy
        //assert bamFile == testRoddyAlignmentStartJob.findUsableBaseBamFile(bamFile.mergingWorkPackage)
        assert null == testRoddyAlignmentStartJob.findUsableBaseBamFile(bamFile.mergingWorkPackage)
    }

    @Test
    void testCreateRoddyBamFile_WhenBaseBamFileIsNull() {
        MergingWorkPackage mwp = createMergingWorkPackage()
        DomainFactory.createRoddyWorkflowConfig([pipeline: mwp.pipeline, project: mwp.project])
        DomainFactory.createSeqTrackWithDataFiles(mwp)
        Collection<SeqTrack> seqTracks = mwp.findMergeableSeqTracks()
        DomainFactory.createRoddyProcessingOptions(TestCase.uniqueNonExistentPath)

        RoddyBamFile rbf = RoddyAlignmentStartJob.createRoddyBamFile(mwp, null)

        assertRoddyBamFileConsistencyWithMwp(rbf, mwp)
        assert null == rbf.baseBamFile
        assert TestCase.containSame(seqTracks, rbf.seqTracks)
        assert seqTracks.size() == rbf.numberOfMergedLanes
        assert TestCase.containSame(seqTracks, rbf.containedSeqTracks)
        assert rbf.workDirectoryName && rbf.workDirectoryName.startsWith(RoddyBamFile.WORK_DIR_PREFIX)
        assert !rbf.isOldStructureUsed()
    }

    @Test
    void testCreateRoddyBamFile_WhenBaseBamFileIsNotNull() {
        RoddyBamFile baseBamFile = DomainFactory.createRoddyBamFile()
        MergingWorkPackage mwp = baseBamFile.mergingWorkPackage
        Collection<SeqTrack> additionalSeqTracks = [
                DomainFactory.createSeqTrackWithDataFiles(mwp),
                DomainFactory.createSeqTrackWithDataFiles(mwp),
                DomainFactory.createSeqTrackWithDataFiles(mwp),
        ]

        DomainFactory.createRoddyProcessingOptions(TestCase.uniqueNonExistentPath)

        RoddyBamFile rbf = RoddyAlignmentStartJob.createRoddyBamFile(mwp, baseBamFile)

        assertRoddyBamFileConsistencyWithMwp(rbf, mwp)
        assert baseBamFile == rbf.baseBamFile
        assert TestCase.containSame(additionalSeqTracks, rbf.seqTracks)
        assert baseBamFile.numberOfMergedLanes + additionalSeqTracks.size() == rbf.numberOfMergedLanes
        assert TestCase.containSame(additionalSeqTracks + baseBamFile.containedSeqTracks, rbf.containedSeqTracks)
        assert rbf.workDirectoryName && rbf.workDirectoryName.startsWith(RoddyBamFile.WORK_DIR_PREFIX)
        assert !rbf.isOldStructureUsed()
    }

    @Test
    void testCreateRoddyBamFile_WhenBaseBamFileIsNullAndNoConfigAvailable_ShouldFail() {
        MergingWorkPackage mwp = createMergingWorkPackage()
        DomainFactory.createSeqTrackWithDataFiles(mwp)
        Collection<SeqTrack> seqTracks = mwp.findMergeableSeqTracks()
        DomainFactory.createRoddyProcessingOptions()
        RoddyWorkflowConfig.list()*.delete(flush: true)
        assert 0 == RoddyWorkflowConfig.list().size()

        assert TestCase.shouldFail (AssertionError) {
            RoddyBamFile rbf = RoddyAlignmentStartJob.createRoddyBamFile(mwp, null)
        }.contains('RoddyWorkflowConfig')
    }

    @Test
    void testCreateRoddyBamFile_WhenBaseBamFileIsNullAndProcessingOptionDoesNotExist_ShouldFail() {
        MergingWorkPackage mwp = createMergingWorkPackage()
        DomainFactory.createSeqTrackWithDataFiles(mwp)
        Collection<SeqTrack> seqTracks = mwp.findMergeableSeqTracks()
        DomainFactory.createRoddyWorkflowConfig([pipeline: mwp.pipeline, project: mwp.project])

        assert TestCase.shouldFail (AssertionError) {
            RoddyBamFile rbf = RoddyAlignmentStartJob.createRoddyBamFile(mwp, null)
        }.contains('roddyVersion')
    }

    @Test
    void testStartRoddyAlignment_WhenProcessingPriorityIsTooLow_ShouldNotCreateProcess() {
        MergingWorkPackage mwp = createMergingWorkPackage()
        mwp.project.processingPriority = ProcessingPriority.NORMAL_PRIORITY - 1
        assert mwp.save(flush: true, failOnError: true)

        withJobExecutionPlan {
            testRoddyAlignmentStartJob.startRoddyAlignment()
        }

        assert mwp.needsProcessing == true
        assert Process.count() == 0
        assert RoddyBamFile.count() == 0
    }

    @Test
    void testStartRoddyAlignment_WhenEverythingIsOkay_ShouldCreateProcess() {
        MergingWorkPackage mwp = createMergingWorkPackageWithRunFileStatus(RunSegment.FilesStatus.FILES_CORRECT)
        DomainFactory.createRoddyProcessingOptions(TestCase.uniqueNonExistentPath)
        DomainFactory.createRoddyWorkflowConfig([pipeline: mwp.pipeline, project: mwp.project])

        withJobExecutionPlan {
            testRoddyAlignmentStartJob.startRoddyAlignment()
        }

        assert mwp.needsProcessing == false
        Process process = exactlyOneElement(Process.list())
        ProcessParameter processParameter = exactlyOneElement(ProcessParameter.findAllByProcess(process))
        RoddyBamFile bamFile = processParameter.toObject()
        assertRoddyBamFileConsistencyWithMwp(bamFile, mwp)
    }

    @Test
    void executeCallsSetStartedForSeqTracks() {
        MergingWorkPackage mwp = createMergingWorkPackageWithRunFileStatus(RunSegment.FilesStatus.FILES_CORRECT)
        OtrsTicket otrsTicket = DomainFactory.createOtrsTicket()
        DataFile.findAll()*.runSegment = DomainFactory.createRunSegment(otrsTicket: otrsTicket)
        DomainFactory.createRoddyProcessingOptions(TestCase.uniqueNonExistentPath)
        DomainFactory.createRoddyWorkflowConfig([pipeline: mwp.pipeline, project: mwp.project])

        withJobExecutionPlan {
            testRoddyAlignmentStartJob.startRoddyAlignment()
        }

        assert otrsTicket.alignmentStarted != null
    }



    private void assertRoddyBamFileConsistencyWithMwp(RoddyBamFile rbf, MergingWorkPackage mwp) {
        assert mwp == rbf.workPackage
        assert RoddyBamFile.maxIdentifier(mwp) == rbf.identifier
        assert mwp.pipeline == rbf.config.pipeline
        assert mwp.project == rbf.config.project
        assert null == rbf.config.obsoleteDate
    }

    MergingWorkPackage createMergingWorkPackage() {
        return DomainFactory.createMergingWorkPackage([
                seqType        : DomainFactory.createWholeGenomeSeqType(),
                needsProcessing: true,
                pipeline       : DomainFactory.createPanCanPipeline(),
        ])
    }

    MergingWorkPackage createMergingWorkPackageWithRunFileStatus(RunSegment.FilesStatus filesStatus) {
        MergingWorkPackage mergingWorkPackage = createMergingWorkPackage()
        Run run = DomainFactory.createRun()
        DomainFactory.createSeqTrackWithDataFiles(mergingWorkPackage, [run: run])
        DomainFactory.createRunSegment(run: run, filesStatus: filesStatus)
        return mergingWorkPackage
    }

    private void withJobExecutionPlan(Closure closure) {
        try {
            JobExecutionPlan jep = JobExecutionPlan.build(enabled: true)
            jep.firstJob = JobDefinition.build(plan: jep)
            assert jep.save(failOnError: true)
            testRoddyAlignmentStartJob.jep = jep
            closure()
        } finally {
            testRoddyAlignmentStartJob.jep = null
        }
    }
}
