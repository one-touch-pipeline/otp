package de.dkfz.tbi.otp.job.jobs.roddyAlignment

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.job.jobs.TestRoddyAlignmentStartJob
import de.dkfz.tbi.otp.job.plan.JobDefinition
import de.dkfz.tbi.otp.job.plan.JobExecutionPlan
import de.dkfz.tbi.otp.job.processing.Process
import de.dkfz.tbi.otp.job.processing.ProcessParameter
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.ngsdata.Run
import de.dkfz.tbi.otp.ngsdata.RunSegment
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.utils.ExternalScript
import org.junit.After
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired

import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement

import de.dkfz.tbi.otp.utils.HelperUtils

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

        assert [highPriority, lowPriority] == RoddyAlignmentStartJob.findProcessableMergingWorkPackages(10 as short)
    }

    @Test
    void testFindProcessableMergingWorkPackages_WhenRoddyBamFileIsNotProcessedAndNotWithdrawn_ShouldReturnEmptyList() {
        DomainFactory.createRoddyBamFile([
                workPackage: createMergingWorkPackage(),
                fileOperationStatus: AbstractMergedBamFile.FileOperationStatus.DECLARED,
                md5sum: null,
                withdrawn: false,
        ])

        assert [] == RoddyAlignmentStartJob.findProcessableMergingWorkPackages(Short.MIN_VALUE)
    }

    @Test
    void testFindProcessableMergingWorkPackages_WhenRoddyBamFileIsNotProcessedButWithdrawn_ShouldReturnMergingWorkPackage() {
        RoddyBamFile roddyBamFile = DomainFactory.createRoddyBamFile([
                workPackage: createMergingWorkPackage(),
                fileOperationStatus: AbstractMergedBamFile.FileOperationStatus.NEEDS_PROCESSING,
                md5sum: null,
                withdrawn: true,
        ])

        assert [roddyBamFile.workPackage] == RoddyAlignmentStartJob.findProcessableMergingWorkPackages(Short.MIN_VALUE)
    }

    @Test
    void testFindProcessableMergingWorkPackages_WhenRoddyBamFileIsProcessed_ShouldReturnMergingWorkPackage() {
        RoddyBamFile roddyBamFile = DomainFactory.createRoddyBamFile([
                workPackage: createMergingWorkPackage(),
                fileOperationStatus: AbstractMergedBamFile.FileOperationStatus.PROCESSED,
                withdrawn: false,
        ])

        assert [roddyBamFile.workPackage] == RoddyAlignmentStartJob.findProcessableMergingWorkPackages(Short.MIN_VALUE)
    }

    @Test
    void testIsDataInstallationWFInProgress_WhenFilesCorrect_ShouldReturnFalse() {
        MergingWorkPackage mwp = createMergingWorkPackage(RunSegment.FilesStatus.FILES_CORRECT)

        assert false == RoddyAlignmentStartJob.isDataInstallationWFInProgress(mwp)
    }

    @Test
    void testIsDataInstallationWFInProgress_WhenFilesNotCorrect_ShouldReturnTrue() {
        MergingWorkPackage mwp = createMergingWorkPackage(RunSegment.FilesStatus.FILES_MISSING)

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
        assert null == RoddyAlignmentStartJob.findUsableBaseBamFile(MergingWorkPackage.build())
    }

    @Test
    void testFindUsableBaseBamFile_WhenBamFileInProjectFolderIsWithdrawn_ShouldReturnNull() {
        MergingWorkPackage mwp = createMergingWorkPackage()
        RoddyBamFile bamFile = DomainFactory.createRoddyBamFile([
                workPackage: mwp,
                withdrawn: true
        ])

        mwp.bamFileInProjectFolder = bamFile

        assert null == RoddyAlignmentStartJob.findUsableBaseBamFile(bamFile.mergingWorkPackage)
    }

    @Test
    void testFindUsableBaseBamFile_WhenBamFileInProjectFolderIsUsable_ShouldReturnIt() {
        MergingWorkPackage mwp = createMergingWorkPackage()
        RoddyBamFile bamFile = DomainFactory.createRoddyBamFile([
                workPackage: mwp
        ])

        mwp.bamFileInProjectFolder = bamFile

        assert bamFile == RoddyAlignmentStartJob.findUsableBaseBamFile(bamFile.mergingWorkPackage)
    }

    @Test
    void testCreateRoddyBamFile_WhenBaseBamFileIsNull() {
        MergingWorkPackage mwp = createMergingWorkPackage()
        DomainFactory.buildSeqTrackWithDataFile(mwp)
        Collection<SeqTrack> seqTracks = mwp.findMergeableSeqTracks()
        DomainFactory.createRoddyProcessingOptions(TestCase.uniqueNonExistentPath)

        RoddyBamFile rbf = RoddyAlignmentStartJob.createRoddyBamFile(mwp, null)

        assertRoddyBamFileConsistencyWithMwp(rbf, mwp)
        assert null == rbf.baseBamFile
        assert TestCase.containSame(seqTracks, rbf.seqTracks)
        assert seqTracks.size() == rbf.numberOfMergedLanes
        assert TestCase.containSame(seqTracks, rbf.containedSeqTracks)
    }

    @Test
    void testCreateRoddyBamFile_WhenBaseBamFileIsNotNull() {
        RoddyBamFile baseBamFile = DomainFactory.createRoddyBamFile()
        MergingWorkPackage mwp = baseBamFile.mergingWorkPackage
        Collection<SeqTrack> additionalSeqTracks = [
                DomainFactory.buildSeqTrackWithDataFile(mwp),
                DomainFactory.buildSeqTrackWithDataFile(mwp),
                DomainFactory.buildSeqTrackWithDataFile(mwp),
        ]

        DomainFactory.createRoddyProcessingOptions(TestCase.uniqueNonExistentPath)

        RoddyBamFile rbf = RoddyAlignmentStartJob.createRoddyBamFile(mwp, baseBamFile)

        assertRoddyBamFileConsistencyWithMwp(rbf, mwp)
        assert baseBamFile == rbf.baseBamFile
        assert TestCase.containSame(additionalSeqTracks, rbf.seqTracks)
        assert baseBamFile.numberOfMergedLanes + additionalSeqTracks.size() == rbf.numberOfMergedLanes
        assert TestCase.containSame(additionalSeqTracks + baseBamFile.containedSeqTracks, rbf.containedSeqTracks)
    }

    @Test
    void testCreateRoddyBamFile_WhenBaseBamFileIsNullAndNoConfigAvailable_ShouldFail() {
        MergingWorkPackage mwp = createMergingWorkPackage()
        DomainFactory.buildSeqTrackWithDataFile(mwp)
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
        DomainFactory.buildSeqTrackWithDataFile(mwp)
        Collection<SeqTrack> seqTracks = mwp.findMergeableSeqTracks()

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
        MergingWorkPackage mwp = createMergingWorkPackage()
        DomainFactory.createRoddyProcessingOptions(TestCase.uniqueNonExistentPath)

        withJobExecutionPlan {
            testRoddyAlignmentStartJob.startRoddyAlignment()
        }

        assert mwp.needsProcessing == false
        Process process = exactlyOneElement(Process.list())
        ProcessParameter processParameter = exactlyOneElement(ProcessParameter.findAllByProcess(process))
        RoddyBamFile bamFile = processParameter.toObject()
        assertRoddyBamFileConsistencyWithMwp(bamFile, mwp)
    }

    private void assertRoddyBamFileConsistencyWithMwp(RoddyBamFile rbf, MergingWorkPackage mwp) {
        assert mwp == rbf.workPackage
        assert RoddyBamFile.maxIdentifier(mwp) == rbf.identifier
        assert mwp.workflow == rbf.config.workflow
        assert mwp.project == rbf.config.project
        assert null == rbf.config.obsoleteDate
    }

    private MergingWorkPackage createMergingWorkPackage(RunSegment.FilesStatus filesStatus = RunSegment.FilesStatus.FILES_CORRECT) {
        Workflow workflow = DomainFactory.createPanCanWorkflow()
        MergingWorkPackage mwp = MergingWorkPackage.build([
                needsProcessing: true,
                workflow: workflow,
                statSizeFileName: DomainFactory.DEFAULT_TAB_FILE_NAME,
        ])
        RoddyWorkflowConfig.build([workflow: workflow, project: mwp.project, pluginVersion: HelperUtils.uniqueString])
        Run run = Run.build()
        DomainFactory.buildSeqTrackWithDataFile(mwp, [run: run])
        RunSegment.build(run: run, filesStatus: filesStatus)
        return mwp
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
