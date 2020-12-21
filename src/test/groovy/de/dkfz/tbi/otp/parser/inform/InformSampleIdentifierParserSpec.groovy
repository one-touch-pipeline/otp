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
package de.dkfz.tbi.otp.parser.inform

import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.parser.DefaultParsedSampleIdentifier

import static de.dkfz.tbi.otp.ngsdata.SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
import static de.dkfz.tbi.otp.ngsdata.SampleType.SpecificReferenceGenome.USE_SAMPLE_TYPE_SPECIFIC

class InformSampleIdentifierParserSpec extends Specification {

    InformSampleIdentifierParser informSampleIdentifierParser = new InformSampleIdentifierParser()

    @Unroll('INFORM identifier #input is parsed to PID #pid, sample type name #sampleTypeDbName')
    void "test parse valid input"() {
        given:
        DefaultParsedSampleIdentifier defaultParsedSampleIdentifier
        boolean validPid

        when:
        defaultParsedSampleIdentifier = informSampleIdentifierParser.tryParse(input)
        validPid = informSampleIdentifierParser.tryParsePid(pid)

        then:
        validPid
        defaultParsedSampleIdentifier.projectName == 'INFORM1'
        defaultParsedSampleIdentifier.pid == pid
        defaultParsedSampleIdentifier.sampleTypeDbName == sampleTypeDbName
        defaultParsedSampleIdentifier.fullSampleName == input
        defaultParsedSampleIdentifier.useSpecificReferenceGenome == useSpecificReferenceGenome

        where:
        input              || pid        | sampleTypeDbName   | useSpecificReferenceGenome
        'I123_456_1T2_D3'  || 'I123_456' | 'tumor012-03'      | USE_PROJECT_DEFAULT
        //different numbers in first group
        'I456_456_1T2_D3'  || 'I456_456' | 'tumor012-03'      | USE_PROJECT_DEFAULT
        'I789_456_1T2_D3'  || 'I789_456' | 'tumor012-03'      | USE_PROJECT_DEFAULT
        //different numbers in second group
        'I123_789_1T2_D3'  || 'I123_789' | 'tumor012-03'      | USE_PROJECT_DEFAULT
        'I123_123_1T2_D3'  || 'I123_123' | 'tumor012-03'      | USE_PROJECT_DEFAULT
        //different tissue types
        'I123_456_1M2_D3'  || 'I123_456' | 'metastasis012-03' | USE_PROJECT_DEFAULT
        'I123_456_1C2_D3'  || 'I123_456' | 'control012-03'    | USE_PROJECT_DEFAULT
        'I123_456_1F2_D3'  || 'I123_456' | 'ffpe012-03'       | USE_PROJECT_DEFAULT
        'I123_456_1P2_D3'  || 'I123_456' | 'pdx012-03'        | USE_SAMPLE_TYPE_SPECIFIC
        'I123_456_1L2_D3'  || 'I123_456' | 'plasma012-03'     | USE_PROJECT_DEFAULT
        'I123_456_1X2_D3'  || 'I123_456' | 'other012-03'      | USE_PROJECT_DEFAULT
        //different numbers for tissue type
        'I123_456_2T2_D3'  || 'I123_456' | 'tumor022-03'      | USE_PROJECT_DEFAULT
        'I123_456_3T2_D3'  || 'I123_456' | 'tumor032-03'      | USE_PROJECT_DEFAULT
        'I123_456_4T2_D3'  || 'I123_456' | 'tumor042-03'      | USE_PROJECT_DEFAULT
        'I123_456_10T2_D3' || 'I123_456' | 'tumor102-03'      | USE_PROJECT_DEFAULT
        'I123_456_23T2_D3' || 'I123_456' | 'tumor232-03'      | USE_PROJECT_DEFAULT
        'I123_456_78T2_D3' || 'I123_456' | 'tumor782-03'      | USE_PROJECT_DEFAULT
        //different order numbers
        'I123_456_1T1_D3'  || 'I123_456' | 'tumor011-03'      | USE_PROJECT_DEFAULT
        'I123_456_1T3_D3'  || 'I123_456' | 'tumor013-03'      | USE_PROJECT_DEFAULT
        'I123_456_1T4_D3'  || 'I123_456' | 'tumor014-03'      | USE_PROJECT_DEFAULT
        //different second order numbers
        'I123_456_1T2_D1'  || 'I123_456' | 'tumor012-01'      | USE_PROJECT_DEFAULT
        'I123_456_1T2_D2'  || 'I123_456' | 'tumor012-02'      | USE_PROJECT_DEFAULT
        'I123_456_1T2_D4'  || 'I123_456' | 'tumor012-04'      | USE_PROJECT_DEFAULT
        'I123_456_1T2_D10' || 'I123_456' | 'tumor012-10'      | USE_PROJECT_DEFAULT
        'I123_456_1T2_D23' || 'I123_456' | 'tumor012-23'      | USE_PROJECT_DEFAULT
        'I123_456_1T2_D56' || 'I123_456' | 'tumor012-56'      | USE_PROJECT_DEFAULT
        //tissue number X
        'I123_456_XT1_D3'  || 'I123_456' | 'tumor0X1-03'      | USE_PROJECT_DEFAULT
        'I123_456_XT2_D3'  || 'I123_456' | 'tumor0X2-03'      | USE_PROJECT_DEFAULT
        'I123_456_XT1_D4'  || 'I123_456' | 'tumor0X1-04'      | USE_PROJECT_DEFAULT
        'I123_456_XT2_D5'  || 'I123_456' | 'tumor0X2-05'      | USE_PROJECT_DEFAULT
    }

