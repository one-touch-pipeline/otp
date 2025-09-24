/*
 * Copyright 2011-2025 The OTP authors
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
package de.dkfz.tbi.otp.parser.ilp

import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.parser.DefaultParsedSampleIdentifier

class IlpParserSpec extends Specification {

    IlpParser ilpParser = new IlpParser(Mock(ProcessingOptionService) {
        _ * findOptionAsString(ProcessingOption.OptionName.ILP_PARSER_MAPPING) >> '{"abcd": "project1", "bcde": "project2"}'
    })

    @Unroll('ILP identifier #input is parsed to project #project, PID #pid, sample type name #sampleTypeDbName')
    void "test parse valid input"() {
        given:
        DefaultParsedSampleIdentifier defaultParsedSampleIdentifier

        when:
        defaultParsedSampleIdentifier = ilpParser.tryParse(input)

        then:
        defaultParsedSampleIdentifier.projectName == project
        defaultParsedSampleIdentifier.pid == pid
        defaultParsedSampleIdentifier.sampleTypeDbName == sampleTypeDbName
        defaultParsedSampleIdentifier.fullSampleName == input

        where:
        input                       || project    | pid               | sampleTypeDbName
        'abcd-abcdef-a1-a1-ig1'     || 'project1' | 'abcd-abcdef'     | 'a1-a1-ig1'
        // other projects
        'bcde-abcdef-a1-a1-ig1'     || 'project2' | 'bcde-abcdef'     | 'a1-a1-ig1'
        // other pids can have different lengths for the pid part
        'abcd-abcdefg-a1-a1-ig1'    || 'project1' | 'abcd-abcdefg'    | 'a1-a1-ig1'
        'abcd-abcdefgh-a1-a1-ig1'   || 'project1' | 'abcd-abcdefgh'   | 'a1-a1-ig1'
        'abcd-123456-a1-a1-ig1'     || 'project1' | 'abcd-123456'     | 'a1-a1-ig1'
        'abcd-1234567-a1-a1-ig1'    || 'project1' | 'abcd-1234567'    | 'a1-a1-ig1'
        'abcd-12345678-a1-a1-ig1'   || 'project1' | 'abcd-12345678'   | 'a1-a1-ig1'
        'abcd-1234-a1-a1-ig1'       || 'project1' | 'abcd-1234'       | 'a1-a1-ig1'
        'abcd-12345-a1-a1-ig1'      || 'project1' | 'abcd-12345'      | 'a1-a1-ig1'
        'abcd-123456-a1-a1-ig1'     || 'project1' | 'abcd-123456'     | 'a1-a1-ig1'
        'abcd-12345678-a1-a1-ig1'   || 'project1' | 'abcd-12345678'   | 'a1-a1-ig1'
        'abcd-123456789-a1-a1-ig1'  || 'project1' | 'abcd-123456789'  | 'a1-a1-ig1'
        'abcd-1234567890-a1-a1-ig1' || 'project1' | 'abcd-1234567890' | 'a1-a1-ig1'
        // other sample types
        'abcd-abcdef-b1-a1-ig1'     || 'project1' | 'abcd-abcdef'     | 'b1-a1-ig1'
        'abcd-abcdef-a2-a1-ig1'     || 'project1' | 'abcd-abcdef'     | 'a2-a1-ig1'
        'abcd-abcdef-a9-a1-ig1'     || 'project1' | 'abcd-abcdef'     | 'a9-a1-ig1'
        'abcd-abcdef-a1-b1-ig1'     || 'project1' | 'abcd-abcdef'     | 'a1-b1-ig1'
        'abcd-abcdef-a1-a2-ig1'     || 'project1' | 'abcd-abcdef'     | 'a1-a2-ig1'
        'abcd-abcdef-a1-a9-ig1'     || 'project1' | 'abcd-abcdef'     | 'a1-a9-ig1'
        'abcd-abcdef-a1-a1-ig2'     || 'project1' | 'abcd-abcdef'     | 'a1-a1-ig2'
        'abcd-abcdef-a1-a1-ig9'     || 'project1' | 'abcd-abcdef'     | 'a1-a1-ig9'
        // uppercase
        'abcd-ABCDEF-a1-a1-ig1'     || 'project1' | 'abcd-ABCDEF'     | 'a1-a1-ig1'
        'abcd-abcdef-A1-a1-ig1'     || 'project1' | 'abcd-abcdef'     | 'a1-a1-ig1'
        'abcd-abcdef-a1-A1-ig1'     || 'project1' | 'abcd-abcdef'     | 'a1-a1-ig1'
        'abcd-abcdef-a1-a1-IG1'     || 'project1' | 'abcd-abcdef'     | 'a1-a1-ig1'
        // first part of sample type has two digits
        'abcd-abcdef-a10-a1-ig1'    || 'project1' | 'abcd-abcdef'     | 'a10-a1-ig1'
        // second part of sample type has two digits
        'abcd-abcdef-a1-a10-ig1'    || 'project1' | 'abcd-abcdef'     | 'a1-a10-ig1'
        // third part of sample type has two digits
        'abcd-abcdef-a1-a1-ig10'    || 'project1' | 'abcd-abcdef'     | 'a1-a1-ig10'
        // last optional part of sample type is missing
        'abcd-abcdef-a1-a1'         || 'project1' | 'abcd-abcdef'     | 'a1-a1'
        // last optional part of sample type has one letter and two digits
        'abcd-abcdef-a1-a1-i10'     || 'project1' | 'abcd-abcdef'     | 'a1-a1-i10'
        // one letter and two digits in the first two parts of sample type and the last optional part has two letters and two digits
        'abcd-abcd-a12-a12-ig10'    || 'project1' | 'abcd-abcd'       | 'a12-a12-ig10'
    }

    @Unroll
    void "test parse invalid input #input (problem: #problem)"() {
        given:
        DefaultParsedSampleIdentifier defaultParsedSampleIdentifier

        when:
        defaultParsedSampleIdentifier = ilpParser.tryParse(input)

        then:
        defaultParsedSampleIdentifier == null

        where:
        input                        | problem
        ''                           | 'empty'
        null                         | 'null'
        'cdef-abcdef-a1-a1-ig1'      | 'unknown project'
        'abc-abcdef-a1-a1-ig1'       | 'project part too short"'
        'abcde-abcdef-a1-a1-ig1'     | 'project part too long"'
        'abcd-abc-a1-a1-ig1'         | 'pid too short'
        'abcd-abcdefghijk-a1-a1-ig1' | 'pid too long'
        'abcd-abcdef-a1-ig1'         | 'one of the sample type parts is missing, but optional part is there'
        'abcd-abcdef-aa-a1-ig1'      | 'two letters in the first part of sample type'
        'abcd-abcdef-a1-11-ig1'      | 'two digits in the second part of sample type'
        'abcd-abcdef-a1-a1-ig'       | 'third optional part of the sample type without digit'
        'abcd-abcdef-a1-a1-i123'     | 'third optional part of the sample type with too many digits'
    }

    void "test tryParseSingleCellWellLabel is not implemented and returns null"() {
        given:
        String singleCellWellLabel

        when:
        singleCellWellLabel = ilpParser.tryParseSingleCellWellLabel('abcd-abcdef-a1-a1-ig1')

        then:
        singleCellWellLabel == null
    }
}
