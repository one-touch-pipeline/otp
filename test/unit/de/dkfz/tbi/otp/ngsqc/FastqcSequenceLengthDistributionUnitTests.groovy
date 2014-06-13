package de.dkfz.tbi.otp.ngsqc
import grails.buildtestdata.mixin.Build
import grails.test.mixin.*
import grails.test.mixin.support.GrailsUnitTestMixin
import de.dkfz.tbi.TestCase
import org.junit.Test

@TestMixin(GrailsUnitTestMixin)
@Build(FastqcSequenceLengthDistribution)
class FastqcSequenceLengthDistributionUnitTests {


    @Test
    void testValid() {
        FastqcSequenceLengthDistribution fastqcSequenceLengthDistribution = FastqcSequenceLengthDistribution.buildWithoutSave()

        assert fastqcSequenceLengthDistribution.validate()
    }

    @Test
    void testValid_LengthIsNull() {
        FastqcSequenceLengthDistribution fastqcSequenceLengthDistribution = FastqcSequenceLengthDistribution.buildWithoutSave()
        fastqcSequenceLengthDistribution.length = null

        TestCase.assertValidateError fastqcSequenceLengthDistribution, 'length', 'nullable', null
    }

    @Test
    void testValid_LengthIsEmpty() {
        FastqcSequenceLengthDistribution fastqcSequenceLengthDistribution = FastqcSequenceLengthDistribution.buildWithoutSave()
        fastqcSequenceLengthDistribution.length = ''

        TestCase.assertValidateError fastqcSequenceLengthDistribution, 'length', 'blank', ''
    }

    @Test
    void testValid_fastqcProcessedFileIsNull() {
        FastqcSequenceLengthDistribution fastqcSequenceLengthDistribution = FastqcSequenceLengthDistribution.buildWithoutSave()
        fastqcSequenceLengthDistribution.fastqcProcessedFile = null

        TestCase.assertValidateError fastqcSequenceLengthDistribution, 'fastqcProcessedFile', 'nullable', null
    }

    @Test
    void testValid_LengthIsNumber() {
        final String SOME_NUMBER = '5'
        FastqcSequenceLengthDistribution fastqcSequenceLengthDistribution = FastqcSequenceLengthDistribution.buildWithoutSave(length: SOME_NUMBER)

        assert fastqcSequenceLengthDistribution.validate()
    }

    @Test
    void testValid_LengthIsRange() {
        final String SOME_RANGE = '5-6'
        FastqcSequenceLengthDistribution fastqcSequenceLengthDistribution = FastqcSequenceLengthDistribution.buildWithoutSave(length: SOME_RANGE)

        assert fastqcSequenceLengthDistribution.validate()
    }

}
