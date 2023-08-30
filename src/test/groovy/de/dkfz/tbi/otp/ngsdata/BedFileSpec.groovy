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

import grails.testing.gorm.DataTest
import spock.lang.Specification

class BedFileSpec extends Specification implements DataTest {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                BedFile,
        ]
    }

    void "validate, when all correct, then pass validation"() {
        given:
        BedFile bedFile = DomainFactory.createBedFile()

        expect:
        bedFile.validate()
    }

    void "validate, when file name is null, then fail validation"() {
        given:
        BedFile bedFile = DomainFactory.createBedFile()

        when:
        bedFile.fileName = null
        bedFile.validate()

        then:
        bedFile.errors.hasErrors()
    }

    void "validate, when file name is blank, then fail validation"() {
        given:
        BedFile bedFile = DomainFactory.createBedFile()

        when:
        bedFile.fileName = ""
        bedFile.validate()

        then:
        bedFile.errors.hasErrors()
    }

    void "validate, when combination of file name and reference genome is not unique, then fail validation"() {
        given:
        BedFile bedFile1 = DomainFactory.createBedFile()
        BedFile bedFile2 = DomainFactory.createBedFile()

        when:
        bedFile2.fileName = bedFile1.fileName
        bedFile2.referenceGenome = bedFile1.referenceGenome
        bedFile2.validate()

        then:
        bedFile2.errors.hasErrors()
    }

    void "validate, when combination of library preperation kit and reference genome is not unique, then fail validation"() {
        given:
        BedFile bedFile1 = DomainFactory.createBedFile()
        BedFile bedFile2 = DomainFactory.createBedFile()

        when:
        bedFile2.libraryPreparationKit = bedFile1.libraryPreparationKit
        bedFile2.referenceGenome = bedFile1.referenceGenome
        bedFile2.validate()

        then:
        bedFile2.errors.hasErrors()
    }

    void "validate, when target size is not positive, then fail validation"() {
        given:
        BedFile bedFile = DomainFactory.createBedFile()

        when:
        bedFile.targetSize = targetSize
        bedFile.validate()

        then:
        bedFile.errors.hasErrors()

        where:
        targetSize << [
                0,
                -1,
        ]
    }
}
