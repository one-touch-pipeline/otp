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

    @SuppressWarnings(['CyclomaticComplexity'])
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
        'A02P-ABCDEF-CAAAAAA' || 'A02P'  | 'A02P-ABCDEF' | 'control-blood-01'             | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        //other funding
        'B02P-ABCDEF-CAAAAAA' || 'B02P'  | 'B02P-ABCDEF' | 'control-blood-01'             | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        'C02P-ABCDEF-CAAAAAA' || 'C02P'  | 'C02P-ABCDEF' | 'control-blood-01'             | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        //other project numbersnumbers
        'A08P-ABCDEF-CAAAAAA' || 'A08P'  | 'A08P-ABCDEF' | 'control-blood-01'             | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        'A35P-ABCDEF-CAAAAAA' || 'A35P'  | 'A35P-ABCDEF' | 'control-blood-01'             | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        //retrospective project
        'A02R-ABCDEF-CAAAAAA' || 'A02R'  | 'A02R-ABCDEF' | 'control-blood-01'             | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        //other pseudonyms
        'A02P-EFGHIJ-CAAAAAA' || 'A02P'  | 'A02P-EFGHIJ' | 'control-blood-01'             | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        'A02P-AAAAAA-CAAAAAA' || 'A02P'  | 'A02P-AAAAAA' | 'control-blood-01'             | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        'A02P-ZZZZZZ-CAAAAAA' || 'A02P'  | 'A02P-ZZZZZZ' | 'control-blood-01'             | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        //other categories
        'A02P-ABCDEF-TAAAAAA' || 'A02P'  | 'A02P-ABCDEF' | 'tumor-blood-01'               | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        'A02P-ABCDEF-MAAAAAA' || 'A02P'  | 'A02P-ABCDEF' | 'metastasis-blood-01'          | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        //other tissues
        'A02P-ABCDEF-CBAAAAA' || 'A02P'  | 'A02P-ABCDEF' | 'control-liver-01'             | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        'A02P-ABCDEF-CCAAAAA' || 'A02P'  | 'A02P-ABCDEF' | 'control-pancreas-01'          | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        'A02P-ABCDEF-CDAAAAA' || 'A02P'  | 'A02P-ABCDEF' | 'control-ovar-01'              | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        'A02P-ABCDEF-CIAAAAA' || 'A02P'  | 'A02P-ABCDEF' | 'control-small-intestine-01'   | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        'A02P-ABCDEF-CNAAAAA' || 'A02P'  | 'A02P-ABCDEF' | 'control-organoid-liver-01'    | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        'A02P-ABCDEF-COAAAAA' || 'A02P'  | 'A02P-ABCDEF' | 'control-organoid-pancreas-01' | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        //other biol replicate
        'A02P-ABCDEF-CACAAAA' || 'A02P'  | 'A02P-ABCDEF' | 'control-blood-03'             | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        'A02P-ABCDEF-CAHAAAA' || 'A02P'  | 'A02P-ABCDEF' | 'control-blood-08'             | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        'A02P-ABCDEF-CANAAAA' || 'A02P'  | 'A02P-ABCDEF' | 'control-blood-14'             | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        'A02P-ABCDEF-CAZAAAA' || 'A02P'  | 'A02P-ABCDEF' | 'control-blood-26'             | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        //other analyte
        'A02P-ABCDEF-CAABAAA' || 'A02P'  | 'A02P-ABCDEF' | 'control-blood-01'             | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        'A02P-ABCDEF-CAAGAAA' || 'A02P'  | 'A02P-ABCDEF' | 'control-blood-01'             | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        'A02P-ABCDEF-CAAACAA' || 'A02P'  | 'A02P-ABCDEF' | 'control-blood-01'             | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        'A02P-ABCDEF-CAAAHAA' || 'A02P'  | 'A02P-ABCDEF' | 'control-blood-01'             | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        //other sequencing assay
        'A02P-ABCDEF-CAAAABA' || 'A02P'  | 'A02P-ABCDEF' | 'control-blood-01'             | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        'A02P-ABCDEF-CAAAAHA' || 'A02P'  | 'A02P-ABCDEF' | 'control-blood-01'             | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        'A02P-ABCDEF-CAAAAAC' || 'A02P'  | 'A02P-ABCDEF' | 'control-blood-01'             | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        'A02P-ABCDEF-CAAAAAH' || 'A02P'  | 'A02P-ABCDEF' | 'control-blood-01'             | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
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
        'a02P-ABCDEF-CAAAAAA'  | 'invalid founding: low letter founding'
        '202P-ABCDEF-CAAAAAA'  | 'invalid founding: number as founding'
        'ABCP-ABCDEF-CAAAAAA'  | 'invalid project number: letter instead of numbers'
        'A1P-ABCDEF-CAAAAAA'   | 'invalid project number: only one digit'
        'A123P-ABCDEF-CAAAAAA' | 'invalid project number: three digits'
        'A12A-ABCDEF-CAAAAAA'  | 'invalid char for retrospective/ prospective: A'
        'A12Z-ABCDEF-CAAAAAA'  | 'invalid char for retrospective/ prospective: Z'
        //invalid pseudonym
        'A02P-abcdef-CAAAAAA'  | 'invalid pseudonym: lowercase letter'
        'A02P-123456-CAAAAAA'  | 'invalid pseudonym: digit letter'
        'A02P-ABCDEFG-CAAAAAA' | 'invalid pseudonym: to many letter'
        'A02P-ABCDE-CAAAAAA'   | 'invalid pseudonym: to less letter'
        //invalid category
        'A02P-ABCDEF-DAAAAAA'  | 'invalid category: invalid char: D'
        'A02P-ABCDEF-HAAAAAA'  | 'invalid category: invalid char: H'
        'A02P-ABCDEF-YAAAAAA'  | 'invalid category: invalid char: Y'
        //invalid tissue type
        'A02P-ABCDEF-CKAAAAA'  | 'invalid tissue type: invalid char: K'
        'A02P-ABCDEF-CMAAAAA'  | 'invalid tissue type: invalid char: M'
        'A02P-ABCDEF-CPAAAAA'  | 'invalid tissue type: invalid char: P'
        'A02P-ABCDEF-CZAAAAA'  | 'invalid tissue type: invalid char: Z'
        //invalid bio replicate
        'A02P-ABCDEF-CAaAAAA'  | 'invalid bio replicate: lower case'
        'A02P-ABCDEF-CA5AAAA'  | 'invalid bio replicate: digit'
        //invalid analyte
        'A02P-ABCDEF-CAAaAA'   | 'invalid analyte type: lower case for technical replicate'
        'A02P-ABCDEF-CAA3AAA'  | 'invalid analyte type: number for analyte'
        'A02P-ABCDEF-CAAA1AA'  | 'invalid analyte type: number 1 instead of char for replicate'
        'A02P-ABCDEF-CAAA9AA'  | 'invalid analyte type: number 9 instead of char for replicate'
        //invalid sequencing assay
        'A02P-ABCDEF-CAAAAaA'  | 'invalid sequencing assay: lower case for sequencing assay'
        'A02P-ABCDEF-CAAAA5A'  | 'invalid sequencing assay: number for sequencing assay'
        'A02P-ABCDEF-CAAAAA1'  | 'invalid sequencing assay: number 1 instead of char for sequencing assay'
        'A02P-ABCDEF-CAAAAA9'  | 'invalid sequencing assay: number 9 instead of char for sequencing assay'
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
                'A02P-ABCDEF-CAAAAAA',
                'C02P-BCDEFG-TBBCCDD',
                'A12P-CDEFGH-MCCEGHH',
                'A02R-DEFGHI-CAAAAAA',
        ]
    }
}
