package de.dkfz.tbi.otp.dataprocessing

import grails.test.mixin.TestFor
import grails.test.mixin.TestMixin
import grails.test.mixin.web.ControllerUnitTestMixin
import org.junit.Test

import de.dkfz.tbi.otp.ngsdata.ReferenceGenome

import static de.dkfz.tbi.otp.ngsdata.TestData.createReferenceGenome

/**
 * See the API for {@link grails.test.mixin.support.GrailsUnitTestMixin} for usage instructions
 */
@TestFor(ReferenceGenome)
@TestMixin(ControllerUnitTestMixin)
class ReferenceGenomeTests {

    final static Long ARBITRARY_REFERENCE_GENOME_LENGTH = 100

    @Test
    void testValidationMethodOfReferenceGenomeNameNotUnique() {
        final String REFERENCE_GENOME_NAME = 'hg19_1_24'
        ReferenceGenome referenceGenome = createReferenceGenome([name: REFERENCE_GENOME_NAME])
        assert referenceGenome.save(flush: true)

        ReferenceGenome referenceGenomeInvalid = createReferenceGenome([name: REFERENCE_GENOME_NAME])
        assert !referenceGenomeInvalid.validate()
    }

    @Test
    void testValidationMethodOfReferenceGenomeNameIsEmpty() {
        ReferenceGenome referenceGenome = createReferenceGenome([name: ''])
        assert !referenceGenome.validate()
    }

    @Test
    void testValidationMethodOfReferenceGenomePathNotUnique() {
        final String REFERENCE_GENOME_PATH = 'referenceGenome'
        ReferenceGenome referenceGenome = createReferenceGenome([path: REFERENCE_GENOME_PATH])
        assert referenceGenome.save(flush: true)

        ReferenceGenome referenceGenomeInvalid = createReferenceGenome([path: REFERENCE_GENOME_PATH])
        assert !referenceGenomeInvalid.validate()
    }

    @Test
    void testValidationMethodOfReferenceGenomePathIsEmpty() {
        ReferenceGenome referenceGenome = createReferenceGenome([path: ''])
        assert !referenceGenome.validate()
    }

    @Test
    void testValidationMethodOfReferenceGenomePrefixIsEmpty() {
        ReferenceGenome referenceGenome = createReferenceGenome([fileNamePrefix: ''])
        assert !referenceGenome.validate()
    }

    // Test constraints on property "length"

    @Test
    void test_ConstraintOnLength_WhenNegative_ShouldFail() {
        ReferenceGenome referenceGenome = createReferenceGenome([length: -5])
        assert !referenceGenome.validate()
    }

    @Test
    void test_ConstraintOnLength_WhenZero_ShouldFail() {
        ReferenceGenome referenceGenome = createReferenceGenome([length: 0])
        assert !referenceGenome.validate()
    }

    @Test
    void test_ConstraintOnLength_WhenPositive_ShouldSucceed() {
        ReferenceGenome referenceGenome = createReferenceGenome([length: 42])
        assert referenceGenome.validate()
    }

    // Test constraints on property "lengthWithoutN"

    @Test
    void test_ConstraintOnLengthWithoutN_WhenNegative_ShouldFail() {
        ReferenceGenome referenceGenome = createReferenceGenome([lengthWithoutN: -5])
        assert !referenceGenome.validate()
    }

    @Test
    void test_ConstraintOnLengthWithoutN_WhenZero_ShouldFail() {
        ReferenceGenome referenceGenome = createReferenceGenome([lengthWithoutN: 0])
        assert !referenceGenome.validate()
    }

    @Test
    void test_ConstraintOnLengthWithoutN_WhenPositive_ShouldSucceed() {
        ReferenceGenome referenceGenome = createReferenceGenome([lengthWithoutN: 42])
        assert referenceGenome.validate()
    }

    // Test constraints on property "lengthRefChromosomes"

    @Test
    void test_ConstraintOnLengthRefChromosomes_WhenNegative_ShouldFail() {
        ReferenceGenome referenceGenome = createReferenceGenome([lengthRefChromosomes: -5])
        assert !referenceGenome.validate()
    }

    @Test
    void test_ConstraintOnLengthRefChromosomes_WhenZero_ShouldFail() {
        ReferenceGenome referenceGenome = createReferenceGenome([lengthRefChromosomes: 0])
        assert !referenceGenome.validate()
    }

    @Test
    void test_ConstraintOnLengthRefChromosomes_WhenPositive_ShouldSucceed() {
        ReferenceGenome referenceGenome = createReferenceGenome([lengthRefChromosomes: 42])
        assert referenceGenome.validate()
    }

    // Test constraints on property "lengthRefChromosomesWithoutN"

    @Test
    void test_ConstraintOnLengthRefChromosomesWithoutN_WhenNegative_ShouldFail() {
        ReferenceGenome referenceGenome = createReferenceGenome([lengthRefChromosomesWithoutN: -5])
        assert !referenceGenome.validate()
    }

    @Test
    void test_ConstraintOnLengthRefChromosomesWithoutN_WhenZero_ShouldFail() {
        ReferenceGenome referenceGenome = createReferenceGenome([lengthRefChromosomesWithoutN: 0])
        assert !referenceGenome.validate()
    }

    @Test
    void test_ConstraintOnLengthRefChromosomesWithoutN_WhenPositive_ShouldSucceed() {
        ReferenceGenome referenceGenome = createReferenceGenome([lengthRefChromosomesWithoutN: 42])
        assert referenceGenome.validate()
    }
}
