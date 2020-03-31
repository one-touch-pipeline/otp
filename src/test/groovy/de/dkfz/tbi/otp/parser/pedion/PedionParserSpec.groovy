/*
 * Copyright 2011-2020 The OTP authors
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
package de.dkfz.tbi.otp.parser.pedion

import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.ngsdata.SampleType
import de.dkfz.tbi.otp.parser.DefaultParsedSampleIdentifier

class PedionParserSpec extends Specification {

    private final PedionParser parser = new PedionParser()

    @SuppressWarnings(['AbcMetric', 'CyclomaticComplexity'])
    @Unroll('PeDiOn identifier #input is parsed to PID #pid, sample type name #sampleTypeDbName')
    void "parse valid input"() {
        when:
        DefaultParsedSampleIdentifier defaultParsedSampleIdentifier = parser.tryParse(input)
        boolean validPid = parser.tryParsePid(pid)

        then:
        defaultParsedSampleIdentifier
        defaultParsedSampleIdentifier.projectName == project
        defaultParsedSampleIdentifier.pid == pid
        defaultParsedSampleIdentifier.sampleTypeDbName == sampleTypeDbName
        defaultParsedSampleIdentifier.fullSampleName == input
        defaultParsedSampleIdentifier.useSpecificReferenceGenome == useSpecificReferenceGenome
        validPid

        where:
        input                 || project | pid           | sampleTypeDbName               | useSpecificReferenceGenome
        'A02P-ABCDEF-CAAA1A1' || 'A02P'  | 'A02P-ABCDEF' | 'CONTROL-BLOOD-01'             | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        //other funding
        'B02P-ABCDEF-CAAA1A1' || 'B02P'  | 'B02P-ABCDEF' | 'CONTROL-BLOOD-01'             | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        'C02P-ABCDEF-CAAA1A1' || 'C02P'  | 'C02P-ABCDEF' | 'CONTROL-BLOOD-01'             | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        //other project numbersnumbers
        'A08P-ABCDEF-CAAA1A1' || 'A08P'  | 'A08P-ABCDEF' | 'CONTROL-BLOOD-01'             | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        'A35P-ABCDEF-CAAA1A1' || 'A35P'  | 'A35P-ABCDEF' | 'CONTROL-BLOOD-01'             | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        //retrospective project
        'A02R-ABCDEF-CAAA1A1' || 'A02R'  | 'A02R-ABCDEF' | 'CONTROL-BLOOD-01'             | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        //other pseudonyms
        'A02P-EFGHIJ-CAAA1A1' || 'A02P'  | 'A02P-EFGHIJ' | 'CONTROL-BLOOD-01'             | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        'A02P-AAAAAA-CAAA1A1' || 'A02P'  | 'A02P-AAAAAA' | 'CONTROL-BLOOD-01'             | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        'A02P-ZZZZZZ-CAAA1A1' || 'A02P'  | 'A02P-ZZZZZZ' | 'CONTROL-BLOOD-01'             | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        //other categories
        'A02P-ABCDEF-TAAA1A1' || 'A02P'  | 'A02P-ABCDEF' | 'TUMOR-BLOOD-01'               | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        'A02P-ABCDEF-MAAA1A1' || 'A02P'  | 'A02P-ABCDEF' | 'METASTASIS-BLOOD-01'          | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        //other tissues
        'A02P-ABCDEF-CBAA1A1' || 'A02P'  | 'A02P-ABCDEF' | 'CONTROL-LIVER-01'             | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        'A02P-ABCDEF-CCAA1A1' || 'A02P'  | 'A02P-ABCDEF' | 'CONTROL-PANCREAS-01'          | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        'A02P-ABCDEF-CDAA1A1' || 'A02P'  | 'A02P-ABCDEF' | 'CONTROL-OVAR-01'              | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        'A02P-ABCDEF-CNAA1A1' || 'A02P'  | 'A02P-ABCDEF' | 'CONTROL-ORGANOID-LIVER-01'    | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        'A02P-ABCDEF-COAA1A1' || 'A02P'  | 'A02P-ABCDEF' | 'CONTROL-ORGANOID-PANCREAS-01' | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        //other biol replicate
        'A02P-ABCDEF-CACA1A1' || 'A02P'  | 'A02P-ABCDEF' | 'CONTROL-BLOOD-03'             | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        'A02P-ABCDEF-CAHA1A1' || 'A02P'  | 'A02P-ABCDEF' | 'CONTROL-BLOOD-08'             | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        'A02P-ABCDEF-CANA1A1' || 'A02P'  | 'A02P-ABCDEF' | 'CONTROL-BLOOD-14'             | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        'A02P-ABCDEF-CAZA1A1' || 'A02P'  | 'A02P-ABCDEF' | 'CONTROL-BLOOD-26'             | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        //other analyte
        'A02P-ABCDEF-CAAB1A1' || 'A02P'  | 'A02P-ABCDEF' | 'CONTROL-BLOOD-01'             | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        'A02P-ABCDEF-CAAG1A1' || 'A02P'  | 'A02P-ABCDEF' | 'CONTROL-BLOOD-01'             | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        'A02P-ABCDEF-CAAA3A1' || 'A02P'  | 'A02P-ABCDEF' | 'CONTROL-BLOOD-01'             | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        'A02P-ABCDEF-CAAA8A1' || 'A02P'  | 'A02P-ABCDEF' | 'CONTROL-BLOOD-01'             | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        //other sequencing assay
        'A02P-ABCDEF-CAAA1B1' || 'A02P'  | 'A02P-ABCDEF' | 'CONTROL-BLOOD-01'             | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        'A02P-ABCDEF-CAAA1H1' || 'A02P'  | 'A02P-ABCDEF' | 'CONTROL-BLOOD-01'             | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        'A02P-ABCDEF-CAAA1A3' || 'A02P'  | 'A02P-ABCDEF' | 'CONTROL-BLOOD-01'             | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        'A02P-ABCDEF-CAAA1A8' || 'A02P'  | 'A02P-ABCDEF' | 'CONTROL-BLOOD-01'             | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
    }

    @Unroll
    void "parse invalid input #input (problem: #problem)"() {
        given:
        DefaultParsedSampleIdentifier defaultParsedSampleIdentifier

        when:
        defaultParsedSampleIdentifier = parser.tryParse(input)

        then:
        defaultParsedSampleIdentifier == null

        where:
        input                  | problem
        ''                     | 'empty'
        null                   | 'null'
        //invalid project
        'a02P-ABCDEF-CAAA1A1'  | 'invalid founding: low letter founding'
        '202P-ABCDEF-CAAA1A1'  | 'invalid founding: number as founding'
        'ABCP-ABCDEF-CAAA1A1'  | 'invalid project number: letter instead of numbers'
        'A1P-ABCDEF-CAAA1A1'   | 'invalid project number: only one digit'
        'A123P-ABCDEF-CAAA1A1' | 'invalid project number: three digits'
        'A12A-ABCDEF-CAAA1A1'  | 'invalid char for retrospective/ prospective: A'
        'A12Z-ABCDEF-CAAA1A1'  | 'invalid char for retrospective/ prospective: Z'
        //invalid pseudonym
        'A02P-abcdef-CAAA1A1'  | 'invalid pseudonym: lowercase letter'
        'A02P-123456-CAAA1A1'  | 'invalid pseudonym: digist letter'
        'A02P-ABCDEFG-CAAA1A1' | 'invalid pseudonym: to many letter'
        'A02P-ABCDE-CAAA1A1'   | 'invalid pseudonym: to less letter'
        //invalid category
        'A02P-ABCDEF-DAAA1A1'  | 'invalid category: invalid char: D'
        'A02P-ABCDEF-HAAA1A1'  | 'invalid category: invalid char: H'
        'A02P-ABCDEF-YAAA1A1'  | 'invalid category: invalid char: Y'
        //invalid tissue type
        'A02P-ABCDEF-CEAA1A1'  | 'invalid tissue type: invalid char: E'
        'A02P-ABCDEF-CMAA1A1'  | 'invalid tissue type: invalid char: M'
        'A02P-ABCDEF-CPAA1A1'  | 'invalid tissue type: invalid char: P'
        'A02P-ABCDEF-CZAA1A1'  | 'invalid tissue type: invalid char: Z'
        //invalid bio replicate
        'A02P-ABCDEF-CAaA1A1'  | 'invalid bio replicate: lower case'
        'A02P-ABCDEF-CA5A1A1'  | 'invalid bio replicate: digests'
        //invalid analyte
        'A02P-ABCDEF-CAAa1A1'  | 'invalid analyte type: lower case for technical replicate'
        'A02P-ABCDEF-CAA31A1'  | 'invalid analyte type: number for technical replicate'
        'A02P-ABCDEF-CAAADA1'  | 'invalid analyte type: char d instead of diget for replicate'
        'A02P-ABCDEF-CAAAZA1'  | 'invalid analyte type: char Z instead of diget for replicate'
        //invalid sequencing assay
        'A02P-ABCDEF-CAAA1a1'  | 'invalid sequencing assay: lower case for technical replicate'
        'A02P-ABCDEF-CAAA151'  | 'invalid sequencing assay: number for technical replicate'
        'A02P-ABCDEF-CAAA1AD'  | 'invalid sequencing assay: char d instead of diget for replicate'
        'A02P-ABCDEF-CAAA1AZ'  | 'invalid sequencing assay: char Z instead of diget for replicate'
    }

    @Unroll
    void "test tryParseCellPosition is not implemented and always returns null (#identifier)"() {
        given:
        String singleCellWellLabel

        when:
        singleCellWellLabel = parser.tryParseCellPosition(identifier)

        then:
        singleCellWellLabel == null

        where:
        identifier << [
                'A02P-ABCDEF-CAAA1A1',
                'C02P-BCDEFG-TBBC3D4',
                'A12P-CDEFGH-MCCE7H8',
                'A02R-DEFGHI-CAAA1A1',
        ]
    }
}
