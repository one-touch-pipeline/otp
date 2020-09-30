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
package de.dkfz.tbi.otp.parser.itccp4

import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.ngsdata.SampleType
import de.dkfz.tbi.otp.parser.DefaultParsedSampleIdentifier

class ITCC_4P_ParserSpec extends Specification {

    ITCC_4P_Parser itcc4PParser = new ITCC_4P_Parser()

    @Unroll('ITCC_4P identifier #input is parsed to PID #pid, sample type name #sampleTypeDbName')
    void "parse valid input"() {
        given:
        DefaultParsedSampleIdentifier defaultParsedSampleIdentifier
        boolean validPid

        when:
        defaultParsedSampleIdentifier = itcc4PParser.tryParse(input)
        validPid = itcc4PParser.tryParsePid(pid)

        then:
        validPid
        defaultParsedSampleIdentifier
        defaultParsedSampleIdentifier.projectName == 'OE0290_ITCC-P4'
        defaultParsedSampleIdentifier.pid == pid
        defaultParsedSampleIdentifier.sampleTypeDbName == sampleTypeDbName
        defaultParsedSampleIdentifier.fullSampleName == input
        defaultParsedSampleIdentifier.useSpecificReferenceGenome == useSpecificReferenceGenome

        where:
        input                                 || pid              | sampleTypeDbName | useSpecificReferenceGenome
        'ITCC-P4_s01_MB0001_TP01_F01_D01'     || 'ITCC-P4_MB0001' | 'TP01-F01-D01'   | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        //different source numbers
        'ITCC-P4_s02_MB0001_TP01_F01_D01'     || 'ITCC-P4_MB0001' | 'TP01-F01-D01'   | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        'ITCC-P4_s21_MB0001_TP01_F01_D01'     || 'ITCC-P4_MB0001' | 'TP01-F01-D01'   | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        //different disease chars
        'ITCC-P4_s01_NB0001_TP01_F01_D01'     || 'ITCC-P4_NB0001' | 'TP01-F01-D01'   | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        'ITCC-P4_s01_NS0001_TP01_F01_D01'     || 'ITCC-P4_NS0001' | 'TP01-F01-D01'   | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        'ITCC-P4_s01_RS0001_TP01_F01_D01'     || 'ITCC-P4_RS0001' | 'TP01-F01-D01'   | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        'ITCC-P4_s01_SS0001_TP01_F01_D01'     || 'ITCC-P4_SS0001' | 'TP01-F01-D01'   | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        'ITCC-P4_s01_ES0001_TP01_F01_D01'     || 'ITCC-P4_ES0001' | 'TP01-F01-D01'   | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        'ITCC-P4_s01_OS0001_TP01_F01_D01'     || 'ITCC-P4_OS0001' | 'TP01-F01-D01'   | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        'ITCC-P4_s01_RT0001_TP01_F01_D01'     || 'ITCC-P4_RT0001' | 'TP01-F01-D01'   | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        'ITCC-P4_s01_HG0001_TP01_F01_D01'     || 'ITCC-P4_HG0001' | 'TP01-F01-D01'   | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        'ITCC-P4_s01_EP0001_TP01_F01_D01'     || 'ITCC-P4_EP0001' | 'TP01-F01-D01'   | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        'ITCC-P4_s01_XT0001_TP01_F01_D01'     || 'ITCC-P4_XT0001' | 'TP01-F01-D01'   | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        'ITCC-P4_s01_XX0001_TP01_F01_D01'     || 'ITCC-P4_XX0001' | 'TP01-F01-D01'   | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        //different disease numbers
        'ITCC-P4_s01_MB0002_TP01_F01_D01'     || 'ITCC-P4_MB0002' | 'TP01-F01-D01'   | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        'ITCC-P4_s01_MB0023_TP01_F01_D01'     || 'ITCC-P4_MB0023' | 'TP01-F01-D01'   | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        'ITCC-P4_s01_MB0432_TP01_F01_D01'     || 'ITCC-P4_MB0432' | 'TP01-F01-D01'   | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        'ITCC-P4_s01_MB2345_TP01_F01_D01'     || 'ITCC-P4_MB2345' | 'TP01-F01-D01'   | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        //different sample type chars
        'ITCC-P4_s01_MB0001_NB01_F01_D01'     || 'ITCC-P4_MB0001' | 'NB01-F01-D01'   | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        'ITCC-P4_s01_MB0001_NT01_F01_D01'     || 'ITCC-P4_MB0001' | 'NT01-F01-D01'   | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        'ITCC-P4_s01_MB0001_TR01_F01_D01'     || 'ITCC-P4_MB0001' | 'TR01-F01-D01'   | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        'ITCC-P4_s01_MB0001_TM01_F01_D01'     || 'ITCC-P4_MB0001' | 'TM01-F01-D01'   | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        'ITCC-P4_s01_MB0001_TT01_F01_D01'     || 'ITCC-P4_MB0001' | 'TT01-F01-D01'   | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        'ITCC-P4_s01_MB0001_PP01_F01_D01'     || 'ITCC-P4_MB0001' | 'PP01-F01-D01'   | SampleType.SpecificReferenceGenome.USE_SAMPLE_TYPE_SPECIFIC
        'ITCC-P4_s01_MB0001_PR01_F01_D01'     || 'ITCC-P4_MB0001' | 'PR01-F01-D01'   | SampleType.SpecificReferenceGenome.USE_SAMPLE_TYPE_SPECIFIC
        'ITCC-P4_s01_MB0001_PM01_F01_D01'     || 'ITCC-P4_MB0001' | 'PM01-F01-D01'   | SampleType.SpecificReferenceGenome.USE_SAMPLE_TYPE_SPECIFIC
        'ITCC-P4_s01_MB0001_PT01_F01_D01'     || 'ITCC-P4_MB0001' | 'PT01-F01-D01'   | SampleType.SpecificReferenceGenome.USE_SAMPLE_TYPE_SPECIFIC
        'ITCC-P4_s01_MB0001_OP01_F01_D01'     || 'ITCC-P4_MB0001' | 'OP01-F01-D01'   | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        'ITCC-P4_s01_MB0001_OR01_F01_D01'     || 'ITCC-P4_MB0001' | 'OR01-F01-D01'   | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        'ITCC-P4_s01_MB0001_OM01_F01_D01'     || 'ITCC-P4_MB0001' | 'OM01-F01-D01'   | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        'ITCC-P4_s01_MB0001_OT01_F01_D01'     || 'ITCC-P4_MB0001' | 'OT01-F01-D01'   | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        'ITCC-P4_s01_MB0001_CP01_F01_D01'     || 'ITCC-P4_MB0001' | 'CP01-F01-D01'   | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        'ITCC-P4_s01_MB0001_CR01_F01_D01'     || 'ITCC-P4_MB0001' | 'CR01-F01-D01'   | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        'ITCC-P4_s01_MB0001_CM01_F01_D01'     || 'ITCC-P4_MB0001' | 'CM01-F01-D01'   | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        'ITCC-P4_s01_MB0001_CT01_F01_D01'     || 'ITCC-P4_MB0001' | 'CT01-F01-D01'   | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        'ITCC-P4_s01_MB0001_GE01_F01_D01'     || 'ITCC-P4_MB0001' | 'GE01-F01-D01'   | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        'ITCC-P4_s01_MB0001_XU01_F01_D01'     || 'ITCC-P4_MB0001' | 'XU01-F01-D01'   | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        'ITCC-P4_s01_MB0001_PU01_F01_D01'     || 'ITCC-P4_MB0001' | 'PU01-F01-D01'   | SampleType.SpecificReferenceGenome.USE_SAMPLE_TYPE_SPECIFIC
        'ITCC-P4_s01_MB0001_OU01_F01_D01'     || 'ITCC-P4_MB0001' | 'OU01-F01-D01'   | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        'ITCC-P4_s01_MB0001_CU01_F01_D01'     || 'ITCC-P4_MB0001' | 'CU01-F01-D01'   | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        //different sample type numbers
        'ITCC-P4_s01_MB0001_TP02_F01_D01'     || 'ITCC-P4_MB0001' | 'TP02-F01-D01'   | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        'ITCC-P4_s01_MB0001_TP45_F01_D01'     || 'ITCC-P4_MB0001' | 'TP45-F01-D01'   | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        //different material type chars
        'ITCC-P4_s01_MB0001_TP01_E01_D01'     || 'ITCC-P4_MB0001' | 'TP01-E01-D01'   | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        //different material type numbers
        'ITCC-P4_s01_MB0001_TP01_F02_D01'     || 'ITCC-P4_MB0001' | 'TP01-F02-D01'   | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        'ITCC-P4_s01_MB0001_TP01_F32_D01'     || 'ITCC-P4_MB0001' | 'TP01-F32-D01'   | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        //different isolate type chars
        'ITCC-P4_s01_MB0001_TP01_F01_R01'     || 'ITCC-P4_MB0001' | 'TP01-F01-R01'   | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        'ITCC-P4_s01_MB0001_TP01_F01_P01'     || 'ITCC-P4_MB0001' | 'TP01-F01-P01'   | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        //different isolate type numbers
        'ITCC-P4_s01_MB0001_TP01_F01_D02'     || 'ITCC-P4_MB0001' | 'TP01-F01-D02'   | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        'ITCC-P4_s01_MB0001_TP01_F01_D23'     || 'ITCC-P4_MB0001' | 'TP01-F01-D23'   | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        //with analyse type
        'ITCC-P4_s01_MB0001_TP01_F01_D01_01'  || 'ITCC-P4_MB0001' | 'TP01-F01-D01'   | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        'ITCC-P4_s01_MB0001_TP01_F01_D01_02'  || 'ITCC-P4_MB0001' | 'TP01-F01-D01'   | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        'ITCC-P4_s01_MB0001_TP01_F01_D01_23'  || 'ITCC-P4_MB0001' | 'TP01-F01-D01'   | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        'ITCC-P4_s01_MB0001_TP01_F01_D01_a45' || 'ITCC-P4_MB0001' | 'TP01-F01-D01'   | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
    }