    @Unroll
    void "test parse invalid input #input (problem: #problem)"() {
        given:
        DefaultParsedSampleIdentifier defaultParsedSampleIdentifier

        when:
        defaultParsedSampleIdentifier = informSampleIdentifierParser.tryParse(input)

        then:
        defaultParsedSampleIdentifier == null

        where:
        input               | problem
        ''                  | 'empty'
        null                | 'null'
        'Z123_456_1T1_D1'   | 'First char is not an "I"'
        'IA23_456_1T1_D1'   | '2. letter is char'
        'I1A3_456_1T1_D1'   | '3. letter is char'
        'I12A_456_1T1_D1'   | '4. letter is char'
        'I123_A56_0T1_D1'   | '6. letter is char'
        'I123_4A6_0T1_D1'   | '7. letter is char'
        'I123_45A_0T1_D1'   | '8. letter is char'
        'I123_456_1Z1_D1'   | 'Input with invalid tissueTypeKey'
        'I123_456_AT1_D1'   | '10. letter is char but not X'
        'I123_456_0TA_D1'   | '12. letter is char but not X'
        'I123_456_0T1_DA'   | '15. letter is char but not X'
        'I12_456_1T1_D1'    | 'First group is too short'
        'I1234_456_1T1_D1'  | 'First group is too long'
        'I123_45_1T1_D1'    | 'Second group is too short'
        'I123_4567_1T1_D1'  | 'Second group is too long'
        'I123_456_123T1_D1' | 'tissue number has three digits'
        'I123_456_1T11_D1'  | 'order number has two digits'
        'I123_456_1T1_D123' | 'second order number has three digits'
    }

    @Unroll
    void "test tryParseSingleCellWellLabel is not implemented and always returns null (#identifier)"() {
        given:
        String singleCellWellLabel

        when:
        singleCellWellLabel = informSampleIdentifierParser.tryParseSingleCellWellLabel(identifier)

        then:
        singleCellWellLabel == null

        where:
        identifier << [
                'I123_456_2T1_D1',
                'I123_456_0F1_D1',
                'Z123_456_1T1_D1',
                'I124_456_2T0_D1',
        ]
    }
}
