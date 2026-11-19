/*
 * Copyright 2011-2026 The OTP authors
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
package de.dkfz.tbi.otp.dataimport

import grails.testing.gorm.DataTest
import spock.lang.Specification

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.domainFactory.dataimport.DataImportDomainFactory

class DataTypeSpec extends Specification implements DataTest, DataImportDomainFactory {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                DataType,
        ]
    }

    void "validate, when name is set, then it is valid"() {
        expect:
        createDataType([:], false).validate()
    }

    void "validate, when name is null, then it is invalid"() {
        expect:
        TestCase.assertValidateError(createDataType([name: null], false), 'name', 'nullable', null)
    }

    void "validate, when name is blank, then it is invalid"() {
        expect:
        TestCase.assertValidateError(createDataType([name: ''], false), 'name', 'blank', '')
    }

    void "validate, when name already exists, then it is invalid"() {
        given:
        createDataType(name: 'ONT')

        expect:
        TestCase.assertValidateError(createDataType([name: 'ONT'], false), 'name', 'unique', 'ONT')
    }
}