    @Unroll
    void "parse invalid input #input (problem: #problem)"() {
        given:
        DefaultParsedSampleIdentifier defaultParsedSampleIdentifier

        when:
        defaultParsedSampleIdentifier = itcc4PParser.tryParse(input)

        then:
        defaultParsedSampleIdentifier == null

        where:
        input                              | problem
        ''                                 | 'empty'
        null                               | 'null'
        //invalid project
        'ITTC-P4_s01_MB0001_TP01_F01_D01'  | 'Invalid Project: ITTC-P4'
        'ITC-P4_s01_MB0001_TP01_F01_D01'   | 'Invalid Project: ITC-P4'
        'ITCCX-P4_s01_MB0001_TP01_F01_D01' | 'Invalid Project: ITCCX-P4'
        'ITCC-P3_s01_MB0001_TP01_F01_D01'  | 'Invalid Project: ITCC-P3'
        'ITCC-Q4_s01_MB0001_TP01_F01_D01'  | 'Invalid Project: ITCC-Q4'
        //invalid center
        'ITCC-P4_a01_MB0001_TP01_F01_D01'  | 'center does not start with a s: a01'
        'ITCC-P4_z01_MB0001_TP01_F01_D01'  | 'center does not start with a s: z01'
        'ITCC-P4_ss1_MB0001_TP01_F01_D01'  | 'center does not start with a s: ss1'
        'ITCC-P4_s0_MB0001_TP01_F01_D01'   | 'center only contains one digit: s0'
        'ITCC-P4_s021_MB0001_TP01_F01_D01' | 'center contains three digits: s021'
        //invalid disease
        'ITCC-P4_s01_M0001_TP01_F01_D01'   | 'disease only contains one char'
        'ITCC-P4_s01_MBB0001_TP01_F01_D01' | 'disease contains three char'
        'ITCC-P4_s01_MB1_TP01_F01_D01'     | 'disease only contains one digit'
        'ITCC-P4_s01_MB01_TP01_F01_D01'    | 'disease contains two digits'
        'ITCC-P4_s01_M0001_TP01_F01_D01'   | 'disease contains three digits'
        'ITCC-P4_s01_M2B0001_TP01_F01_D01' | 'disease contains five digits'
        //invalid sampleType
        'ITCC-P4_s01_MB0001_T01_F01_D01'   | 'sampleType only contains one char'
        'ITCC-P4_s01_MB0001_TPP01_F01_D01' | 'sampleType contains three chars'
        'ITCC-P4_s01_MB0001_TP1_F01_D01'   | 'sampleType only contains one digit'
        'ITCC-P4_s01_MB0001_TP201_F01_D01' | 'sampleType contains three digits'
        //invalid materialType
        'ITCC-P4_s01_MB0001_TP01_FF01_D01' | 'materialType contains two chars'
        'ITCC-P4_s01_MB0001_TP01_F1_D01'   | 'materialType only contains one digit'
        'ITCC-P4_s01_MB0001_TP01_F101_D01' | 'materialType contains three digits'
        //invalid isolateType
        'ITCC-P4_s01_MB0001_TP01_F01_DD01' | 'isolateType contains two chars'
        'ITCC-P4_s01_MB0001_TP01_F01_D1'   | 'isolateType only contains one digit'
        'ITCC-P4_s01_MB0001_TP01_F01_D101' | 'isolateType contains three digits'
    }

    @Unroll
    void "test tryParseSingleCellWellLabel is not implemented and always returns null (#identifier)"() {
        given:
        String singleCellWellLabel

        when:
        singleCellWellLabel = itcc4PParser.tryParseSingleCellWellLabel(identifier)

        then:
        singleCellWellLabel == null

        where:
        identifier << [
                'ITCC-P4_s01_MB0001_TP01_F01_D01',
                'ITCC-P4_s02_MB0001_TP01_F01_D01',
                'ITCC-P4_s01_MB0002_TP01_F01_D01',
                'ITCC-P4_s01_MB0001_TP02_F01_D01',
                'ITCC-P4_s01_MB0001_TP01_F02_D01',
                'ITCC-P4_s01_MB0001_TP01_F01_D02',
        ]
    }
}
