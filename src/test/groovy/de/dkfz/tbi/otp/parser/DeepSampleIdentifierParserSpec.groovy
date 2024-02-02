/*
 * Copyright 2011-2024 The OTP authors
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
package de.dkfz.tbi.otp.parser

import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.ngsdata.SampleType

class DeepSampleIdentifierParserSpec extends Specification {

    DeepSampleIdentifierParser deepSampleIdentifierParser = new DeepSampleIdentifierParser()

    void "test parsing invalid input"() {
        given:
        DefaultParsedSampleIdentifier defaultParsedSampleIdentifier

        when:
        defaultParsedSampleIdentifier = deepSampleIdentifierParser.tryParse(input)

        then:
        defaultParsedSampleIdentifier == null

        where:
        input                           | _
        ''                              | _
        null                            | _
        and: 'invalid libraryStrategy'
        '41_Hf01_BlAd_CD_XXXX_S_1'      | _
        and: 'wrong characters between sex and cellLines'
        '02_Mm003T3L1_NNAd_T1_CTCF_R_1' | _
        and: 'wrong character between disease and _libraryStrategy'
        '01_Mm3T3L1_CoAd_T11_CTCF_R_1'  | _
        and: 'wrong character between disease and _libraryStrategy'
        '41_Hf01_BlAd_CD2_WGBS_S_1'     | _
        and: 'wrong characters between subproject_ and cellLines'
        '01_MmHepaRG_LiHR_D11_tRNA_K_1' | _
        and: 'libraryStrategy NOMe is valid but not allowed to be automated'
        '02_HepaRG_InAs_D31_NOMe_S_2'   | _
    }

    @Unroll
    void "test parsePid invalid input #pid"() {
        given:
        boolean validPid

        when:
        validPid = deepSampleIdentifierParser.tryParsePid(pid)

        then:
        !validPid

        where:
        pid                 | _
        ''                  | _
        null                | _
        and: 'invalid libraryStrategy'
        '41_Hf01_BlAd_CD_XXXX_S_1'      | _
        and: 'wrong characters between sex and cellLines'
        '02_Mm003T3L1_NNAd_T1_CTCF_R_1' | _
        and: 'wrong character between disease and _libraryStrategy'
        '01_Mm3T3L1_CoAd_T11_CTCF_R_1'  | _
        and: 'wrong character between disease and _libraryStrategy'
        '41_Hf01_BlAd_CD2_WGBS_S_1'     | _
        and: 'wrong characters between subproject_ and cellLines'
        '01_MmHepaRG_LiHR_D11_tRNA_K_1' | _
        and: 'libraryStrategy NOMe is valid but not allowed to be automated'
        '02_HepaRG_InAs_D31_NOMe_S_2'   | _
    }

    @Unroll('DEEP identifier #input is parsed to PID #pid, sample type name #sampleTypeDbName and Full Sample Name #fullSampleName')
    void "test parsing valid input"() {
        given:
        DefaultParsedSampleIdentifier defaultParsedSampleIdentifier
        boolean validPid

        when:
        defaultParsedSampleIdentifier = deepSampleIdentifierParser.tryParse(input)
        validPid = deepSampleIdentifierParser.tryParsePid(pid)

        then:
        validPid
        defaultParsedSampleIdentifier.projectName == 'DEEP'
        defaultParsedSampleIdentifier.pid == pid
        defaultParsedSampleIdentifier.sampleTypeDbName == sampleTypeDbName
        defaultParsedSampleIdentifier.fullSampleName == fullSampleName
        defaultParsedSampleIdentifier.useSpecificReferenceGenome == SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT

        where:
        input                           || pid                   | sampleTypeDbName  | fullSampleName
        '41_Hf01_BlAd_CD_WGBS_S_1'      || '41_Hf01_BlAd_CD'     | 'Replicate1'      | '41_Hf01_BlAd_CD_WGBS_S_1'
        '02_HepaRG_InAs_D11_tRNA_K_1'   || '02_HepaRG_InAs_D11'  | 'Replicate1'      | '02_HepaRG_InAs_D11_tRNA_K_1'
        '02_HepaRG_InAs_D31_DNase_S_2'  || '02_HepaRG_InAs_D31'  | 'Replicate2'      | '02_HepaRG_InAs_D31_DNase_S_2'
        '02_HepaRG_InAs_D31_WGBS_S_2'   || '02_HepaRG_InAs_D31'  | 'Replicate1'      | '02_HepaRG_InAs_D31_WGBS_S_2'
        '01_Mm3T3L1_CoAd_T1_CTCF_R_1'   || '01_Mm3T3L1_CoAd_T1'  | 'Replicate1'      | '01_Mm3T3L1_CoAd_T1_CTCF_R_1'
        '02_Hf3T3L1_NNAd_T1_CTCF_R_1'   || '02_Hf3T3L1_NNAd_T1'  | 'Replicate1'      | '02_Hf3T3L1_NNAd_T1_CTCF_R_1'
        '02_Hf3T3L1_NNAs_T1_CTCF_R_1'   || '02_Hf3T3L1_NNAs_T1'  | 'Replicate1'      | '02_Hf3T3L1_NNAs_T1_CTCF_R_1'
        '02_Hf3T3L1_NNAl_T1_CTCF_R_1'   || '02_Hf3T3L1_NNAl_T1'  | 'Replicate1'      | '02_Hf3T3L1_NNAl_T1_CTCF_R_1'
    }

    @Unroll
    void "test tryParseSingleCellWellLabel is not implemented and always returns null"() {
        given:
        String singleCellWellLabel

        when:
        singleCellWellLabel = deepSampleIdentifierParser.tryParseSingleCellWellLabel(identifier)

        then:
        singleCellWellLabel == null

        where:
        identifier << [
                '41_Hf01_BlAd_CD_XXXX_S_1',
                '02_Mm003T3L1_NNAd_T1_CTCF_R_1',
                '41_Hf01_BlAd_CD_WGBS_S_1',
                '02_Hf3T3L1_NNAd_T1_CTCF_R_1',
        ]
    }
}
