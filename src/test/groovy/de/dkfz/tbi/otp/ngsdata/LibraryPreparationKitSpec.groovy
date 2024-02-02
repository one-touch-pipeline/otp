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

class LibraryPreparationKitSpec extends Specification implements DataTest, DomainFactoryCore {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                LibraryPreparationKit,
        ]
    }

    @Unroll
    void "validate, when #property is '#value', then validation should fail for #constraint"() {
        given:
        LibraryPreparationKit libraryPreparationKit = createLibraryPreparationKit()

        when:
        libraryPreparationKit[property] = value

        then:
        TestCase.assertValidateError(libraryPreparationKit, property, constraint, value)

        where:
        property | constraint | value
        'name'   | 'nullable' | null
        'name'   | 'blank'    | ''
    }

    void "validate, when name is not unique, then validation should fail"() {
        given:
        LibraryPreparationKit libraryPreparationKit1 = createLibraryPreparationKit()
        LibraryPreparationKit libraryPreparationKit2 = createLibraryPreparationKit()

        when:
        libraryPreparationKit2.name = libraryPreparationKit1.name

        then:
        TestCase.assertValidateError(libraryPreparationKit2, 'name', 'unique', libraryPreparationKit2.name)
    }
}
