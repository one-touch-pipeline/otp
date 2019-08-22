/*
 * Copyright 2011-2019 The OTP authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
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
