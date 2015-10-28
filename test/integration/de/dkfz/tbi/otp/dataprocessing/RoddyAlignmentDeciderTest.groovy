package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.ngsdata.SeqType
import de.dkfz.tbi.otp.ngsdata.SeqTypeNames
import de.dkfz.tbi.otp.ngsdata.TestData
import de.dkfz.tbi.otp.dataprocessing.AbstractMergedBamFile.FileOperationStatus
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired

public class RoddyAlignmentDeciderTest {
    @Autowired
    RoddyAlignmentDecider decider
    final shouldFail = new GroovyTestCase().&shouldFail

    @Test
    void testGetWorkflow() {
        Workflow wf = decider.getWorkflow()
        assert wf.name == Workflow.Name.PANCAN_ALIGNMENT
        assert wf.type == Workflow.Type.ALIGNMENT
    }


    private createAndRunPrepare(boolean bamFileContainsSeqTrack, boolean withdrawn, FileOperationStatus fileOperationStatus, boolean forceAlign) {
        RoddyBamFile bamFile = DomainFactory.createRoddyBamFile([
                withdrawn: withdrawn,
                md5sum: fileOperationStatus == FileOperationStatus.PROCESSED ? DomainFactory.DEFAULT_MD5_SUM : null,
                fileOperationStatus: fileOperationStatus,
                fileSize: fileOperationStatus == FileOperationStatus.PROCESSED ? 10000 : -1
                ]
        )

        SeqTrack seqTrack = bamFileContainsSeqTrack ?
                bamFile.seqTracks.iterator().next() :
                DomainFactory.buildSeqTrackWithDataFile(bamFile.workPackage)

        assert bamFile.workPackage.satisfiesCriteria(seqTrack)
        assert !bamFile.workPackage.needsProcessing

        decider.prepareForAlignment(bamFile.workPackage, seqTrack, forceAlign)

        return bamFile.workPackage
    }

    /**
    Expected behaviour of RoddyAlignmentDecider.prepareForAlignment():
    properties checked for the latest bam file |  result
    contains given  withdrawn   fileOperationStatus        |   action
     seq track                                             |
    ---------------------------------------------------------------------
    true            true        DECLARED/NEEDS_PROCESSING      look at previous bam file*
    true            true        INPROGRESS/PROCESSED           needs processing
    true            false       DECLARED/NEEDS_PROCESSING      no op
    true            false       INPROGRESS/PROCESSED           no op
    false           true        DECLARED/NEEDS_PROCESSING      look at previous bam file*
    false           true        INPRORGESS/PROCESSED           needs processing
    false           false       DECLARED/NEEDS_PROCESSING      needs processing
    false           false       INPROGRESS/PROCESSED           needs processing
         non-existent bam file                                 needs processing
    (*if a bam file matches this criteria, the properties of the previous bam file should be used to make the decision)

    */

    @Test
    void testPrepareForAlignment_bamFileContainsSeqTrackWithdrawnTrueFileOperationStatusDeclared_shouldSetNeedsProcessing() {
        MergingWorkPackage workPackage = createAndRunPrepare(true, true, FileOperationStatus.DECLARED, false)
        assert workPackage.needsProcessing
    }

    @Test
    void testPrepareForAlignment_bamFileContainsSeqTrackWithdrawnTrueFileOperationStatusNeedsProcessing_shouldSetNeedsProcessing() {
        MergingWorkPackage workPackage = createAndRunPrepare(true, true, FileOperationStatus.NEEDS_PROCESSING, false)
        assert workPackage.needsProcessing
    }

    @Test
    void testPrepareForAlignment_bamFileContainsSeqTrackWithdrawnTrueFileOperationStatusInProgress_shouldSetNeedsProcessing() {
        MergingWorkPackage workPackage = createAndRunPrepare(true, true, FileOperationStatus.INPROGRESS, false)
        assert workPackage.needsProcessing
    }

