package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import grails.test.spock.IntegrationSpec

class SampleIdentifierIntegrationSpec extends IntegrationSpec {

    static private String CORRECT_NAME = 'name'

    private SampleIdentifier sampleIdentifier

    def setup() {
        sampleIdentifier = new SampleIdentifier(
                name: CORRECT_NAME,
                sample: DomainFactory.createSample())
        assert sampleIdentifier.validate()
    }

    private void createRegex(final Project project) {
        assert new ProcessingOption(
                name: SampleIdentifier.REGEX_OPTION_NAME,
                project: project,
                value: '[a-z]{4}',
                comment: '',
        ).save(failOnError: true)
    }

    void test_validate_fail_regexIsSet() {
        createRegex(sampleIdentifier.sample.project)
        final String name = 'toolong'
        sampleIdentifier.name = name

        expect:
        TestCase.assertValidateError(sampleIdentifier, 'name', 'validator.invalid', name)
    }

    void test_validate_pass_regexIsSet() {
        createRegex(sampleIdentifier.sample.project)

        expect:
        sampleIdentifier.validate()
    }


    void test_validate_pass_regexIsSetForOtherProject() {
        createRegex(DomainFactory.createProject())
        final String name = 'toolong'
        sampleIdentifier.name = name

        expect:
        sampleIdentifier.validate()
    }
}
