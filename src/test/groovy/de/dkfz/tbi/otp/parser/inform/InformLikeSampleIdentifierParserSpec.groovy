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
package de.dkfz.tbi.otp.parser.inform

import grails.testing.gorm.DataTest
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.parser.DefaultParsedSampleIdentifier

class InformLikeSampleIdentifierParserSpec extends Specification implements DataTest, DomainFactoryCore {

    InformLikeSampleIdentifierParser informLikeSampleIdentifierParser = new InformLikeSampleIdentifierParser()

    @Override
    Class[] getDomainClassesToMock() {
        return [
                ProcessingOption,
        ]
    }

    void setup() {
        findOrCreateProcessingOption(
                name: ProcessingOption.OptionName.INFORM_LIKE_PARSER_MAPPING,
                value: '{\n' +
                        '   "I":"INFORM1",\n' +
                        '   "AB":"AB",\n' +
                        '   "MSP":"MSP",\n' +
                        '   "MPSA":"MPSA"\n' +
                        '}',
        )
        informLikeSampleIdentifierParser.processingOptionService = new ProcessingOptionService()
    }

    @Unroll('INFORM like identifier #input is parsed to PID #pid, sample type name #sampleTypeDbName')
    void "test parse valid input"() {
        given:
        setup()
        DefaultParsedSampleIdentifier defaultParsedSampleIdentifier

        when:
        defaultParsedSampleIdentifier = informLikeSampleIdentifierParser.tryParse(input)

        then:
        defaultParsedSampleIdentifier.projectName == 'INFORM1' || defaultParsedSampleIdentifier.projectName == 'MSP' ||
                defaultParsedSampleIdentifier.projectName == 'AB'
        defaultParsedSampleIdentifier.pid == pid
        defaultParsedSampleIdentifier.sampleTypeDbName == sampleTypeDbName
        defaultParsedSampleIdentifier.fullSampleName == input

        where:
        input               || pid          | sampleTypeDbName
        'I123_456_1T2_D3'   || 'I123_456'   | 'tumor012-03'
        // different numbers in first group
        'I456_456_1T2_D3'   || 'I456_456'   | 'tumor012-03'
        'I789_456_1T2_D3'   || 'I789_456'   | 'tumor012-03'
        // different numbers in second group
        'I123_789_1T2_D3'   || 'I123_789'   | 'tumor012-03'
        'I123_123_1T2_D3'   || 'I123_123'   | 'tumor012-03'
        // different tissue types
        'I123_456_1M2_D3'   || 'I123_456'   | 'metastasis012-03'
        'I123_456_1C2_D3'   || 'I123_456'   | 'control012-03'
        'I123_456_1F2_D3'   || 'I123_456'   | 'ffpe012-03'
        'I123_456_1P2_D3'   || 'I123_456'   | 'pdx012-03'
        'I123_456_1L2_D3'   || 'I123_456'   | 'plasma012-03'
        'I123_456_1X2_D3'   || 'I123_456'   | 'other012-03'
        // different numbers for tissue type
        'I123_456_2T2_D3'   || 'I123_456'   | 'tumor022-03'
        'I123_456_3T2_D3'   || 'I123_456'   | 'tumor032-03'
        'I123_456_4T2_D3'   || 'I123_456'   | 'tumor042-03'
        'I123_456_10T2_D3'  || 'I123_456'   | 'tumor102-03'
        'I123_456_23T2_D3'  || 'I123_456'   | 'tumor232-03'
        'I123_456_78T2_D3'  || 'I123_456'   | 'tumor782-03'
        // different order numbers
        'I123_456_1T1_D3'   || 'I123_456'   | 'tumor011-03'
        'I123_456_1T3_D3'   || 'I123_456'   | 'tumor013-03'
        'I123_456_1T4_D3'   || 'I123_456'   | 'tumor014-03'
        // different second order numbers
        'I123_456_1T2_D1'   || 'I123_456'   | 'tumor012-01'
        'I123_456_1T2_D2'   || 'I123_456'   | 'tumor012-02'
        'I123_456_1T2_D4'   || 'I123_456'   | 'tumor012-04'
        'I123_456_1T2_D10'  || 'I123_456'   | 'tumor012-10'
        'I123_456_1T2_D23'  || 'I123_456'   | 'tumor012-23'
        'I123_456_1T2_D56'  || 'I123_456'   | 'tumor012-56'
        // tissue number X
        'I123_456_XT1_D3'   || 'I123_456'   | 'tumor0X1-03'
        'I123_456_XT2_D3'   || 'I123_456'   | 'tumor0X2-03'
        'I123_456_XT1_D4'   || 'I123_456'   | 'tumor0X1-04'
        'I123_456_XT2_D5'   || 'I123_456'   | 'tumor0X2-05'
        // 2 chars project prefix is allowed
        'AB999_888_1F2_D3'  || 'AB999_888'  | 'ffpe012-03'
        // 3 chars project prefix is allowed
        'MSP123_456_1T2_D3' || 'MSP123_456' | 'tumor012-03'
        'MSP999_888_1F2_D3' || 'MSP999_888' | 'ffpe012-03'
    }

    @Unroll
    void "test parse invalid input #input (problem: #problem)"() {
        given:
        setup()
        DefaultParsedSampleIdentifier defaultParsedSampleIdentifier

        when:
        defaultParsedSampleIdentifier = informLikeSampleIdentifierParser.tryParse(input)

        then:
        defaultParsedSampleIdentifier == null

        where:
        input                | problem
        ''                   | 'empty'
        null                 | 'null'
        'Z123_456_1T1_D1'    | 'This project prefix is not allowed/set in the processing options'
        'MPSA999_888_1F2_D3' | 'This project prefix is not allowed because it is 4 chars long even though it was set in the processing options'
        'ABCD999_888_1F2_D3' | 'This project prefix is not allowed because it is 4 chars long'
        'IA23_456_1T1_D1'    | '2. letter is char'
        'I1A3_456_1T1_D1'    | '3. letter is char'
        'I12A_456_1T1_D1'    | '4. letter is char'
        'I123_A56_0T1_D1'    | '6. letter is char'
        'I123_4A6_0T1_D1'    | '7. letter is char'
        'I123_45A_0T1_D1'    | '8. letter is char'
        'I123_456_1Z1_D1'    | 'Input with invalid tissueTypeKey'
        'I123_456_AT1_D1'    | '10. letter is char but not X'
        'I123_456_0TA_D1'    | '12. letter is char but not X'
        'I123_456_0T1_DA'    | '15. letter is char but not X'
        'I12_456_1T1_D1'     | 'First group is too short'
        'I1234_456_1T1_D1'   | 'First group is too long'
        'I123_45_1T1_D1'     | 'Second group is too short'
        'I123_4567_1T1_D1'   | 'Second group is too long'
        'I123_456_123T1_D1'  | 'tissue number has three digits'
        'I123_456_1T11_D1'   | 'order number has two digits'
        'I123_456_1T1_D123'  | 'second order number has three digits'
    }

    @Unroll
    void "test tryParseSingleCellWellLabel is not implemented and always returns null (#identifier)"() {
        given:
        setup()
        String singleCellWellLabel

        when:
        singleCellWellLabel = informLikeSampleIdentifierParser.tryParseSingleCellWellLabel(identifier)

        then:
        singleCellWellLabel == null

        where:
        identifier << [
                'I123_456_2T1_D1',
                'I123_456_0F1_D1',
                'Z123_456_1T1_D1',
                'I124_456_2T0_D1',
                'MSP123_456_1T2_D4',
        ]
    }
}
