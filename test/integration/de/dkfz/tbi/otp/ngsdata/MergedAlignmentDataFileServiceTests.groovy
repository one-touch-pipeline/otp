package de.dkfz.tbi.otp.ngsdata

import org.junit.Test

import static org.junit.Assert.assertEquals

class MergedAlignmentDataFileServiceTests {

    MergedAlignmentDataFileService mergedAlignmentDataFileService

    @Test
    void testAlignmentSequenceFiles() {
        List<SeqScan> scans = SeqScan.list([max: 10])
        for (SeqScan scan in scans) {
            mergedAlignmentDataFileService.alignmentSequenceFiles(scan)
        }
    }

    @Test
    void testScansWithSingleLane() {

        SeqType type = SeqType.findByNameAndLibraryLayout("RNA", SeqType.LIBRARYLAYOUT_PAIRED)
        List<SeqScan> scans = SeqScan.findAllBySeqType(type)
        for (SeqScan scan in scans) {
            mergedAlignmentDataFileService.alignmentSequenceFiles(scan)
        }
    }

    @Test
    void testBuildRelativePath() {
        TestData testData = new TestData()
        testData.createObjects()
        SeqType seqType = testData.seqType
        Sample sample = testData.sample

        String expectedPath = "${testData.project.dirName}/sequencing/whole_genome_sequencing/view-by-pid/654321/tumor/paired/merged-alignment/"
        String actualPath = mergedAlignmentDataFileService.buildRelativePath(seqType, sample)

        assertEquals(expectedPath, actualPath)
    }
}
