package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import grails.buildtestdata.mixin.Build
import grails.test.mixin.*
import grails.test.mixin.support.*

import org.junit.*

import de.dkfz.tbi.TestCase


@TestMixin(GrailsUnitTestMixin)
@Build([ProcessingOption, SampleIdentifier])
class SampleIdentifierUnitTest {

    static private String CORRECT_NAME = 'name'

    private SampleIdentifier sampleIdentifier



    @Before
    void setUp() {
        sampleIdentifier = new SampleIdentifier(
            name: CORRECT_NAME,
            sample: Sample.build())
        assert sampleIdentifier.validate()
    }



    @Test
    void test_validate_fail_nameIsNull() {
        sampleIdentifier.name = null

        TestCase.assertValidateError(sampleIdentifier, 'name', 'nullable', null)
    }


    @Test
    void test_validate_fail_sampleIsNull() {
        sampleIdentifier.sample = null

        TestCase.assertValidateError(sampleIdentifier, 'sample', 'nullable', null)
    }


    @Test
    void test_validate_fail_nameIsEmpty() {
        sampleIdentifier.name = ''

        TestCase.assertValidateError(sampleIdentifier, 'name', 'blank', '')
    }


    @Test
    void test_validate_fail_nameIsTooShort() {
        final String name = '12'
        sampleIdentifier.name = name

        TestCase.assertValidateError(sampleIdentifier, 'name', 'minSize.notmet', name)
    }


    @Test
    void test_validate_fail_nameIsNotUnique() {
        assert sampleIdentifier.save(flush: true)
        SampleIdentifier sampleIdentifier2 = new SampleIdentifier(
            name: CORRECT_NAME,
            sample: Sample.build())

        TestCase.assertValidateError(sampleIdentifier2, 'name', 'unique', CORRECT_NAME)
    }

    @Test
    void test_validate_fail_nameStartsWithSpace() {
        final String name = ' aname'
        sampleIdentifier.name = name

        TestCase.assertValidateError(sampleIdentifier, 'name', 'validator.invalid', name)
    }

    @Test
    void test_validate_fail_nameEndsWithSpace() {
        final String name = 'aname '
        sampleIdentifier.name = name

        TestCase.assertValidateError(sampleIdentifier, 'name', 'validator.invalid', name)
    }

    private void createRegex(final Project project) {
        assert new ProcessingOption(
                name: SampleIdentifier.REGEX_OPTION_NAME,
                project: project,
                value: '[a-z]{4}',
                comment: '',
        ).save(failOnError: true)
    }

    @Test
    void test_validate_pass_regexIsSet() {
        createRegex(sampleIdentifier.sample.project)

        assert sampleIdentifier.validate()
    }

    @Test
    void test_validate_fail_regexIsSet() {
        createRegex(sampleIdentifier.sample.project)
        final String name = 'toolong'
        sampleIdentifier.name = name

        TestCase.assertValidateError(sampleIdentifier, 'name', 'validator.invalid', name)
    }

    @Test
    void test_validate_pass_regexIsSetForOtherProject() {
        createRegex(Project.build())
        final String name = 'toolong'
        sampleIdentifier.name = name

        assert sampleIdentifier.validate()
    }
}
