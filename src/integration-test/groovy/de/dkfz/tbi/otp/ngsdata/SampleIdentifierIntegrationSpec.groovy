/*
 * Copyright 2011-2024 The OTP authors
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
import grails.gorm.transactions.Rollback
import grails.validation.ValidationException
import spock.lang.Specification

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.SessionUtils

@Rollback
@Integration
class SampleIdentifierIntegrationSpec extends Specification {

    private static final String CORRECT_NAME = 'name'
    private static final String INCORRECT_NAME = 'toolong'

    private SampleIdentifier sampleIdentifier

    void setupData() {
        SessionUtils.metaClass.static.withNewSession = { Closure c -> c() }
        sampleIdentifier = DomainFactory.createSampleIdentifier(
                name: CORRECT_NAME,
        )
    }

    void cleanup() {
        TestCase.removeMetaClass(SessionUtils)
    }

    private void createRegex(final Project project) {
        DomainFactory.createProcessingOptionLazy(
                name: OptionName.VALIDATOR_SAMPLE_IDENTIFIER_REGEX,
                project: project,
                value: '[a-z]{4}',
        )
    }

    void "validation fails, name does not match project specific regex"() {
        given:
        setupData()
        createRegex(sampleIdentifier.sample.project)
        sampleIdentifier.name = INCORRECT_NAME

        when:
        sampleIdentifier.save(flush: true)

        then:
        thrown(ValidationException)
    }

    void "validation passes, name does match project specific regex"() {
        given:
        setupData()
        createRegex(sampleIdentifier.sample.project)

        expect:
        sampleIdentifier.save(flush: true)
    }

    void "validation passes, name does not match regex, but it is for a different project"() {
        given:
        setupData()
        createRegex(DomainFactory.createProject())
        sampleIdentifier.name = INCORRECT_NAME

        expect:
        sampleIdentifier.save(flush: true)
    }
}
