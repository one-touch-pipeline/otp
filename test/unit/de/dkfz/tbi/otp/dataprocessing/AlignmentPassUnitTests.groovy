package de.dkfz.tbi.otp.dataprocessing

import org.junit.*

import grails.buildtestdata.mixin.Build
import grails.test.mixin.*

import de.dkfz.tbi.otp.ngsdata.*

@TestFor(AlignmentPass)
@Build([MergingCriteria, MergingWorkPackage, SeqTrack,])
class AlignmentPassUnitTests {

    @Test
    void testIsLatestPass_1Pass() {
        //preparation
        AlignmentPass alignmentPass = DomainFactory.createAlignmentPass()

        //tests
        assert alignmentPass.isLatestPass()
    }


    @Test
    void testIsLatestPass_2Passes() {
        //preparation
        SeqTrack seqTrack = DomainFactory.createSeqTrack()
        AlignmentPass alignmentPass1 = DomainFactory.createAlignmentPass(seqTrack: seqTrack, identifier: 0)
        AlignmentPass alignmentPass2 = DomainFactory.createAlignmentPass(seqTrack: seqTrack, identifier: 1)

        //tests
        assert !alignmentPass1.isLatestPass()
        assert alignmentPass2.isLatestPass()
    }

    @Ignore  // TODO OTP-1401: Un-ignore this test as soon as the constraints on MergingWorkPackage allow multiple
             // instances for the same Sample and SeqType.
    @Test
    void testIsLatestPass_2PassesDifferentWorkPackages() {
        //preparation
        SeqTrack seqTrack = DomainFactory.createSeqTrack()
        AlignmentPass alignmentPass1 = DomainFactory.createAlignmentPass(seqTrack: seqTrack, identifier: 0,
                referenceGenome: ReferenceGenome.build())
        AlignmentPass alignmentPass2 = DomainFactory.createAlignmentPass(seqTrack: seqTrack, identifier: 1,
                referenceGenome: ReferenceGenome.build())

        //tests
        assert alignmentPass1.isLatestPass()
        assert alignmentPass2.isLatestPass()
    }
}
