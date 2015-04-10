package de.dkfz.tbi.otp.dataprocessing

import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired

import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.ngsdata.SeqType
import de.dkfz.tbi.otp.ngsdata.SeqTypeNames
import de.dkfz.tbi.otp.ngsdata.TestData

public class DefaultOtpAlignmentDeciderTest {
    @Autowired
    DefaultOtpAlignmentDecider defaultOtpAlignmentDecider
    final shouldFail = new GroovyTestCase().&shouldFail

    @Test
    void testPrepareForAlignment_whenEverythingIsOkayAndForceRealignIsFalse_shouldCreateAlignmentPass() {
        testPrepareForAlignment_whenEverythingIsOkay_shouldCreateAlignmentPass(false)
    }

    @Test
    void testPrepareForAlignment_whenEverythingIsOkayAndForceRealignIsTrue_shouldCreateAlignmentPass() {
        testPrepareForAlignment_whenEverythingIsOkay_shouldCreateAlignmentPass(true)
    }

    private void testPrepareForAlignment_whenEverythingIsOkay_shouldCreateAlignmentPass(boolean forceRealign) {
        SeqTrack seqTrack = SeqTrack.build()

        MergingWorkPackage workPackage = TestData.createMergingWorkPackage(sample: seqTrack.sample, seqType: seqTrack.seqType, seqPlatformGroup: seqTrack.seqPlatformGroup)
        workPackage.save(failOnError: true)

        defaultOtpAlignmentDecider.prepareForAlignment(workPackage, seqTrack, forceRealign)

        List<AlignmentPass> alignmentPasses = AlignmentPass.findAllByWorkPackageAndSeqTrack(workPackage, seqTrack)

        assert alignmentPasses.size() == 1
        assert alignmentPasses.first().alignmentState == AlignmentPass.AlignmentState.NOT_STARTED
    }

    @Test
    void testPrepareForAlignment_whenAlignmentPassExists_shouldNotCreateAlignmentPass() {
        SeqTrack seqTrack = SeqTrack.build()

        MergingWorkPackage workPackage = TestData.createMergingWorkPackage(sample: seqTrack.sample, seqType: seqTrack.seqType, seqPlatformGroup: seqTrack.seqPlatformGroup)
        workPackage.save(failOnError: true)

        AlignmentPass alignmentPass1 = TestData.createAndSaveAlignmentPass(seqTrack: seqTrack, workPackage: workPackage)

        defaultOtpAlignmentDecider.prepareForAlignment(workPackage, seqTrack, false)

        List<AlignmentPass> alignmentPasses = AlignmentPass.findAllByWorkPackageAndSeqTrack(workPackage, seqTrack)

        assert alignmentPasses.size() == 1
        assert alignmentPasses.first().id == alignmentPass1.id
    }

    @Test
    void testPrepareForAlignment_whenForceRealign_shouldCreateAnotherAlignmentPass() {
        SeqTrack seqTrack = SeqTrack.build()

        MergingWorkPackage workPackage = TestData.createMergingWorkPackage(sample: seqTrack.sample, seqType: seqTrack.seqType, seqPlatformGroup: seqTrack.seqPlatformGroup)
        workPackage.save(failOnError: true)

        AlignmentPass alignmentPass1 = TestData.createAndSaveAlignmentPass(seqTrack: seqTrack, workPackage: workPackage)

        defaultOtpAlignmentDecider.prepareForAlignment(workPackage, seqTrack, true)

        List<AlignmentPass> alignmentPasses = AlignmentPass.findAllByWorkPackageAndSeqTrack(workPackage, seqTrack)

        assert alignmentPasses.size() == 2
        assert alignmentPasses.last().alignmentState == AlignmentPass.AlignmentState.NOT_STARTED
    }
}
