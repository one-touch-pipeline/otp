package de.dkfz.tbi.otp.ngsdata

import grails.test.mixin.*
import grails.test.mixin.support.*

import org.junit.*

import de.dkfz.tbi.TestCase


@TestMixin(GrailsUnitTestMixin)
@Mock([SampleIdentifier])
class SampleIdentifierUnitTest {

    static private String CORRECT_NAME = 'name'

    private SampleIdentifier sampleIdentifier



    void setUp() {
        sampleIdentifier = new SampleIdentifier(
            name: CORRECT_NAME,
            sample: new Sample())
        assert sampleIdentifier.validate()
    }



    void test_validate_fail_nameIsNull() {
        sampleIdentifier.name = null

        TestCase.assertValidateError(sampleIdentifier, 'name', 'nullable', null)
    }


    void test_validate_fail_sampleIsNull() {
        sampleIdentifier.sample = null

        TestCase.assertValidateError(sampleIdentifier, 'sample', 'nullable', null)
    }


    void test_validate_fail_nameIsEmpty() {
        sampleIdentifier.name = ''

        TestCase.assertValidateError(sampleIdentifier, 'name', 'blank', '')
    }


    void test_validate_fail_nameIsToShortNull() {
        final String name = '12'
        sampleIdentifier.name = name

        TestCase.assertValidateError(sampleIdentifier, 'name', 'minSize.notmet', name)
    }


    void test_validate_fail_nameIsNotUnique() {
        assert sampleIdentifier.save(flush: true)
        SampleIdentifier sampleIdentifier2 = new SampleIdentifier(
            name: CORRECT_NAME,
            sample: new Sample())

        TestCase.assertValidateError(sampleIdentifier2, 'name', 'unique', CORRECT_NAME)
    }

    void test_validate_fail_nameStartsWithSpace() {
        final String name = ' aname'
        sampleIdentifier.name = name

        TestCase.assertValidateError(sampleIdentifier, 'name', 'validator.invalid', name)
    }

    void test_validate_fail_nameEndsWithSpace() {
        final String name = 'aname '
        sampleIdentifier.name = name

        TestCase.assertValidateError(sampleIdentifier, 'name', 'validator.invalid', name)
    }

}
