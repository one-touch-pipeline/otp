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

package de.dkfz.tbi.otp.ngsdata

import grails.testing.mixin.integration.Integration
import grails.transaction.Rollback
import grails.validation.ValidationException
import spock.lang.Specification

import de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName

@Rollback
@Integration
class SampleIdentifierIntegrationSpec extends Specification {

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
                name: OptionName.VALIDATOR_SAMPLE_IDENTIFIER_REGEX,
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