    @Test
    void testPrepareForAlignment_bamFileContainsSeqTrackWithdrawnTrueFileOperationStatusProcessed_shouldSetNeedsProcessing() {
        MergingWorkPackage workPackage = createAndRunPrepare(true, true, FileOperationStatus.PROCESSED, false)
        assert workPackage.needsProcessing
    }

    @Test
    void testPrepareForAlignment_bamFileContainsSeqTrackWithdrawnFalseFileOperationStatusDeclared_shouldNotSetNeedsProcessing() {
        MergingWorkPackage workPackage = createAndRunPrepare(true, false, FileOperationStatus.DECLARED, false)
        assert !workPackage.needsProcessing
    }

    @Test
    void testPrepareForAlignment_bamFileContainsSeqTrackWithdrawnFalseFileOperationStatusNeedsProcessing_shouldNotSetNeedsProcessing() {
        MergingWorkPackage workPackage = createAndRunPrepare(true, false, FileOperationStatus.NEEDS_PROCESSING, false)
        assert !workPackage.needsProcessing
    }

    @Test
    void testPrepareForAlignment_bamFileContainsSeqTrackWithdrawnFalseFileOperationStatusInProgress_shouldNotSetNeedsProcessing() {
        MergingWorkPackage workPackage = createAndRunPrepare(true, false, FileOperationStatus.INPROGRESS, false)
        assert !workPackage.needsProcessing
    }

    @Test
    void testPrepareForAlignment_bamFileContainsSeqTrackWithdrawnFalseFileOperationStatusProcessed_shouldNotSetNeedsProcessing() {
        MergingWorkPackage workPackage = createAndRunPrepare(true, false, FileOperationStatus.PROCESSED, false)
        assert !workPackage.needsProcessing
    }

    @Test
    void testPrepareForAlignment_bamFileDoesntContainSeqTrackWithdrawnTrueFileOperationStatusDeclared_shouldSetNeedsProcessing() {
        MergingWorkPackage workPackage = createAndRunPrepare(false, true, FileOperationStatus.DECLARED, false)
        assert workPackage.needsProcessing
    }

    @Test
    void testPrepareForAlignment_bamFileDoesntContainSeqTrackWithdrawnTrueFileOperationStatusNeedsProcessing_shouldSetNeedsProcessing() {
        MergingWorkPackage workPackage = createAndRunPrepare(false, true, FileOperationStatus.NEEDS_PROCESSING, false)
        assert workPackage.needsProcessing
    }


    @Test
    void testPrepareForAlignment_bamFileDoesntContainSeqTrackWithdrawnTrueFileOperationStatusInProgress_shouldSetNeedsProcessing() {
        MergingWorkPackage workPackage = createAndRunPrepare(false, true, FileOperationStatus.INPROGRESS, false)
        assert workPackage.needsProcessing
    }

    @Test
    void testPrepareForAlignment_bamFileDoesntContainSeqTrackWithdrawnTrueFileOperationStatusProcessed_shouldSetNeedsProcessing() {
        MergingWorkPackage workPackage = createAndRunPrepare(false, true, FileOperationStatus.PROCESSED, false)
        assert workPackage.needsProcessing
    }

    @Test
    void testPrepareForAlignment_bamFileDoesntContainSeqTrackWithdrawnFalseFileOperationStatusDeclared_shouldSetNeedsProcessing() {
        MergingWorkPackage workPackage = createAndRunPrepare(false, false, FileOperationStatus.DECLARED, false)
        assert workPackage.needsProcessing
    }

    @Test
    void testPrepareForAlignment_bamFileDoesntContainSeqTrackWithdrawnFalseFileOperationStatusNeedsProcessing_shouldSetNeedsProcessing() {
        MergingWorkPackage workPackage = createAndRunPrepare(false, false, FileOperationStatus.NEEDS_PROCESSING, false)
        assert workPackage.needsProcessing
    }

    @Test
    void testPrepareForAlignment_bamFileDoesntContainSeqTrackWithdrawnFalseFileOperationStatusInProgress_shouldSetNeedsProcessing() {
        MergingWorkPackage workPackage = createAndRunPrepare(false, false, FileOperationStatus.INPROGRESS, false)
        assert workPackage.needsProcessing
    }

