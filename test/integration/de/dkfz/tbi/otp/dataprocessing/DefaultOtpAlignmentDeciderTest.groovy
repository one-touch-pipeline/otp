package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.ngsdata.*
import org.junit.*
import org.springframework.beans.factory.annotation.*

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
        SeqTrack seqTrack = DomainFactory.createSeqTrack()
        DomainFactory.createMergingCriteriaLazy(project: seqTrack.project, seqType: seqTrack.seqType)
        MergingWorkPackage workPackage = TestData.createMergingWorkPackage(MergingWorkPackage.getMergingProperties(seqTrack))
        workPackage.save(failOnError: true)

        defaultOtpAlignmentDecider.prepareForAlignment(workPackage, seqTrack, forceRealign)

        List<AlignmentPass> alignmentPasses = AlignmentPass.findAllByWorkPackageAndSeqTrack(workPackage, seqTrack)

        assert alignmentPasses.size() == 1
        assert alignmentPasses.first().alignmentState == AlignmentPass.AlignmentState.NOT_STARTED
    }

    @Test
    void testPrepareForAlignment_whenAlignmentPassExists_shouldNotCreateAlignmentPass() {
        SeqTrack seqTrack = DomainFactory.createSeqTrack()
        DomainFactory.createMergingCriteriaLazy(project: seqTrack.project, seqType: seqTrack.seqType)

        MergingWorkPackage workPackage = TestData.createMergingWorkPackage(MergingWorkPackage.getMergingProperties(seqTrack))
        workPackage.save(failOnError: true)

        AlignmentPass alignmentPass1 = DomainFactory.createAlignmentPass(seqTrack: seqTrack, workPackage: workPackage)

        defaultOtpAlignmentDecider.prepareForAlignment(workPackage, seqTrack, false)

        List<AlignmentPass> alignmentPasses = AlignmentPass.findAllByWorkPackageAndSeqTrack(workPackage, seqTrack)

        assert alignmentPasses.size() == 1
        assert alignmentPasses.first().id == alignmentPass1.id
    }

    @Test
    void testPrepareForAlignment_whenForceRealign_shouldCreateAnotherAlignmentPass() {
        SeqTrack seqTrack = DomainFactory.createSeqTrack()
        DomainFactory.createMergingCriteriaLazy(project: seqTrack.project, seqType: seqTrack.seqType)

        MergingWorkPackage workPackage = TestData.createMergingWorkPackage(MergingWorkPackage.getMergingProperties(seqTrack))
        workPackage.save(failOnError: true)

        AlignmentPass alignmentPass1 = DomainFactory.createAlignmentPass(seqTrack: seqTrack, workPackage: workPackage)

        defaultOtpAlignmentDecider.prepareForAlignment(workPackage, seqTrack, true)

        List<AlignmentPass> alignmentPasses = AlignmentPass.findAllByWorkPackageAndSeqTrack(workPackage, seqTrack)

        assert alignmentPasses.size() == 2
        assert alignmentPasses.last().alignmentState == AlignmentPass.AlignmentState.NOT_STARTED
    }
}
