package de.dkfz.tbi.otp.ngsdata

import grails.test.mixin.*
import grails.test.mixin.support.*

import org.junit.*

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.ngsdata.SampleType.SpecificReferenceGenome

/**
 * See the API for {@link grails.test.mixin.support.GrailsUnitTestMixin} for usage instructions
 */
@TestMixin(GrailsUnitTestMixin)
@TestFor(SampleType)
class SampleTypeUnitTests {



    void testValidate_valid() {
        SampleType sampleType = new SampleType(
            name: 'sampleType'
        )
        assert sampleType.validate()
    }

    void testValidate_invalid_NameIsNull() {
        SampleType sampleType = new SampleType(
            name: null,
        )
        TestCase.assertValidateError(sampleType, 'name', 'nullable', null)
    }

    void testValidate_invalid_SpecificReferenceGenomeIsNull() {
        SampleType sampleType = new SampleType(
            name: 'sampleType',
            specificReferenceGenome: null,
        )
        TestCase.assertValidateError(sampleType, 'specificReferenceGenome', 'nullable', null)
    }

}
