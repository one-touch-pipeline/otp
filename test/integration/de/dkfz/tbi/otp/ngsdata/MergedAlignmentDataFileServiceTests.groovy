package de.dkfz.tbi.otp.ngsdata

import static org.junit.Assert.*
import org.junit.*

class MergedAlignmentDataFileServiceTests {

    MergedAlignmentDataFileService mergedAlignmentDataFileService

    @Before
    void setUp() {
        // Setup logic here
    }

    @After
    void tearDown() {
        // Tear down logic here
    }

    @Test
    void testAlignmentSequenceFiles() {
        List<SeqScan> scans = SeqScan.findAll([max: 10])
        for (SeqScan scan in scans) {
            List<DataFile> files = mergedAlignmentDataFileService.alignmentSequenceFiles(scan)
        }
    }

    @Test
    void testScansWithSingleLane() {

        SeqType type = SeqType.findByNameAndLibraryLayout("RNA", "PAIRED")
        List<SeqScan> scans = SeqScan.findAllBySeqType(type)
        for (SeqScan scan in scans) {
            List<DataFile> files = mergedAlignmentDataFileService.alignmentSequenceFiles(scan)
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
