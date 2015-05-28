package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.ngsdata.SeqTrack
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


    private createAndRunPrepare(boolean bamFileContainsSeqTrack, boolean withdrawn, boolean md5sumNotNull, boolean forceAlign) {
        RoddyBamFile bamFile = DomainFactory.createRoddyBamFile([
                withdrawn: withdrawn,
                md5sum: md5sumNotNull ? DomainFactory.DEFAULT_MD5_SUM : null,
                fileOperationStatus: md5sumNotNull ? FileOperationStatus.PROCESSED : FileOperationStatus.DECLARED,
                fileSize: md5sumNotNull ? 10000 : -1
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
    contains given   withdrawn   md5sum        |
     seq track                                 |
    ---------------------------------------------------------------------
    true            true        null            look at previous bam file*
    true            true        not null        needs processing
    true            false       null            no op
    true            false       not null        no op
    false           true        null            look at previous bam file*
    false           true        not null        needs processing
    false           false       null            needs processing
    false           false       not null        needs processing
         non-existent bam file                  needs processing
    (*if a bam file matches this criteria, the properties of the previous bam file should be used to make the decision)

    */

    @Test
    void testPrepareForAlignment_bamFileContainsSeqTrackWithdrawnTrueMd5sumNull_shouldSetNeedsProcessing() {
        MergingWorkPackage workPackage = createAndRunPrepare(true, true, false, false)
        assert workPackage.needsProcessing
    }

    @Test
    void testPrepareForAlignment_bamFileContainsSeqTrackWithdrawnTrueMd5sumNotNull_shouldSetNeedsProcessing() {
        MergingWorkPackage workPackage = createAndRunPrepare(true, true, true, false)
        assert workPackage.needsProcessing
    }

    @Test
    void testPrepareForAlignment_bamFileContainsSeqTrackWithdrawnFalseMd5sumNull_shouldNotSetNeedsProcessing() {
        MergingWorkPackage workPackage = createAndRunPrepare(true, false, false, false)
        assert !workPackage.needsProcessing
    }

    @Test
    void testPrepareForAlignment_bamFileContainsSeqTrackWithdrawnFalseMd5sumNotNull_shouldNotSetNeedsProcessing() {
        MergingWorkPackage workPackage = createAndRunPrepare(true, false, true, false)
        assert !workPackage.needsProcessing
    }

    @Test
    void testPrepareForAlignment_bamFileDoesntContainSeqTrackWithdrawnTrueMd5sumNull_shouldSetNeedsProcessing() {
        MergingWorkPackage workPackage = createAndRunPrepare(false, true, false, false)
        assert workPackage.needsProcessing
    }

    @Test
    void testPrepareForAlignment_bamFileDoesntContainSeqTrackWithdrawnTrueMd5sumNotNull_shouldSetNeedsProcessing() {
        MergingWorkPackage workPackage = createAndRunPrepare(false, true, true, false)
        assert workPackage.needsProcessing
    }

    @Test
    void testPrepareForAlignment_bamFileDoesntContainSeqTrackWithdrawnFalseMd5sumNull_shouldSetNeedsProcessing() {
        MergingWorkPackage workPackage = createAndRunPrepare(false, false, false, false)
        assert workPackage.needsProcessing
    }

    @Test
    void testPrepareForAlignment_bamFileDoesntContainSeqTrackWithdrawnFalseMd5sumNotNull_shouldSetNeedsProcessing() {
        MergingWorkPackage workPackage = createAndRunPrepare(false, false, true, false)
        assert workPackage.needsProcessing
    }

    @Test
    void testPrepareForAlignment_noBamFileFound_shouldSetNeedsProcessing() {
        SeqTrack seqTrack = SeqTrack.build()

        MergingWorkPackage workPackage = TestData.createMergingWorkPackage(
                sample: seqTrack.sample,
                seqType: seqTrack.seqType,
                seqPlatformGroup: seqTrack.seqPlatformGroup,
                workflow: decider.getWorkflow(),
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
        MergingWorkPackage workPackage = createAndRunPrepare(true, false, false, true)
        assert !workPackage.needsProcessing
    }
}
