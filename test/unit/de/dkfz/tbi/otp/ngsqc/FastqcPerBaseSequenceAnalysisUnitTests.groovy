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
@Mock(FastqcPerBaseSequenceAnalysis)
class FastqcPerBaseSequenceAnalysisUnitTests {

    FastqcPerBaseSequenceAnalysis fastqcPerBaseSequenceAnalysis

    private final int NEGATIVE_NUMBER = -1

    private final int NUMBER_BIGGER_THEN_100 = 101

    void setUp() {
        fastqcPerBaseSequenceAnalysis = new FastqcPerBaseSequenceAnalysis(
            fastqcProcessedFile: new FastqcProcessedFile()
        )
    }

    void tearDown() {
        fastqcPerBaseSequenceAnalysis = null
    }

    void testValidateValid() {
        assertTrue(fastqcPerBaseSequenceAnalysis.validate())
    }

    void testValidateNoFastqcProcessedFile() {
        fastqcPerBaseSequenceAnalysis.fastqcProcessedFile = null
        assertFalse(fastqcPerBaseSequenceAnalysis.validate())
    }

    void testValidatePercentageOfGCIsTooSmall() {
        fastqcPerBaseSequenceAnalysis.percentageOfGC = NEGATIVE_NUMBER
        assertFalse(fastqcPerBaseSequenceAnalysis.validate())
        assertTrue(fastqcPerBaseSequenceAnalysis.errors.hasFieldErrors("percentageOfGC"))
    }

    void testValidatePercentageOfGCIsTooBig() {
        fastqcPerBaseSequenceAnalysis.percentageOfGC = NUMBER_BIGGER_THEN_100
        assertFalse(fastqcPerBaseSequenceAnalysis.validate())
        assertTrue(fastqcPerBaseSequenceAnalysis.errors.hasFieldErrors("percentageOfGC"))
    }

    void testValidatePercentageOfGCIsNaN() {
        fastqcPerBaseSequenceAnalysis.percentageOfGC = Double.NaN
        assertTrue(fastqcPerBaseSequenceAnalysis.validate())
    }

    void testValidateCountOfNucleotideAIsTooSmall() {
        fastqcPerBaseSequenceAnalysis.countOfNucleotideA = NEGATIVE_NUMBER
        assertFalse(fastqcPerBaseSequenceAnalysis.validate())
        assertTrue(fastqcPerBaseSequenceAnalysis.errors.hasFieldErrors("countOfNucleotideA"))
    }

    void testValidateCountOfNucleotideCIsTooSmall() {
        fastqcPerBaseSequenceAnalysis.countOfNucleotideC = NEGATIVE_NUMBER
        assertFalse(fastqcPerBaseSequenceAnalysis.validate())
        assertTrue(fastqcPerBaseSequenceAnalysis.errors.hasFieldErrors("countOfNucleotideC"))
    }

    void testValidateCountOfNucleotideGIsTooSmall() {
        fastqcPerBaseSequenceAnalysis.countOfNucleotideG = NEGATIVE_NUMBER
        assertFalse(fastqcPerBaseSequenceAnalysis.validate())
        assertTrue(fastqcPerBaseSequenceAnalysis.errors.hasFieldErrors("countOfNucleotideG"))
    }

    void testValidateCountOfNucleotideTIsTooSmall() {
        fastqcPerBaseSequenceAnalysis.countOfNucleotideT = NEGATIVE_NUMBER
        assertFalse(fastqcPerBaseSequenceAnalysis.validate())
        assertTrue(fastqcPerBaseSequenceAnalysis.errors.hasFieldErrors("countOfNucleotideT"))
    }

    void testValidateCountOfNucleotideNIsTooSmall() {
        fastqcPerBaseSequenceAnalysis.countOfNucleotideN = NEGATIVE_NUMBER
        assertFalse(fastqcPerBaseSequenceAnalysis.validate())
        assertTrue(fastqcPerBaseSequenceAnalysis.errors.hasFieldErrors("countOfNucleotideN"))
    }

}
