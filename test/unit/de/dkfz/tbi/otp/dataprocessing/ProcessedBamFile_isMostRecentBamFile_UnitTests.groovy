package de.dkfz.tbi.otp.dataprocessing

import grails.test.mixin.*

import org.junit.*

import de.dkfz.tbi.otp.ngsdata.*

@TestFor(ProcessedBamFile)
@Mock([AlignmentPass, SeqTrack])
class ProcessedBamFile_isMostRecentBamFile_UnitTests {

    TestData testData


    void setUp() {
        testData = new TestData()
        testData.seqTrack = testData.createSeqTrack()
        assert testData.seqTrack.save(validate: false)
    }


    void tearDown() {
        testData = null
    }


    void testIsMostRecentBamFile_1Pass() {
        //preparation
        AlignmentPass alignmentPass = testData.createAlignmentPass()
        assert alignmentPass.save()
        ProcessedBamFile processedBamFile = testData.createProcessedBamFile([alignmentPass: alignmentPass])
        assert processedBamFile.save()

        //tests
        assert processedBamFile.isMostRecentBamFile()
    }


    void testIsMostRecentBamFile_2Passes() {
        //preparation
        AlignmentPass alignmentPass1 = testData.createAlignmentPass([identifier: 0])
        assert alignmentPass1.save()
        ProcessedBamFile processedBamFile1 = testData.createProcessedBamFile([alignmentPass: alignmentPass1])
        assert processedBamFile1.save()
        AlignmentPass alignmentPass2 = testData.createAlignmentPass([identifier: 1])
        assert alignmentPass2.save()
        ProcessedBamFile processedBamFile2 = testData.createProcessedBamFile([alignmentPass: alignmentPass2])
        assert processedBamFile2.save()

        //tests
        assert !processedBamFile1.isMostRecentBamFile()
        assert processedBamFile2.isMostRecentBamFile()
    }
}
