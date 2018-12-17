package de.dkfz.tbi.otp.ngsdata

import grails.test.mixin.TestFor
import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin
import org.junit.Test

import de.dkfz.tbi.TestCase

/**
 * See the API for {@link grails.test.mixin.support.GrailsUnitTestMixin} for usage instructions
 */
@TestMixin(GrailsUnitTestMixin)
@TestFor(SampleType)
class SampleTypeUnitTests {



    @Test
    void testValidate_valid() {
        SampleType sampleType = new SampleType(
            name: 'sampleType'
        )
        assert sampleType.validate()
    }

    @Test
    void testValidate_invalid_NameIsNull() {
        SampleType sampleType = new SampleType(
            name: null,
        )
        TestCase.assertValidateError(sampleType, 'name', 'nullable', null)
    }

    @Test
    void testValidate_invalid_SpecificReferenceGenomeIsNull() {
        SampleType sampleType = new SampleType(
            name: 'sampleType',
            specificReferenceGenome: null,
        )
        TestCase.assertValidateError(sampleType, 'specificReferenceGenome', 'nullable', null)
    }

}
