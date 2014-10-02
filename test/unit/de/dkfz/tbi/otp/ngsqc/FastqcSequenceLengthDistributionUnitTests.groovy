package de.dkfz.tbi.otp.ngsqc
import grails.buildtestdata.mixin.Build
import grails.test.mixin.*
import grails.test.mixin.support.GrailsUnitTestMixin
import de.dkfz.tbi.TestCase

@TestMixin(GrailsUnitTestMixin)
@Build(FastqcSequenceLengthDistribution)
class FastqcSequenceLengthDistributionUnitTests {


    void testValid() {
        FastqcSequenceLengthDistribution fastqcSequenceLengthDistribution = FastqcSequenceLengthDistribution.buildWithoutSave()

        assert fastqcSequenceLengthDistribution.validate()
    }

    void testValid_LengthIsNull() {
        FastqcSequenceLengthDistribution fastqcSequenceLengthDistribution = FastqcSequenceLengthDistribution.buildWithoutSave()
        fastqcSequenceLengthDistribution.length = null

        TestCase.assertValidateError fastqcSequenceLengthDistribution, 'length', 'nullable', null
    }

    void testValid_LengthIsEmpty() {
        FastqcSequenceLengthDistribution fastqcSequenceLengthDistribution = FastqcSequenceLengthDistribution.buildWithoutSave()
        fastqcSequenceLengthDistribution.length = ''

        TestCase.assertValidateError fastqcSequenceLengthDistribution, 'length', 'blank', ''
    }

    void testValid_fastqcProcessedFileIsNull() {
        FastqcSequenceLengthDistribution fastqcSequenceLengthDistribution = FastqcSequenceLengthDistribution.buildWithoutSave()
        fastqcSequenceLengthDistribution.fastqcProcessedFile = null

        TestCase.assertValidateError fastqcSequenceLengthDistribution, 'fastqcProcessedFile', 'nullable', null
    }

    void testValid_LengthIsNumber() {
        final String SOME_NUMBER = '5'
        FastqcSequenceLengthDistribution fastqcSequenceLengthDistribution = FastqcSequenceLengthDistribution.buildWithoutSave(length: SOME_NUMBER)

        assert fastqcSequenceLengthDistribution.validate()
    }

    void testValid_LengthIsRange() {
        final String SOME_RANGE = '5-6'
        FastqcSequenceLengthDistribution fastqcSequenceLengthDistribution = FastqcSequenceLengthDistribution.buildWithoutSave(length: SOME_RANGE)

        assert fastqcSequenceLengthDistribution.validate()
    }

}
