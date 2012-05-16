package de.dkfz.tbi.otp.ngsdata

import static org.junit.Assert.*
import org.junit.*

class MergedAlignmentDataFileServiceTests {

    def mergedAlignmentDataFileService

    @Before
    void setUp() {
        // Setup logic here
    }

    @After
    void tearDown() {
        // Tear down logic here
    }

    @Test
    void testSomething() {
        List<SeqScan> scans = SeqScan.findAll([max: 10])
        for(SeqScan scan in scans) {
            List<DataFile> files = mergedAlignmentDataFileService.alignmentSequenceFiles(scan)
            println files
        }
    }

    @Test
    void testScansWithSingleLane() {

        SeqType type = SeqType.findByNameAndLibraryLayout("RNA", "PAIRED")
        List<SeqScan> scans = SeqScan.findAllBySeqType(type)
        for(SeqScan scan in scans) {
            List<DataFile> files = mergedAlignmentDataFileService.alignmentSequenceFiles(scan)
            println "${scan}: ${files}"
            //if (files.size() > 1) {
            //    println files
            //}
        }
    }
}
