package de.dkfz.tbi.otp.ngsqc

import static org.junit.Assert.*
import grails.test.mixin.*
import grails.test.mixin.support.*
import org.junit.*
import de.dkfz.tbi.otp.dataprocessing.FastqcProcessedFile

/**
 * See the API for {@link grails.test.mixin.support.GrailsUnitTestMixin} for usage instructions
 */
@TestMixin(GrailsUnitTestMixin)
@Mock([FastqcProcessedFile])
class FastqcBasicStatisticsTests {


    void testFileType() {
        FastqcBasicStatistics fastqcBasicStatistics = createFastqcBasicStatistics()
        assertTrue(fastqcBasicStatistics.validate())

        fastqcBasicStatistics.fileType = ""
        assertFalse(fastqcBasicStatistics.validate())
    }

    void testEncoding() {
        FastqcBasicStatistics fastqcBasicStatistics = createFastqcBasicStatistics()
        assertTrue(fastqcBasicStatistics.validate())

        fastqcBasicStatistics.encoding = ""
        assertFalse(fastqcBasicStatistics.validate())
    }

    void testSequenceLength() {
        FastqcBasicStatistics fastqcBasicStatistics = createFastqcBasicStatistics()
        assertTrue(fastqcBasicStatistics.validate())

        fastqcBasicStatistics.sequenceLength = ""
        assertFalse(fastqcBasicStatistics.validate())
    }

    FastqcBasicStatistics createFastqcBasicStatistics() {
        return new FastqcBasicStatistics(
        fileType: "Conventional base calls",
        encoding: "Sanger / Illumina 1.9",
        totalSequences: 51718846,
        filteredSequences: 0,
        sequenceLength: "101",
        totalDuplicatePercentage: 6.113009854060126,
        fastqcProcessedFile: new FastqcProcessedFile()
        )
    }
}
