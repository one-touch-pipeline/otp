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
package de.dkfz.tbi.otp.parser.hipo

import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.ngsdata.SampleType

class HipoSampleIdentifierParserSpec extends Specification {

    HipoSampleIdentifierParser parser = new HipoSampleIdentifierParser()

    @Unroll
    void 'tryParse, when H059, uses sample number exactly as given'(String sampleNumber) {
        given:
        String fullSampleName = "H059-ABCDEF-T${sampleNumber}-D1"

        when:
        HipoSampleIdentifier identifier = parser.tryParse(fullSampleName)
        boolean validPid = parser.tryParsePid(fullSampleName.substring(0, 11))

        then:
        validPid
        identifier.sampleNumber == sampleNumber
        identifier.sampleTypeDbName == "tumor${sampleNumber}".toString()
        identifier.fullSampleName == fullSampleName
        identifier.useSpecificReferenceGenome == SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT

        where:
        sampleNumber << ['0', '00', '1', '01', '2', '02', '10', '11']
    }

    @Unroll
    void 'tryParse, when not H059 and sample number has two digits, returns null'(String sampleNumber) {
        given:
        String fullSampleName = "H123-ABCDEF-T${sampleNumber}-D1"

        expect:
        parser.tryParse(fullSampleName) == null

        where:
        sampleNumber << ['00', '01', '02', '10', '11']
    }

    @Unroll
    void "test parsePid invalid input #pid"() {
        given:
        boolean validPid

        when:
        validPid = parser.tryParsePid(pid)

        then:
        !validPid

        where:
        pid                 | _
        ''                  | _
        null                | _
        and: 'Input with invalid pid'
        'INVALID_PID'          | _
    }

    @Unroll
    void "test tryParseSingleCellWellLabel is not implemented and always returns null"() {
        given:
        String singleCellWellLabel

        when:
        singleCellWellLabel = parser.tryParseSingleCellWellLabel(identifier)

        then:
        singleCellWellLabel == null

        where:
        identifier << [
                'H059-ABCDEF-T0-D1',
                'H059-ABCDEF-T01-D1',
                'INVALID_PID',
        ]
    }
}