    @Test
    void testPrepareForAlignment_bamFileDoesntContainSeqTrackWithdrawnFalseFileOperationStatusProcessed_shouldSetNeedsProcessing() {
        MergingWorkPackage workPackage = createAndRunPrepare(false, false, FileOperationStatus.PROCESSED, false)
        assert workPackage.needsProcessing
    }

    @Test
    void testPrepareForAlignment_noBamFileFound_shouldSetNeedsProcessing() {
        SeqTrack seqTrack = SeqTrack.build()

        Workflow workflow = decider.getWorkflow()

        MergingWorkPackage workPackage = TestData.createMergingWorkPackage(
                sample: seqTrack.sample,
                seqType: seqTrack.seqType,
                seqPlatformGroup: seqTrack.seqPlatformGroup,
                workflow: workflow,
                statSizeFileName: workflow.name == Workflow.Name.PANCAN_ALIGNMENT ? DomainFactory.DEFAULT_TAB_FILE_NAME : null
        )
        workPackage.save(failOnError: true)

        decider.prepareForAlignment(workPackage, seqTrack, false)

        assert workPackage.needsProcessing
    }


    void prepareGetLatestExistingValidBamFile_latestShouldBeFound(boolean withdrawn, boolean md5sumNotNull) {
        RoddyBamFile bamFile1 = DomainFactory.createRoddyBamFile(
                withdrawn: false,
        )
        RoddyBamFile bamFile2 = DomainFactory.createRoddyBamFile(
                withdrawn: false,
                identifier: bamFile1.identifier + 1,
                workPackage: bamFile1.workPackage,
                seqTracks: [bamFile1.seqTracks.iterator().next()] as Set<SeqTrack>,
        )
        RoddyBamFile bamFile3 = DomainFactory.createRoddyBamFile([
                withdrawn: withdrawn,
                md5sum: md5sumNotNull ? DomainFactory.DEFAULT_MD5_SUM : null,
                identifier: bamFile1.identifier + 2,
                workPackage: bamFile1.workPackage,
                seqTracks: [bamFile1.seqTracks.iterator().next()] as Set<SeqTrack>,
                fileOperationStatus: md5sumNotNull ? FileOperationStatus.PROCESSED : FileOperationStatus.DECLARED,
                fileSize: md5sumNotNull ? 10000 : -1
                ]
        )

        def bamFileResult = decider.getLatestBamFileWhichHasBeenOrCouldBeCopied(bamFile1.workPackage)

        assert bamFileResult.id == bamFile3.id
    }

    @Test
    void testGetLatestExistingValidBamFile_latestShouldBeFound() {
        prepareGetLatestExistingValidBamFile_latestShouldBeFound(true, true)
        prepareGetLatestExistingValidBamFile_latestShouldBeFound(false, false)
        prepareGetLatestExistingValidBamFile_latestShouldBeFound(false, true)
    }


    void prepareGetLatestExistingValidBamFile_secondShouldBeFound(boolean withdrawn, boolean md5sumNotNull) {
        RoddyBamFile bamFile1 = DomainFactory.createRoddyBamFile(
                withdrawn: false,
        )
        RoddyBamFile bamFile2 = DomainFactory.createRoddyBamFile([
                withdrawn: withdrawn,
                md5sum: md5sumNotNull ? DomainFactory.DEFAULT_MD5_SUM : null,
                identifier: bamFile1.identifier + 1,
                workPackage: bamFile1.workPackage,
                seqTracks: [bamFile1.seqTracks.iterator().next()] as Set<SeqTrack>,
                fileOperationStatus: md5sumNotNull ? FileOperationStatus.PROCESSED : FileOperationStatus.DECLARED,
                fileSize: md5sumNotNull ? 10000 : -1
                ]
        )
        RoddyBamFile bamFile3 = DomainFactory.createRoddyBamFile(
                withdrawn: true,
                md5sum: null,
                fileOperationStatus: FileOperationStatus.DECLARED,
                fileSize: -1,
                identifier: bamFile1.identifier + 2,
                workPackage: bamFile1.workPackage,
                seqTracks: [bamFile1.seqTracks.iterator().next()] as Set<SeqTrack>,
        )

        def bamFileResult = decider.getLatestBamFileWhichHasBeenOrCouldBeCopied(bamFile1.workPackage)

        assert bamFileResult.id == bamFile2.id
    }

