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
import de.dkfz.tbi.otp.ngsdata.SampleTypePerProject
import de.dkfz.tbi.otp.parser.DefaultParsedSampleIdentifier

class IlpParserSpec extends Specification {

    IlpParser ilpParser = new IlpParser(Mock(ProcessingOptionService) {
        _ * findOptionAsString(ProcessingOption.OptionName.ILP_PARSER_MAPPING) >> '{"abcd": "project1", "bcde": "project2"}'
    })

    @Unroll('ILP identifier #input is parsed to project #project, PID #pid, sample type name #sampleTypeDbName, category #expectedCategory')
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
        defaultParsedSampleIdentifier.sampleTypeCategory == expectedCategory

        where:
        input                       || project    | pid               | sampleTypeDbName | expectedCategory
        'abcd-abcdef-a1-a1-ig1'     || 'project1' | 'abcd-abcdef'     | 'a1-a1-ig1'      | SampleTypePerProject.Category.DISEASE
        and: 'other projects'
        'bcde-abcdef-a1-a1-ig1'     || 'project2' | 'bcde-abcdef'     | 'a1-a1-ig1'      | SampleTypePerProject.Category.DISEASE
        and: 'other pids can have different lengths for the pid part'
        'abcd-abcdefg-a1-a1-ig1'    || 'project1' | 'abcd-abcdefg'    | 'a1-a1-ig1'      | SampleTypePerProject.Category.DISEASE
        'abcd-abcdefgh-a1-a1-ig1'   || 'project1' | 'abcd-abcdefgh'   | 'a1-a1-ig1'      | SampleTypePerProject.Category.DISEASE
        'abcd-1234-a1-a1-ig1'       || 'project1' | 'abcd-1234'       | 'a1-a1-ig1'      | SampleTypePerProject.Category.DISEASE
        'abcd-12345-a1-a1-ig1'      || 'project1' | 'abcd-12345'      | 'a1-a1-ig1'      | SampleTypePerProject.Category.DISEASE
        'abcd-123456-a1-a1-ig1'     || 'project1' | 'abcd-123456'     | 'a1-a1-ig1'      | SampleTypePerProject.Category.DISEASE
        'abcd-1234567-a1-a1-ig1'    || 'project1' | 'abcd-1234567'    | 'a1-a1-ig1'      | SampleTypePerProject.Category.DISEASE
        'abcd-12345678-a1-a1-ig1'   || 'project1' | 'abcd-12345678'   | 'a1-a1-ig1'      | SampleTypePerProject.Category.DISEASE
        'abcd-123456789-a1-a1-ig1'  || 'project1' | 'abcd-123456789'  | 'a1-a1-ig1'      | SampleTypePerProject.Category.DISEASE
        'abcd-1234567890-a1-a1-ig1' || 'project1' | 'abcd-1234567890' | 'a1-a1-ig1'      | SampleTypePerProject.Category.DISEASE
        and: 'other sample types - CONTROL types'
        'abcd-abcdef-b1-a1-ig1'     || 'project1' | 'abcd-abcdef'     | 'b1-a1-ig1'      | SampleTypePerProject.Category.CONTROL
        'abcd-abcdef-n1-a1-ig1'     || 'project1' | 'abcd-abcdef'     | 'n1-a1-ig1'      | SampleTypePerProject.Category.CONTROL
        'abcd-abcdef-f1-a1-ig1'     || 'project1' | 'abcd-abcdef'     | 'f1-a1-ig1'      | SampleTypePerProject.Category.CONTROL
        'abcd-abcdef-k1-a1-ig1'     || 'project1' | 'abcd-abcdef'     | 'k1-a1-ig1'      | SampleTypePerProject.Category.CONTROL
        'abcd-abcdef-z1-a1-ig1'     || 'project1' | 'abcd-abcdef'     | 'z1-a1-ig1'      | SampleTypePerProject.Category.CONTROL
        and: 'other sample types - DISEASE types'
        'abcd-abcdef-a9-a1-ig1'     || 'project1' | 'abcd-abcdef'     | 'a9-a1-ig1'      | SampleTypePerProject.Category.DISEASE
        'abcd-abcdef-a1-b1-ig1'     || 'project1' | 'abcd-abcdef'     | 'a1-b1-ig1'      | SampleTypePerProject.Category.DISEASE
        'abcd-abcdef-a1-a2-ig1'     || 'project1' | 'abcd-abcdef'     | 'a1-a2-ig1'      | SampleTypePerProject.Category.DISEASE
        'abcd-abcdef-a1-a9-ig1'     || 'project1' | 'abcd-abcdef'     | 'a1-a9-ig1'      | SampleTypePerProject.Category.DISEASE
        'abcd-abcdef-a1-a1-ig2'     || 'project1' | 'abcd-abcdef'     | 'a1-a1-ig2'      | SampleTypePerProject.Category.DISEASE
        'abcd-abcdef-a1-a1-ig9'     || 'project1' | 'abcd-abcdef'     | 'a1-a1-ig9'      | SampleTypePerProject.Category.DISEASE
        'abcd-abcdef-t1-a1-ig1'     || 'project1' | 'abcd-abcdef'     | 't1-a1-ig1'      | SampleTypePerProject.Category.DISEASE
        'abcd-abcdef-m1-a1-ig1'     || 'project1' | 'abcd-abcdef'     | 'm1-a1-ig1'      | SampleTypePerProject.Category.DISEASE
        'abcd-abcdef-s1-a1-ig1'     || 'project1' | 'abcd-abcdef'     | 's1-a1-ig1'      | SampleTypePerProject.Category.DISEASE
        'abcd-abcdef-x1-a1-ig1'     || 'project1' | 'abcd-abcdef'     | 'x1-a1-ig1'      | SampleTypePerProject.Category.DISEASE
        'abcd-abcdef-l1-a1-ig1'     || 'project1' | 'abcd-abcdef'     | 'l1-a1-ig1'      | SampleTypePerProject.Category.DISEASE
        'abcd-abcdef-p1-a1-ig1'     || 'project1' | 'abcd-abcdef'     | 'p1-a1-ig1'      | SampleTypePerProject.Category.DISEASE
        'abcd-abcdef-c1-a1-ig1'     || 'project1' | 'abcd-abcdef'     | 'c1-a1-ig1'      | SampleTypePerProject.Category.DISEASE
        'abcd-abcdef-a2-a1-ig1'     || 'project1' | 'abcd-abcdef'     | 'a2-a1-ig1'      | SampleTypePerProject.Category.DISEASE
        'abcd-abcdef-q1-a1-ig1'     || 'project1' | 'abcd-abcdef'     | 'q1-a1-ig1'      | SampleTypePerProject.Category.DISEASE
        'abcd-abcdef-y1-a1-ig1'     || 'project1' | 'abcd-abcdef'     | 'y1-a1-ig1'      | SampleTypePerProject.Category.DISEASE
        'abcd-abcdef-u1-a1-ig1'     || 'project1' | 'abcd-abcdef'     | 'u1-a1-ig1'      | SampleTypePerProject.Category.DISEASE
        and: 'other sample types - no category mapping (null)'
        'abcd-abcdef-d1-a1-ig1'     || 'project1' | 'abcd-abcdef'     | 'd1-a1-ig1'      | null
        'abcd-abcdef-e1-a1-ig1'     || 'project1' | 'abcd-abcdef'     | 'e1-a1-ig1'      | null
        'abcd-abcdef-g1-a1-ig1'     || 'project1' | 'abcd-abcdef'     | 'g1-a1-ig1'      | null
        'abcd-abcdef-h1-a1-ig1'     || 'project1' | 'abcd-abcdef'     | 'h1-a1-ig1'      | null
        'abcd-abcdef-i1-a1-ig1'     || 'project1' | 'abcd-abcdef'     | 'i1-a1-ig1'      | null
        'abcd-abcdef-j1-a1-ig1'     || 'project1' | 'abcd-abcdef'     | 'j1-a1-ig1'      | null
        'abcd-abcdef-o1-a1-ig1'     || 'project1' | 'abcd-abcdef'     | 'o1-a1-ig1'      | null
        'abcd-abcdef-r1-a1-ig1'     || 'project1' | 'abcd-abcdef'     | 'r1-a1-ig1'      | null
        'abcd-abcdef-v1-a1-ig1'     || 'project1' | 'abcd-abcdef'     | 'v1-a1-ig1'      | null
        'abcd-abcdef-w1-a1-ig1'     || 'project1' | 'abcd-abcdef'     | 'w1-a1-ig1'      | null
        and: 'uppercase - should still work because we convert to lowercase'
        'abcd-ABCDEF-a1-a1-ig1'     || 'project1' | 'abcd-ABCDEF'     | 'a1-a1-ig1'      | SampleTypePerProject.Category.DISEASE
        'abcd-abcdef-A1-a1-ig1'     || 'project1' | 'abcd-abcdef'     | 'a1-a1-ig1'      | SampleTypePerProject.Category.DISEASE
        'abcd-abcdef-B1-a1-ig1'     || 'project1' | 'abcd-abcdef'     | 'b1-a1-ig1'      | SampleTypePerProject.Category.CONTROL
        'abcd-abcdef-a1-A1-ig1'     || 'project1' | 'abcd-abcdef'     | 'a1-a1-ig1'      | SampleTypePerProject.Category.DISEASE
        'abcd-abcdef-a1-a1-IG1'     || 'project1' | 'abcd-abcdef'     | 'a1-a1-ig1'      | SampleTypePerProject.Category.DISEASE
        and: 'first part of sample type has two digits'
        'abcd-abcdef-a10-a1-ig1'    || 'project1' | 'abcd-abcdef'     | 'a10-a1-ig1'     | SampleTypePerProject.Category.DISEASE
        and: 'second part of sample type has two digits'
        'abcd-abcdef-a1-a10-ig1'    || 'project1' | 'abcd-abcdef'     | 'a1-a10-ig1'     | SampleTypePerProject.Category.DISEASE
        and: 'third part of sample type has two digits'
        'abcd-abcdef-a1-a1-ig10'    || 'project1' | 'abcd-abcdef'     | 'a1-a1-ig10'     | SampleTypePerProject.Category.DISEASE
        and: 'last optional part of sample type is missing'
        'abcd-abcdef-a1-a1'         || 'project1' | 'abcd-abcdef'     | 'a1-a1'          | SampleTypePerProject.Category.DISEASE
        and: 'last optional part of sample type has one letter and two digits'
        'abcd-abcdef-a1-a1-i10'     || 'project1' | 'abcd-abcdef'     | 'a1-a1-i10'      | SampleTypePerProject.Category.DISEASE
        and: 'one letter and two digits in the first two parts of sample type and the last optional part has two letters and two digits'
        'abcd-abcd-a12-a12-ig10'    || 'project1' | 'abcd-abcd'       | 'a12-a12-ig10'   | SampleTypePerProject.Category.DISEASE
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

