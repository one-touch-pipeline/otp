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
import spock.lang.Unroll

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore

class SampleTypeSpec extends Specification implements DataTest, DomainFactoryCore {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                SampleType,
        ]
    }

    @Unroll
    void "validate, when name is '#name', then validation should pass"() {
        when:
        SampleType sampleType = new SampleType(
                name: name,
        )
        sampleType.validate()

        then:
        !sampleType.errors.hasErrors()

        where:
        name << [
                'sampletype',
                'sample-type',
                'sample123',
        ]
    }

    @Unroll
    void "validate, when name is '#name', then validation should fail"() {
        when:
        SampleType sampleType = new SampleType(
                name: name,
        )
        sampleType.validate()

        then:
        TestCase.assertValidateError(sampleType, 'name', constraint, sampleType.name)

        where:
        name          | constraint
        null          | 'nullable'
        'SampleType'  | 'validator.obj.name.toLowerCase'
        ''            | 'blank'
        'sample_type' | 'underscore'
        'sample type' | 'validator.path.component'
        'sample!type' | 'validator.path.component'
        'sample?type' | 'validator.path.component'
        'sample#type' | 'validator.path.component'
        'sample&type' | 'validator.path.component'
    }

    void "validate, when specificReferenceGenome is null, then validation should fail"() {
        when:
        SampleType sampleType = new SampleType(
                name: 'sample-type',
                specificReferenceGenome: null,
        )

        then:
        TestCase.assertValidateError(sampleType, 'specificReferenceGenome', 'nullable', null)
    }

    void "validate, when name already exist with underscore, then validation should pass"() {
        // Some legacy objects has already underscore and needs therefore pass the revalidation
        given:
        SampleType sampleType = new SampleType(
                name: 'sample_type',
        )
        sampleType.save(flush: true, validate: false)

        when:
        sampleType.validate()

        then:
        !sampleType.errors.hasErrors()
    }
}