    @Test
    void testGetLatestExistingValidBamFile_secondShouldBeFound() {
        prepareGetLatestExistingValidBamFile_secondShouldBeFound(true, true)
        prepareGetLatestExistingValidBamFile_secondShouldBeFound(false, false)
        prepareGetLatestExistingValidBamFile_secondShouldBeFound(false, true)
    }


    @Test
    void prepareGetLatestExistingValidBamFile_nothingShouldBeFound() {
        RoddyBamFile bamFile1 = DomainFactory.createRoddyBamFile(
                withdrawn: true,
                md5sum: null,
                fileOperationStatus: FileOperationStatus.DECLARED,
                fileSize: -1,
        )
        RoddyBamFile bamFile2 = DomainFactory.createRoddyBamFile(
                withdrawn: true,
                md5sum: null,
                fileOperationStatus: FileOperationStatus.DECLARED,
                fileSize: -1,
                identifier: bamFile1.identifier + 1,
                workPackage: bamFile1.workPackage,
                seqTracks: [bamFile1.seqTracks.iterator().next()] as Set<SeqTrack>,
        )
        RoddyBamFile bamFile3 = DomainFactory.createRoddyBamFile(
                withdrawn: true,
                md5sum: null,
                fileOperationStatus: FileOperationStatus.DECLARED,
                fileSize: -1,
                identifier: bamFile1.identifier + 2,
                workPackage: bamFile1.workPackage,
                seqTracks: [bamFile1.seqTracks.iterator().next()] as Set<SeqTrack>,
        )

        def bamFileResult = decider.getLatestBamFileWhichHasBeenOrCouldBeCopied(bamFile1.workPackage)

        assert bamFileResult == null
    }


    @Test
    void testPrepareForAlignment_forceAlignment_shouldNotSetNeedsProcessing() {
        MergingWorkPackage workPackage = createAndRunPrepare(true, false, FileOperationStatus.DECLARED, true)
        assert !workPackage.needsProcessing
    }


    @Test
    void testCanWorkflowAlign_whenEverythingIsOkay_shouldReturnTrue() {
        TestData testData = new TestData()
        testData.createObjects()

        SeqTrack seqTrack = testData.createSeqTrack()
        seqTrack.save(failOnError: true)

        assert decider.canWorkflowAlign(seqTrack)
    }

    @Test
    void testCanWorkflowAlign_whenWrongLibraryLayout_shouldReturnFalse() {
        TestData testData = new TestData()
        testData.createObjects()

        SeqType seqType = SeqType.build(
                name: SeqTypeNames.WHOLE_GENOME,
                libraryLayout: SeqType.LIBRARYLAYOUT_MATE_PAIR
        )
        seqType.save(failOnError: true)

        SeqTrack seqTrack = testData.createSeqTrack(seqType: seqType)
        seqTrack.save(failOnError: true)

        assert !decider.canWorkflowAlign(seqTrack)
    }

    @Test
    void testCanWorkflowAlign_whenWrongSeqType_shouldReturnFalse() {
        TestData testData = new TestData()
        testData.createObjects()

        SeqType seqType = SeqType.build(
                name: SeqTypeNames.EXOME,
                libraryLayout: SeqType.LIBRARYLAYOUT_PAIRED
        )
        seqType.save(failOnError: true)

        SeqTrack seqTrack = testData.createSeqTrack(seqType: seqType)
        seqTrack.save(failOnError: true)

        assert !decider.canWorkflowAlign(seqTrack)
    }
}