    @Unroll('Sample type category determination: #sampleType -> #expectedCategory')
    void "test determineSampleTypeCategory"() {
        when:
        SampleTypePerProject.Category result = ilpParser.determineSampleTypeCategory(sampleType)

        then:
        result == expectedCategory

        where:
        sampleType  || expectedCategory
        and: 'CONTROL'
        'b1-a1-ig1' || SampleTypePerProject.Category.CONTROL
        'n1-a1-ig1' || SampleTypePerProject.Category.CONTROL
        'f1-a1-ig1' || SampleTypePerProject.Category.CONTROL
        'k1-a1-ig1' || SampleTypePerProject.Category.CONTROL
        'z1-a1-ig1' || SampleTypePerProject.Category.CONTROL
        and: 'DISEASE'
        't1-a1-ig1' || SampleTypePerProject.Category.DISEASE
        'm1-a1-ig1' || SampleTypePerProject.Category.DISEASE
        's1-a1-ig1' || SampleTypePerProject.Category.DISEASE
        'x1-a1-ig1' || SampleTypePerProject.Category.DISEASE
        'l1-a1-ig1' || SampleTypePerProject.Category.DISEASE
        'p1-a1-ig1' || SampleTypePerProject.Category.DISEASE
        'c1-a1-ig1' || SampleTypePerProject.Category.DISEASE
        'a1-a1-ig1' || SampleTypePerProject.Category.DISEASE
        'q1-a1-ig1' || SampleTypePerProject.Category.DISEASE
        'y1-a1-ig1' || SampleTypePerProject.Category.DISEASE
        'u1-a1-ig1' || SampleTypePerProject.Category.DISEASE
        and: 'null'
        'd1-a1-ig1' || null
        'e1-a1-ig1' || null
        'g1-a1-ig1' || null
        'h1-a1-ig1' || null
        'i1-a1-ig1' || null
        'j1-a1-ig1' || null
        'o1-a1-ig1' || null
        'r1-a1-ig1' || null
        'v1-a1-ig1' || null
        'w1-a1-ig1' || null
    }
}
