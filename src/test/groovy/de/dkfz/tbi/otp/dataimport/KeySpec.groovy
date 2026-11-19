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
import spock.lang.Unroll

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.domainFactory.dataimport.DataImportDomainFactory

class KeySpec extends Specification implements DataTest, DataImportDomainFactory {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                DataType,
                Key,
        ]
    }

    void "KeyType contains exactly the expected values"() {
        expect:
        KeyType.values() as List == [KeyType.PROJECT, KeyType.DATA, KeyType.FILE, KeyType.DIRECTORY, KeyType.MD5SUM]
    }

    void "validate, when all properties are set, then it is valid"() {
        expect:
        createKey([:], false).validate()
    }

    @Unroll
    void "validate, when #property is null, then it is invalid"() {
        expect:
        TestCase.assertValidateError(createKey([(property): null], false), property, 'nullable', null)

        where:
        property << ['name', 'keyType', 'dataType']
    }

    void "validate, when name is blank, then it is invalid"() {
        expect:
        TestCase.assertValidateError(createKey([name: ''], false), 'name', 'blank', '')
    }

    void "validate, when name already exists for the same data type, then it is invalid"() {
        given:
        DataType dataType = createDataType()
        createKey(name: 'file', dataType: dataType)

        expect:
        TestCase.assertValidateError(createKey([name: 'file', dataType: dataType], false), 'name', 'unique', 'file')
    }

    void "validate, when name already exists for another data type, then it is valid"() {
        given:
        createKey(name: 'file', keyType: KeyType.FILE)

        expect:
        createKey([name: 'file', keyType: KeyType.DIRECTORY, dataType: createDataType()], false).validate()
    }
}
