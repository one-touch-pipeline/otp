package de.dkfz.tbi.otp.dataprocessing

import grails.test.mixin.*

import org.junit.*

import de.dkfz.tbi.otp.ngsdata.*

@TestFor(AlignmentPass)
@Mock([ReferenceGenome, SeqTrack])
class AlignmentPassUnitTests {

    TestData testData


    void setUp() {
        testData = new TestData()
        testData.seqTrack = testData.createSeqTrack()
        assert testData.seqTrack.save(validate: false)
    }


    void tearDown() {
        testData = null
    }


    void testIsLatestPass_1Pass() {
        //preparation
        AlignmentPass alignmentPass = testData.createAlignmentPass()
        assert alignmentPass.save()

        //tests
        assert alignmentPass.isLatestPass()
    }


    void testIsLatestPass_2Passes() {
        //preparation
        AlignmentPass alignmentPass1 = testData.createAlignmentPass([identifier: 0])
        AlignmentPass alignmentPass2 = testData.createAlignmentPass([identifier: 1])
        assert alignmentPass1.save()
        assert alignmentPass2.save()

        //tests
        assert !alignmentPass1.isLatestPass()
        assert alignmentPass2.isLatestPass()
    }
}
