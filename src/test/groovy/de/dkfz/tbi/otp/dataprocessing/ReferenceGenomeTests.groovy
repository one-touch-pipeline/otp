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

import grails.testing.gorm.DomainUnitTest
import spock.lang.Specification

import de.dkfz.tbi.otp.ngsdata.ReferenceGenome

import static de.dkfz.tbi.otp.ngsdata.TestData.createReferenceGenome

class ReferenceGenomeTests extends Specification implements DomainUnitTest<ReferenceGenome> {

    final static Long ARBITRARY_REFERENCE_GENOME_LENGTH = 100

    void testValidationMethodOfReferenceGenomeNameNotUnique() {
        when:
        final String REFERENCE_GENOME_NAME = 'hg19_1_24'
        ReferenceGenome referenceGenome = createReferenceGenome([name: REFERENCE_GENOME_NAME])
        ReferenceGenome referenceGenomeInvalid = createReferenceGenome([name: REFERENCE_GENOME_NAME])

        then:
        referenceGenome.save(flush: true)
        !referenceGenomeInvalid.validate()
    }

    void testValidationMethodOfReferenceGenomeNameIsEmpty() {
        when:
        ReferenceGenome referenceGenome = createReferenceGenome([name: ''])

        then:
        !referenceGenome.validate()
    }

    void testValidationMethodOfReferenceGenomePathNotUnique() {
        when:
        final String REFERENCE_GENOME_PATH = 'referenceGenome'
        ReferenceGenome referenceGenome = createReferenceGenome([path: REFERENCE_GENOME_PATH])
        ReferenceGenome referenceGenomeInvalid = createReferenceGenome([path: REFERENCE_GENOME_PATH])

        then:
        referenceGenome.save(flush: true)
        !referenceGenomeInvalid.validate()
    }

    void testValidationMethodOfReferenceGenomePathIsEmpty() {
        when:
        ReferenceGenome referenceGenome = createReferenceGenome([path: ''])

        then:
        !referenceGenome.validate()
    }

    void testValidationMethodOfReferenceGenomePrefixIsEmpty() {
        when:
        ReferenceGenome referenceGenome = createReferenceGenome([fileNamePrefix: ''])

        then:
        !referenceGenome.validate()
    }

    // Test constraints on property "length"

    void test_ConstraintOnLength_WhenNegative_ShouldFail() {
        when:
        ReferenceGenome referenceGenome = createReferenceGenome([length: -5])

        then:
        !referenceGenome.validate()
    }

    void test_ConstraintOnLength_WhenZero_ShouldFail() {
        when:
        ReferenceGenome referenceGenome = createReferenceGenome([length: 0])

        then:
        !referenceGenome.validate()
    }

    void test_ConstraintOnLength_WhenPositive_ShouldSucceed() {
        when:
        ReferenceGenome referenceGenome = createReferenceGenome([length: 42])

        then:
        referenceGenome.validate()
    }

    // Test constraints on property "lengthWithoutN"

    void test_ConstraintOnLengthWithoutN_WhenNegative_ShouldFail() {
        when:
        ReferenceGenome referenceGenome = createReferenceGenome([lengthWithoutN: -5])

        then:
        !referenceGenome.validate()
    }

    void test_ConstraintOnLengthWithoutN_WhenZero_ShouldFail() {
        when:
        ReferenceGenome referenceGenome = createReferenceGenome([lengthWithoutN: 0])

        then:
        !referenceGenome.validate()
    }

    void test_ConstraintOnLengthWithoutN_WhenPositive_ShouldSucceed() {
        when:
        ReferenceGenome referenceGenome = createReferenceGenome([lengthWithoutN: 42])

        then:
        referenceGenome.validate()
    }

    // Test constraints on property "lengthRefChromosomes"

    void test_ConstraintOnLengthRefChromosomes_WhenNegative_ShouldFail() {
        when:
        ReferenceGenome referenceGenome = createReferenceGenome([lengthRefChromosomes: -5])

        then:
        !referenceGenome.validate()
    }

    void test_ConstraintOnLengthRefChromosomes_WhenZero_ShouldFail() {
        when:
        ReferenceGenome referenceGenome = createReferenceGenome([lengthRefChromosomes: 0])

        then:
        !referenceGenome.validate()
    }

    void test_ConstraintOnLengthRefChromosomes_WhenPositive_ShouldSucceed() {
        when:
        ReferenceGenome referenceGenome = createReferenceGenome([lengthRefChromosomes: 42])

        then:
        referenceGenome.validate()
    }

    // Test constraints on property "lengthRefChromosomesWithoutN"

    void test_ConstraintOnLengthRefChromosomesWithoutN_WhenNegative_ShouldFail() {
        when:
        ReferenceGenome referenceGenome = createReferenceGenome([lengthRefChromosomesWithoutN: -5])

        then:
        !referenceGenome.validate()
    }

    void test_ConstraintOnLengthRefChromosomesWithoutN_WhenZero_ShouldFail() {
        when:
        ReferenceGenome referenceGenome = createReferenceGenome([lengthRefChromosomesWithoutN: 0])

        then:
        !referenceGenome.validate()
    }

    void test_ConstraintOnLengthRefChromosomesWithoutN_WhenPositive_ShouldSucceed() {
        when:
        ReferenceGenome referenceGenome = createReferenceGenome([lengthRefChromosomesWithoutN: 42])

        then:
        referenceGenome.validate()
    }
}
