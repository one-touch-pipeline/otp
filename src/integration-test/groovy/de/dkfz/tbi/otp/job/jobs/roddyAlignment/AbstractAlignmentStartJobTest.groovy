/*
 * Copyright 2011-2019 The OTP authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package de.dkfz.tbi.otp.job.jobs.roddyAlignment

import grails.testing.mixin.integration.Integration
import grails.transaction.Rollback
import org.junit.After
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.job.jobs.TestAbstractAlignmentStartJob
import de.dkfz.tbi.otp.job.jobs.alignment.AbstractAlignmentStartJob
import de.dkfz.tbi.otp.job.plan.JobExecutionPlan
import de.dkfz.tbi.otp.job.processing.Process
import de.dkfz.tbi.otp.job.processing.ProcessParameter
import de.dkfz.tbi.otp.job.scheduler.SchedulerService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.tracking.OtrsTicket
import de.dkfz.tbi.otp.utils.SessionUtils

import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement

@Rollback
@Integration
class AbstractAlignmentStartJobTest {

    @Autowired
    TestAbstractAlignmentStartJob testAbstractAlignmentStartJob

    SchedulerService schedulerService

    boolean originalSchedulerActive

    void setupData() {
        originalSchedulerActive = schedulerService.schedulerActive
        schedulerService.schedulerActive = true
        SessionUtils.metaClass.static.withNewSession = { Closure c -> c() }
    }

    @After
    void tearDown() {
        schedulerService.schedulerActive = originalSchedulerActive
        TestCase.cleanTestDirectory()
        TestCase.removeMetaClass(SessionUtils)
    }

    @Test
    void testFindProcessableMergingWorkPackages_WhenSeveralMergingWorkPackages_ShouldReturnOrderedMergingWorkPackageList() {
        setupData()
        MergingWorkPackage tooLowPriority = createMergingWorkPackage()
        tooLowPriority.project.processingPriority = ProcessingPriority.MINIMUM.priority
        assert tooLowPriority.save(flush: true)

        MergingWorkPackage lowPriority = createMergingWorkPackage()
        lowPriority.project.processingPriority = ProcessingPriority.NORMAL.priority
        assert lowPriority.save(flush: true)

        MergingWorkPackage highPriority = createMergingWorkPackage()
        highPriority.project.processingPriority = ProcessingPriority.FAST_TRACK.priority
        assert highPriority.save(flush: true)

        MergingWorkPackage doesNotNeedProcessing = createMergingWorkPackage()
        doesNotNeedProcessing.project.processingPriority = ProcessingPriority.FAST_TRACK.priority
        doesNotNeedProcessing.needsProcessing = false
        assert doesNotNeedProcessing.save(flush: true)

        assert [highPriority, lowPriority] == testAbstractAlignmentStartJob.findProcessableMergingWorkPackages(ProcessingPriority.NORMAL)
    }

    @Test
    void testFindProcessableMergingWorkPackages_WhenMergingWorkPackagesHasNoSeqTracks_ShouldNotReturnThatMergingWorkpackage() {
        setupData()
        MergingWorkPackage mwp = createMergingWorkPackage()
        mwp.seqTracks = null
        mwp.save()

        assert [] == testAbstractAlignmentStartJob.findProcessableMergingWorkPackages(ProcessingPriority.MINIMUM)
    }

    @Test
    void testFindProcessableMergingWorkPackages_WhenRoddyBamFileIsNotProcessedAndNotWithdrawn_ShouldReturnEmptyList() {
        setupData()
        DomainFactory.createRoddyBamFile([
                workPackage: createMergingWorkPackage(),
                fileOperationStatus: AbstractMergedBamFile.FileOperationStatus.DECLARED,
                md5sum: null,
                withdrawn: false,
        ])

        assert [] == testAbstractAlignmentStartJob.findProcessableMergingWorkPackages(ProcessingPriority.MINIMUM)
    }

    @Test
    void testFindProcessableMergingWorkPackages_WhenRoddyBamFileIsNotProcessedButWithdrawn_ShouldReturnMergingWorkPackage() {
        setupData()
        RoddyBamFile roddyBamFile = DomainFactory.createRoddyBamFile([
                workPackage: createMergingWorkPackage(),
                fileOperationStatus: AbstractMergedBamFile.FileOperationStatus.NEEDS_PROCESSING,
                md5sum: null,
                withdrawn: true,
        ])

        assert [roddyBamFile.workPackage] == testAbstractAlignmentStartJob.findProcessableMergingWorkPackages(ProcessingPriority.MINIMUM)
    }

    @Test
    void testFindProcessableMergingWorkPackages_WhenRoddyBamFileIsProcessed_ShouldReturnMergingWorkPackage() {
        setupData()
        RoddyBamFile roddyBamFile = DomainFactory.createRoddyBamFile([
                workPackage: createMergingWorkPackage(),
                fileOperationStatus: AbstractMergedBamFile.FileOperationStatus.PROCESSED,
                withdrawn: false,
        ])

        assert [roddyBamFile.workPackage] == testAbstractAlignmentStartJob.findProcessableMergingWorkPackages(ProcessingPriority.MINIMUM)
    }

    @Test
    void testIsDataInstallationWFInProgress_WhenSeqTrackInStateFinished_ShouldReturnFalse() {
        setupData()
        MergingWorkPackage mwp = createMergingWorkPackageWithSeqTrackInState(SeqTrack.DataProcessingState.FINISHED)

        assert false == AbstractAlignmentStartJob.isDataInstallationWFInProgress(mwp)
    }

    @Test
    void testIsDataInstallationWFInProgress_WhenSeqTrackInProgress_ShouldReturnTrue() {
        setupData()
        MergingWorkPackage mwp = createMergingWorkPackageWithSeqTrackInState(SeqTrack.DataProcessingState.IN_PROGRESS)

        assert AbstractAlignmentStartJob.isDataInstallationWFInProgress(mwp)
    }

    @Test
    void testIsDataInstallationWFInProgress_WhenSeqTrackInStateNotStarted_ShouldReturnTrue() {
        setupData()
        MergingWorkPackage mwp = createMergingWorkPackageWithSeqTrackInState(SeqTrack.DataProcessingState.NOT_STARTED)

        assert AbstractAlignmentStartJob.isDataInstallationWFInProgress(mwp)
    }

    @Test
    void testFindBamFileInProjectFolder_WhenNoRoddyBamFile_ShouldReturnNull() {
        setupData()
        MergingWorkPackage mwp = createMergingWorkPackage()

        assert null == AbstractAlignmentStartJob.findBamFileInProjectFolder(mwp)
    }

    @Test
    void testFindBamFileInProjectFolder_WhenNoRoddyBamFileInProgressOrProcessed_ShouldReturnNull() {
        setupData()
        MergingWorkPackage mwp = createMergingWorkPackage()
        RoddyBamFile roddyBamFile = DomainFactory.createRoddyBamFile([
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
                config: roddyBamFile.config,
        ])

        assert null == AbstractAlignmentStartJob.findBamFileInProjectFolder(mwp)
    }

    @Test
    void testFindBamFileInProjectFolder_WhenLatestBamFileIsInProgress_ShouldReturnNull() {
        setupData()
        MergingWorkPackage mwp = createMergingWorkPackage()
        RoddyBamFile rbf = DomainFactory.createRoddyBamFile([
                workPackage: mwp,
                fileOperationStatus: AbstractMergedBamFile.FileOperationStatus.PROCESSED,
        ])
        RoddyBamFile roddyBamFile = DomainFactory.createRoddyBamFile([
                workPackage: mwp,
                fileOperationStatus: AbstractMergedBamFile.FileOperationStatus.INPROGRESS,
                md5sum: null,
                withdrawn: true,
                config: rbf.config,
        ])

        mwp.bamFileInProjectFolder = roddyBamFile

        assert null == AbstractAlignmentStartJob.findBamFileInProjectFolder(mwp)
    }

    @Test
    void testFindBamFileInProjectFolder_WhenLatestBamFileNeitherInProgressNorProcessed_ShouldReturnEarlierFile() {
        setupData()
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
                config: roddyBamFile.config,
        ])

        mwp.bamFileInProjectFolder = roddyBamFile

        assert roddyBamFile == AbstractAlignmentStartJob.findBamFileInProjectFolder(mwp)
    }

    @Test
    void testFindUsableBaseBamFile_WhenMergingWorkPackageHasNoBamFile_ShouldReturnNull() {
        setupData()
        assert null == testAbstractAlignmentStartJob.findUsableBaseBamFile(DomainFactory.createMergingWorkPackage())
    }

    @Test
    void testFindUsableBaseBamFile_WhenBamFileInProjectFolderIsWithdrawn_ShouldReturnNull() {
        setupData()
        MergingWorkPackage mwp = createMergingWorkPackage()
        RoddyBamFile bamFile = DomainFactory.createRoddyBamFile([
                workPackage: mwp,
                withdrawn: true,
        ])

        mwp.bamFileInProjectFolder = bamFile

        assert null == testAbstractAlignmentStartJob.findUsableBaseBamFile(bamFile.mergingWorkPackage)
    }

    @Test
    void testFindUsableBaseBamFile_WhenBamFileInProjectFolderIsUsable_ShouldReturnIt() {
        setupData()
        MergingWorkPackage mwp = createMergingWorkPackage()
        RoddyBamFile bamFile = DomainFactory.createRoddyBamFile([
                workPackage: mwp,
        ])

        mwp.bamFileInProjectFolder = bamFile

        assert bamFile == testAbstractAlignmentStartJob.findUsableBaseBamFile(bamFile.mergingWorkPackage)
    }

    @Test
    void testCreateRoddyBamFile_WithoutIndividualConfig_WhenBaseBamFileIsNull() {
        setupData()
        MergingWorkPackage mwp = createMergingWorkPackage()
        DomainFactory.createRoddyWorkflowConfig([pipeline: mwp.pipeline, project: mwp.project])
        helperTestCreateRoddyBamFile_WhenBaseBamFileIsNull(mwp)
    }

    @Test
    void testCreateRoddyBamFile_WithIndividualConfig_WhenBaseBamFileIsNull() {
        setupData()
        MergingWorkPackage mwp = createMergingWorkPackage()
        DomainFactory.createRoddyWorkflowConfig([pipeline: mwp.pipeline, project: mwp.project])
        RoddyWorkflowConfig config = DomainFactory.createRoddyWorkflowConfig([pipeline: mwp.pipeline, project: mwp.project, individual: mwp.individual])
        RoddyBamFile rbf = helperTestCreateRoddyBamFile_WhenBaseBamFileIsNull(mwp)

        assert rbf.config == config
    }

    RoddyBamFile helperTestCreateRoddyBamFile_WhenBaseBamFileIsNull(MergingWorkPackage mwp) {
        setupData()
        Collection<SeqTrack> seqTracks = [DomainFactory.createSeqTrackWithDataFiles(mwp)]
        mwp.seqTracks = seqTracks
        mwp.save(flush: true)
        DomainFactory.createRoddyProcessingOptions(TestCase.uniqueNonExistentPath)

        RoddyBamFile rbf = testAbstractAlignmentStartJob.createBamFile(mwp, null)

        assertRoddyBamFileConsistencyWithMwp(rbf, mwp)
        assert null == rbf.baseBamFile
        assert TestCase.containSame(seqTracks, rbf.seqTracks)
        assert seqTracks.size() == rbf.numberOfMergedLanes
        assert TestCase.containSame(seqTracks, rbf.containedSeqTracks)
        assert rbf.workDirectoryName && rbf.workDirectoryName.startsWith(RoddyBamFile.WORK_DIR_PREFIX)
        assert !rbf.isOldStructureUsed()

        return rbf
    }


    @Test
    void testCreateRoddyBamFile_WhenBaseBamFileIsNotNull() {
        setupData()
        RoddyBamFile baseBamFile = DomainFactory.createRoddyBamFile()
        MergingWorkPackage mwp = baseBamFile.mergingWorkPackage
        Collection<SeqTrack> additionalSeqTracks = [
                DomainFactory.createSeqTrackWithDataFiles(mwp),
                DomainFactory.createSeqTrackWithDataFiles(mwp),
                DomainFactory.createSeqTrackWithDataFiles(mwp),
        ]
        mwp.seqTracks.addAll(additionalSeqTracks)
        mwp.save(flush: true)

        DomainFactory.createRoddyProcessingOptions(TestCase.uniqueNonExistentPath)

        RoddyBamFile rbf = testAbstractAlignmentStartJob.createBamFile(mwp, baseBamFile)

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
        setupData()
        MergingWorkPackage mwp = createMergingWorkPackage()
        DomainFactory.createSeqTrackWithDataFiles(mwp)
        DomainFactory.createRoddyProcessingOptions()
        RoddyWorkflowConfig.list()*.delete(flush: true)
        assert 0 == RoddyWorkflowConfig.list().size()

        assert TestCase.shouldFail (AssertionError) {
            testAbstractAlignmentStartJob.createBamFile(mwp, null)
        }.contains('RoddyWorkflowConfig')
    }

    @Test
    void testStartRoddyAlignment_WhenProcessingPriorityIsTooLow_ShouldNotCreateProcess() {
        setupData()
        MergingWorkPackage mwp = createMergingWorkPackage()
        mwp.project.processingPriority = ProcessingPriority.NORMAL.priority - 1 as short
        assert mwp.save(flush: true)

        withJobExecutionPlan {
            testAbstractAlignmentStartJob.startAlignment()
        }

        assert mwp.needsProcessing == true
        assert Process.count() == 0
        assert RoddyBamFile.count() == 0
    }

    @Test
    void testStartRoddyAlignment_WhenEverythingIsOkay_ShouldCreateProcess() {
        setupData()
        MergingWorkPackage mwp = createMergingWorkPackageWithSeqTrackInState(SeqTrack.DataProcessingState.FINISHED)
        DomainFactory.createRoddyProcessingOptions(TestCase.uniqueNonExistentPath)
        DomainFactory.createRoddyWorkflowConfig([pipeline: mwp.pipeline, project: mwp.project])

        withJobExecutionPlan {
            testAbstractAlignmentStartJob.startAlignment()
        }

        assert mwp.needsProcessing == false
        Process process = exactlyOneElement(Process.list())
        ProcessParameter processParameter = exactlyOneElement(ProcessParameter.findAllByProcess(process))
        RoddyBamFile bamFile = processParameter.toObject()
        assertRoddyBamFileConsistencyWithMwp(bamFile, mwp)
    }

    @Test
    void executeCallsSetStartedForSeqTracks() {
        setupData()
        MergingWorkPackage mwp = createMergingWorkPackageWithSeqTrackInState(SeqTrack.DataProcessingState.FINISHED)
        OtrsTicket otrsTicket = DomainFactory.createOtrsTicket()
        DataFile.findAll()*.runSegment = DomainFactory.createRunSegment(otrsTicket: otrsTicket)
        DomainFactory.createRoddyProcessingOptions(TestCase.uniqueNonExistentPath)
        DomainFactory.createRoddyWorkflowConfig([pipeline: mwp.pipeline, project: mwp.project])

        withJobExecutionPlan {
            testAbstractAlignmentStartJob.startAlignment()
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
        MergingWorkPackage mergingWorkPackage = DomainFactory.createMergingWorkPackage([
                seqType        : DomainFactory.createWholeGenomeSeqType(),
                needsProcessing: true,
                pipeline       : DomainFactory.createPanCanPipeline(),
        ])
        SeqTrack seqTrack = DomainFactory.createSeqTrack(mergingWorkPackage)
        DomainFactory.createMergingCriteriaLazy(project: seqTrack.project, seqType: seqTrack.seqType)
        mergingWorkPackage.seqTracks = [seqTrack]
        mergingWorkPackage.save()
        return mergingWorkPackage
    }

    MergingWorkPackage createMergingWorkPackageWithSeqTrackInState(SeqTrack.DataProcessingState dataInstallationState) {
        MergingWorkPackage mergingWorkPackage = createMergingWorkPackage()
        mergingWorkPackage.seqTracks = [DomainFactory.createSeqTrackWithDataFiles(mergingWorkPackage, [dataInstallationState: dataInstallationState])]
        mergingWorkPackage.save(flush: true)
        return mergingWorkPackage
    }

    private void withJobExecutionPlan(Closure closure) {
        try {
            JobExecutionPlan jep = DomainFactory.createJobExecutionPlan(enabled: true)
            jep.firstJob = DomainFactory.createJobDefinition(plan: jep)
            assert jep.save()
            testAbstractAlignmentStartJob.jep = jep
            closure()
        } finally {
            testAbstractAlignmentStartJob.jep = null
        }
    }
}
