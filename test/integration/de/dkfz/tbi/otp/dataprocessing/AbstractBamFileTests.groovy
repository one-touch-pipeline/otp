package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.ngsdata.TestData
import org.junit.*

class AbstractBamFileTests {

    TestData data = new TestData()

    @Before
    void setUp() {
        data.createObjects()
    }

    // Test methods

    @Test
    void test_getReferenceGenome_WhenSet_ShouldSucceed() {

        ProcessedBamFile processedBamFile = createProcessedBamFile()

        assert processedBamFile.referenceGenome == data.referenceGenome
    }

    @Test
    void test_getReferenceGenome_WhenNotSet_ShouldFail() {

        ProcessedBamFile processedBamFile = createProcessedBamFile()
        data.referenceGenomeProjectSeqType.delete([flush: true])

        assert processedBamFile.referenceGenome == null
    }

    // Helper methods

    private ProcessedBamFile createProcessedBamFile() {

        AlignmentPass alignmentPass = data.createAlignmentPass()
        alignmentPass.save([flush: true])

        ProcessedBamFile processedBamFile = new ProcessedBamFile([
                type         : AbstractBamFile.BamType.SORTED,
                withdrawn    : false,
                alignmentPass: alignmentPass,
        ])
        assert processedBamFile.save([flush: true])

        return processedBamFile
    }
}
