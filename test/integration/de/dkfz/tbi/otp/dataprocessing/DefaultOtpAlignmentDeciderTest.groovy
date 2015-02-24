package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.ngsdata.SeqPlatform
import de.dkfz.tbi.otp.ngsdata.SeqPlatformGroup
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.ngsdata.SeqType
import de.dkfz.tbi.otp.ngsdata.SeqTypeNames
import de.dkfz.tbi.otp.ngsdata.SequencingKit
import de.dkfz.tbi.otp.ngsdata.TestData
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired

public class DefaultOtpAlignmentDeciderTest {
    @Autowired
    DefaultOtpAlignmentDecider defaultOtpAlignmentDecider
    final shouldFail = new GroovyTestCase().&shouldFail

    @Test
    void testCanWorkflowAlign_whenEverythingIsOkay_shouldReturnTrue() {
        TestData testData = new TestData()
        testData.createObjects()

        SeqTrack seqTrack = testData.createSeqTrack()
        seqTrack.save(failOnError: true)

        assert defaultOtpAlignmentDecider.canWorkflowAlign(seqTrack)
    }

    @Test
    void testCanWorkflowAlign_whenWrongSeqType_shouldReturnFalse() {
        TestData testData = new TestData()
        testData.createObjects()

        SeqType seqType = SeqType.build(
                name: SeqTypeNames.WHOLE_GENOME,
                libraryLayout: SeqType.LIBRARYLAYOUT_MATE_PAIRED
        )
        seqType.save(failOnError: true)

        SeqTrack seqTrack = testData.createSeqTrack(seqType: seqType)
        seqTrack.save(failOnError: true)

        assert !defaultOtpAlignmentDecider.canWorkflowAlign(seqTrack)
    }





    @Test
    void testPrepareForAlignment_whenEverythingIsOkayAndForceRealignIsFalse_shouldCreateAlignmentPass() {
        testPrepareForAlignment_whenEverythingIsOkay_shouldCreateAlignmentPass(false)
    }

    @Test
    void testPrepareForAlignment_whenEverythingIsOkayAndForceRealignIsTrue_shouldCreateAlignmentPass() {
        testPrepareForAlignment_whenEverythingIsOkay_shouldCreateAlignmentPass(true)
    }

    private void testPrepareForAlignment_whenEverythingIsOkay_shouldCreateAlignmentPass(boolean forceRealign) {
        TestData testData = new TestData()
        testData.createObjects()

        SequencingKit sequencingKit = SequencingKit.build(name: "V1")
        SeqPlatformGroup seqPlatformGroup = SeqPlatformGroup.build(name: "HiSeq 2000/2500")
        SeqPlatform seqPlatform = SeqPlatform.build(seqPlatformGroup: seqPlatformGroup)
        SeqTrack seqTrack = SeqTrack.build(sequencingKit: sequencingKit, seqPlatform: seqPlatform)

        MergingWorkPackage workPackage = testData.createMergingWorkPackage(sample: seqTrack.sample, seqType: seqTrack.seqType, seqPlatformGroup: seqPlatformGroup)
        workPackage.save(failOnError: true)

        defaultOtpAlignmentDecider.prepareForAlignment(workPackage, seqTrack, forceRealign)

        List<AlignmentPass> alignmentPasses = AlignmentPass.findAllByWorkPackageAndSeqTrack(workPackage, seqTrack)

        assert alignmentPasses.size() == 1
        assert alignmentPasses.first().alignmentState == AlignmentPass.AlignmentState.NOT_STARTED
    }

    @Test
    void testPrepareForAlignment_whenAlignmentPassExists_shouldNotCreateAlignmentPass() {
        TestData testData = new TestData()
        testData.createObjects()

        SequencingKit sequencingKit = SequencingKit.build(name: "V1")
        SeqPlatformGroup seqPlatformGroup = SeqPlatformGroup.build(name: "HiSeq 2000/2500")
        SeqPlatform seqPlatform = SeqPlatform.build(seqPlatformGroup: seqPlatformGroup)
        SeqTrack seqTrack = SeqTrack.build(sequencingKit: sequencingKit, seqPlatform: seqPlatform)

        MergingWorkPackage workPackage = testData.createMergingWorkPackage(sample: seqTrack.sample, seqType: seqTrack.seqType, seqPlatformGroup: seqPlatformGroup)
        workPackage.save(failOnError: true)

        AlignmentPass alignmentPass1 = testData.createAlignmentPass(seqTrack: seqTrack, workPackage: workPackage)
        alignmentPass1.save(failOnError: true)

        defaultOtpAlignmentDecider.prepareForAlignment(workPackage, seqTrack, false)

        List<AlignmentPass> alignmentPasses = AlignmentPass.findAllByWorkPackageAndSeqTrack(workPackage, seqTrack)

        assert alignmentPasses.size() == 1
        assert alignmentPasses.first().id == alignmentPass1.id
    }

    @Test
    void testPrepareForAlignment_whenForceRealign_shouldCreateAnotherAlignmentPass() {
        TestData testData = new TestData()
        testData.createObjects()

        SequencingKit sequencingKit = SequencingKit.build(name: "V1")
        SeqPlatformGroup seqPlatformGroup = SeqPlatformGroup.build(name: "HiSeq 2000/2500")
        SeqPlatform seqPlatform = SeqPlatform.build(seqPlatformGroup: seqPlatformGroup)
        SeqTrack seqTrack = SeqTrack.build(sequencingKit: sequencingKit, seqPlatform: seqPlatform)

        MergingWorkPackage workPackage = testData.createMergingWorkPackage(sample: seqTrack.sample, seqType: seqTrack.seqType, seqPlatformGroup: seqPlatformGroup)
        workPackage.save(failOnError: true)

        AlignmentPass alignmentPass1 = testData.createAlignmentPass(seqTrack: seqTrack, workPackage: workPackage)
        alignmentPass1.save(failOnError: true)

        defaultOtpAlignmentDecider.prepareForAlignment(workPackage, seqTrack, true)

        List<AlignmentPass> alignmentPasses = AlignmentPass.findAllByWorkPackageAndSeqTrack(workPackage, seqTrack)

        assert alignmentPasses.size() == 2
        assert alignmentPasses.last().alignmentState == AlignmentPass.AlignmentState.NOT_STARTED
    }
}
