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

import grails.testing.gorm.DataTest
import spock.lang.Specification

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption

class SampleIdentifierSpec extends Specification implements DataTest {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                ProcessingOption,
                SampleIdentifier,
        ]
    }

    private static final String CORRECT_NAME = 'name'

    private SampleIdentifier sampleIdentifier

    void setup() {
        sampleIdentifier = new SampleIdentifier(
            name: CORRECT_NAME,
            sample: DomainFactory.createSample(),
        )
        assert sampleIdentifier.validate()
    }

    void "test validate, when name is null, should fail"() {
        given:
        sampleIdentifier.name = null

        expect:
        TestCase.assertValidateError(sampleIdentifier, 'name', 'nullable', null)
    }

    void "test validate, when sample is null, should fail"() {
        given:
        sampleIdentifier.sample = null

        expect:
        TestCase.assertValidateError(sampleIdentifier, 'sample', 'nullable', null)
    }

    void "test validate, when name is empty, should fail"() {
        given:
        sampleIdentifier.name = ''

        expect:
        TestCase.assertValidateError(sampleIdentifier, 'name', 'blank', '')
    }

    void "test validate, when name is too short, should fail"() {
        given:
        final String shortName = '12'
        sampleIdentifier.name = shortName

        expect:
        TestCase.assertValidateError(sampleIdentifier, 'name', 'minSize.notmet', shortName)
    }

    void "test validate, when name is not unique, should fail"() {
        given:
        assert sampleIdentifier.save(flush: true)
        SampleIdentifier sampleIdentifier2 = new SampleIdentifier(
            name: CORRECT_NAME,
            sample: DomainFactory.createSample(),
        )

        expect:
        TestCase.assertValidateError(sampleIdentifier2, 'name', 'unique', CORRECT_NAME)
    }

    void "test validate, when name is untrimmed, should fail"() {
        given:
        sampleIdentifier.name = name

        expect:
        TestCase.assertValidateError(sampleIdentifier, 'name', 'untrimmed', name)

        where:
        name <<  [
                ' aname',
                'aname ',
                ' aname ',
        ]
    }
}
