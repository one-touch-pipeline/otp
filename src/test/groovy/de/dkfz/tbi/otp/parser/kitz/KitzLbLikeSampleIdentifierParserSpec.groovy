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
package de.dkfz.tbi.otp.parser.kitz

import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.parser.DefaultParsedSampleIdentifier

class KitzLbLikeSampleIdentifierParserSpec extends Specification {

    KitzLbLikeSampleIdentifierParser parser = new KitzLbLikeSampleIdentifierParser(Mock(ProcessingOptionService) {
        _ * findOptionAsString(ProcessingOption.OptionName.KITZ_LB_LIKE_PARSER_MAPPING) >> '{"TBFT": "project1", "KITZ": "project2"}'
    })

    @Unroll('KITZ_LB identifier #input is parsed to project #project, PID #pid, and sample type name #sampleTypeDbName')
    void "test parse valid input"() {
        given:
        DefaultParsedSampleIdentifier defaultParsedSampleIdentifier

        when:
        defaultParsedSampleIdentifier = parser.tryParse(input)

        then:
        defaultParsedSampleIdentifier.projectName == project
        defaultParsedSampleIdentifier.pid == pid
        defaultParsedSampleIdentifier.sampleTypeDbName == sampleTypeDbName
        defaultParsedSampleIdentifier.fullSampleName == input
        defaultParsedSampleIdentifier.sampleTypeCategory == null

        where:
        input                   || project    | pid                | sampleTypeDbName
        'TBFT-16LB-0030-B01.37' || 'project1' | 'TBFT-16LB-0030'   | 'blood-01-37'
        'TBFT-5NBS-0139-DBS1.1' || 'project1' | 'TBFT-5NBS-0139'   | 'dbs-01-01'
        'KITZ-ABC-1234-T1.2'    || 'project2' | 'KITZ-ABC-1234'    | 'tumor-01-02'
        'TBFT-123456-0000-P9.9' || 'project1' | 'TBFT-123456-0000' | 'plasma-09-09'
        'TBFT-XYZ-9999-L10.10'  || 'project1' | 'TBFT-XYZ-9999'    | 'csf-10-10'
        'TBFT-ABC-1234-A1.1'    || 'project1' | 'TBFT-ABC-1234'    | 'eyefluid-01-01'
        'TBFT-ABC-1234-S1.1'    || 'project1' | 'TBFT-ABC-1234'    | 'serum-01-01'
        'TBFT-ABC-1234-TD1.1'   || 'project1' | 'TBFT-ABC-1234'    | 'teardrop-01-01'
        'TBFT-ABC-1234-U1.1'    || 'project1' | 'TBFT-ABC-1234'    | 'urine-01-01'
        'TBFT-ABC-123-P1.1'     || 'project1' | 'TBFT-ABC-123'     | 'plasma-01-01'
    }

    @Unroll
    void "test parse invalid input #input (problem: #problem)"() {
        given:
        DefaultParsedSampleIdentifier defaultParsedSampleIdentifier

        when:
        defaultParsedSampleIdentifier = parser.tryParse(input)

        then:
        defaultParsedSampleIdentifier == null

        where:
        input                      | problem
        ''                         | 'empty'
        null                       | 'null'
        'UNKN-16LB-0030-B01.37'    | 'unknown project'
        'TBF-16LB-0030-B01.37'     | 'prefix too short'
        'TBFTT-16LB-0030-B01.37'   | 'prefix too long'
        'TBFT-12-0030-B01.37'      | 'intermediate too short'
        'TBFT-1234567-0030-B01.37' | 'intermediate too long'
        'TBFT-16LB-02-B01.37'      | 'digits too short'
        'TBFT-16LB-00300-B01.37'   | 'digits too long'
        'TBFT-16LB-0030-X01.37'    | 'invalid tissue type'
        'TBFT-16LB-0030-B.37'      | 'missing sample number'
        'TBFT-16LB-0030-B133.37'   | 'sample number too long'
        'TBFT-16LB-0030-B01.'      | 'missing aliquot number'
        'TBFT-16LB-0030-B01.1337'  | 'aliquot number too long'
        'TBFT-16LB-0030-B01-37'    | 'wrong separator'
    }

    void "test tryParseSingleCellWellLabel returns null"() {
        expect:
        parser.tryParseSingleCellWellLabel('TBFT-16LB-0030-B01.37') == null
    }
}
