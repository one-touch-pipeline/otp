package de.dkfz.tbi.otp.ngsdata

import spock.lang.Specification
import spock.lang.Unroll

class DeepSampleIdentifierParserSpec extends Specification{
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

    @Unroll('DEEP identifier #input is parsed to PID #pid, sample type name #sampleTypeDbName and Full Sample Name #fullSampleName')
    void "test parsing valid input"() {
        given:
        DefaultParsedSampleIdentifier defaultParsedSampleIdentifier

        when:
        defaultParsedSampleIdentifier = deepSampleIdentifierParser.tryParse(input)

        then:
        defaultParsedSampleIdentifier?.projectName == 'DEEP'
        defaultParsedSampleIdentifier?.pid == pid
        defaultParsedSampleIdentifier?.sampleTypeDbName == sampleTypeDbName
        defaultParsedSampleIdentifier?.fullSampleName == fullSampleName

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
}
