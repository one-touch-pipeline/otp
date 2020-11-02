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
        input                  || project | pid           | sampleTypeDbName             | useSpecificReferenceGenome
        'A02P-ABCDEF-C1AAAAAA' || 'A02P'  | 'A02P-ABCDEF' | 'control-blood-01'           | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        //other funding
        'B02P-ABCDEF-C1AAAAAA' || 'B02P'  | 'B02P-ABCDEF' | 'control-blood-01'           | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        'C02P-ABCDEF-C1AAAAAA' || 'C02P'  | 'C02P-ABCDEF' | 'control-blood-01'           | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        //other project numbersnumbers
        'A08P-ABCDEF-C1AAAAAA' || 'A08P'  | 'A08P-ABCDEF' | 'control-blood-01'           | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        'A35P-ABCDEF-C1AAAAAA' || 'A35P'  | 'A35P-ABCDEF' | 'control-blood-01'           | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        //retrospective project
        'A02R-ABCDEF-C1AAAAAA' || 'A02R'  | 'A02R-ABCDEF' | 'control-blood-01'           | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        //other pseudonyms
        'A02P-EFGHIJ-C1AAAAAA' || 'A02P'  | 'A02P-EFGHIJ' | 'control-blood-01'           | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        'A02P-AAAAAA-C1AAAAAA' || 'A02P'  | 'A02P-AAAAAA' | 'control-blood-01'           | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        'A02P-ZZZZZZ-C1AAAAAA' || 'A02P'  | 'A02P-ZZZZZZ' | 'control-blood-01'           | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        //other categories
        'A02P-ABCDEF-T1AAAAAA' || 'A02P'  | 'A02P-ABCDEF' | 'tumor-blood-01'             | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        'A02P-ABCDEF-M1AAAAAA' || 'A02P'  | 'A02P-ABCDEF' | 'metastasis-blood-01'        | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        //other tissues
        'A02P-ABCDEF-C1AAAAAA' || 'A02P'  | 'A02P-ABCDEF' | 'control-blood-01'           | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        'A02P-ABCDEF-C1BAAAAA' || 'A02P'  | 'A02P-ABCDEF' | 'control-liver-01'           | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        'A02P-ABCDEF-C1CAAAAA' || 'A02P'  | 'A02P-ABCDEF' | 'control-pancreas-01'        | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        'A02P-ABCDEF-C1DAAAAA' || 'A02P'  | 'A02P-ABCDEF' | 'control-ovary-01'           | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        'A02P-ABCDEF-C1EAAAAA' || 'A02P'  | 'A02P-ABCDEF' | 'control-brain-01'           | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        'A02P-ABCDEF-C1FAAAAA' || 'A02P'  | 'A02P-ABCDEF' | 'control-prostate-01'        | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        'A02P-ABCDEF-C1GAAAAA' || 'A02P'  | 'A02P-ABCDEF' | 'control-neural-tissue-01'   | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        'A02P-ABCDEF-C1HAAAAA' || 'A02P'  | 'A02P-ABCDEF' | 'control-rectum-01'          | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        'A02P-ABCDEF-C1IAAAAA' || 'A02P'  | 'A02P-ABCDEF' | 'control-small-intestine-01' | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        'A02P-ABCDEF-C1JAAAAA' || 'A02P'  | 'A02P-ABCDEF' | 'control-bone-marrow-01'     | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        'A02P-ABCDEF-C1KAAAAA' || 'A02P'  | 'A02P-ABCDEF' | 'control-bladder-01'         | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        'A02P-ABCDEF-C1LAAAAA' || 'A02P'  | 'A02P-ABCDEF' | 'control-tongue-01'          | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        'A02P-ABCDEF-C1MAAAAA' || 'A02P'  | 'A02P-ABCDEF' | 'control-gingiva-01'         | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        'A02P-ABCDEF-C1NAAAAA' || 'A02P'  | 'A02P-ABCDEF' | 'control-diaphragma-oris-01' | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        'A02P-ABCDEF-C1OAAAAA' || 'A02P'  | 'A02P-ABCDEF' | 'control-lips-01'            | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        'A02P-ABCDEF-C1PAAAAA' || 'A02P'  | 'A02P-ABCDEF' | 'control-lymph-node-01'      | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        'A02P-ABCDEF-C1QAAAAA' || 'A02P'  | 'A02P-ABCDEF' | 'control-colon-01'           | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        'A02P-ABCDEF-C1RAAAAA' || 'A02P'  | 'A02P-ABCDEF' | 'control-sigmoid-colon-01'   | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        'A02P-ABCDEF-C1SAAAAA' || 'A02P'  | 'A02P-ABCDEF' | 'control-adrenal-gland-01'   | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        'A02P-ABCDEF-C1TAAAAA' || 'A02P'  | 'A02P-ABCDEF' | 'control-muscle-01'          | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        'A02P-ABCDEF-C1UAAAAA' || 'A02P'  | 'A02P-ABCDEF' | 'control-skin-01'            | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        'A02P-ABCDEF-C1VAAAAA' || 'A02P'  | 'A02P-ABCDEF' | 'control-lung-01'            | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        'A02P-ABCDEF-C1WAAAAA' || 'A02P'  | 'A02P-ABCDEF' | 'control-fat-tissue-01'      | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        'A02P-ABCDEF-C1ZAAAAA' || 'A02P'  | 'A02P-ABCDEF' | 'control-unknown-01'         | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        //other biol replicate
        'A02P-ABCDEF-C1ACAAAA' || 'A02P'  | 'A02P-ABCDEF' | 'control-blood-03'           | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        'A02P-ABCDEF-C1AHAAAA' || 'A02P'  | 'A02P-ABCDEF' | 'control-blood-08'           | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        'A02P-ABCDEF-C1ANAAAA' || 'A02P'  | 'A02P-ABCDEF' | 'control-blood-14'           | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        'A02P-ABCDEF-C1AZAAAA' || 'A02P'  | 'A02P-ABCDEF' | 'control-blood-26'           | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        //other analyte
        'A02P-ABCDEF-C1AABAAA' || 'A02P'  | 'A02P-ABCDEF' | 'control-blood-01'           | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        'A02P-ABCDEF-C1AAGAAA' || 'A02P'  | 'A02P-ABCDEF' | 'control-blood-01'           | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        'A02P-ABCDEF-C1AAACAA' || 'A02P'  | 'A02P-ABCDEF' | 'control-blood-01'           | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        'A02P-ABCDEF-C1AAAHAA' || 'A02P'  | 'A02P-ABCDEF' | 'control-blood-01'           | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        //other sequencing assay
        'A02P-ABCDEF-C1AAAABA' || 'A02P'  | 'A02P-ABCDEF' | 'control-blood-01'           | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        'A02P-ABCDEF-C1AAAAHA' || 'A02P'  | 'A02P-ABCDEF' | 'control-blood-01'           | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        'A02P-ABCDEF-C1AAAAAC' || 'A02P'  | 'A02P-ABCDEF' | 'control-blood-01'           | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        'A02P-ABCDEF-C1AAAAAH' || 'A02P'  | 'A02P-ABCDEF' | 'control-blood-01'           | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
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
        input                   | problem
        ''                      | 'empty'
        null                    | 'null'
        //invalid project
        'a02P-ABCDEF-C1AAAAAA'  | 'invalid founding: low letter founding'
        '202P-ABCDEF-C1AAAAAA'  | 'invalid founding: number as founding'
        'ABCP-ABCDEF-C1AAAAAA'  | 'invalid project number: letter instead of numbers'
        'A1P-ABCDEF-C1AAAAAA'   | 'invalid project number: only one digit'
        'A123P-ABCDEF-C1AAAAAA' | 'invalid project number: three digits'
        'A12A-ABCDEF-C1AAAAAA'  | 'invalid char for retrospective/ prospective: A'
        'A12Z-ABCDEF-C1AAAAAA'  | 'invalid char for retrospective/ prospective: Z'
        //invalid pseudonym
        'A02P-abcdef-C1AAAAAA'  | 'invalid pseudonym: lowercase letter'
        'A02P-123456-C1AAAAAA'  | 'invalid pseudonym: digit letter'
        'A02P-ABCDEFG-C1AAAAAA' | 'invalid pseudonym: to many letter'
        'A02P-ABCDE-C1AAAAAA'   | 'invalid pseudonym: to less letter'
        //invalid category
        'A02P-ABCDEF-D1AAAAAA'  | 'invalid category: invalid char: D'
        'A02P-ABCDEF-H1AAAAAA'  | 'invalid category: invalid char: H'
        'A02P-ABCDEF-Y1AAAAAA'  | 'invalid category: invalid char: Y'
        //invalid tissue type
        'A02P-ABCDEF-CKAAAAA'   | 'invalid tissue type: invalid char: K'
        'A02P-ABCDEF-CMAAAAA'   | 'invalid tissue type: invalid char: M'
        'A02P-ABCDEF-CPSAAAAAA' | 'invalid tissue type: invalid char: PSA'
        'A02P-ABCDEF-CZZZAAAAA' | 'invalid tissue type: invalid char: ZZZ'
        //invalid bio replicate
        'A02P-ABCDEF-C1AaAAAA'  | 'invalid bio replicate: lower case'
        'A02P-ABCDEF-C1A5AAAA'  | 'invalid bio replicate: digit'
        //invalid analyte
        'A02P-ABCDEF-C1AAaAA'   | 'invalid analyte type: lower case for technical replicate'
        'A02P-ABCDEF-C1AA3AAA'  | 'invalid analyte type: number for analyte'
        'A02P-ABCDEF-C1AAA1AA'  | 'invalid analyte type: number 1 instead of char for replicate'
        'A02P-ABCDEF-C1AAA9AA'  | 'invalid analyte type: number 9 instead of char for replicate'
        //invalid sequencing assay
        'A02P-ABCDEF-C1AAAAaA'  | 'invalid sequencing assay: lower case for sequencing assay'
        'A02P-ABCDEF-C1AAAA5A'  | 'invalid sequencing assay: number for sequencing assay'
        'A02P-ABCDEF-C1AAAAA1'  | 'invalid sequencing assay: number 1 instead of char for sequencing assay'
        'A02P-ABCDEF-C1AAAAA9'  | 'invalid sequencing assay: number 9 instead of char for sequencing assay'
    }

    @Unroll
    void "test tryParseSingleCellWellLabel is not implemented and always returns null (#identifier)"() {
        given:
        String singleCellWellLabel

        when:
        singleCellWellLabel = parser.tryParseSingleCellWellLabel(identifier)

        then:
        singleCellWellLabel == null

        where:
        identifier << [
                'A02P-ABCDEF-C1AAAAAA',
                'C02P-BCDEFG-T1BBCCDD',
                'A12P-CDEFGH-M1CCEGHH',
                'A02R-DEFGHI-C1AAAAAA',
        ]
    }
}
