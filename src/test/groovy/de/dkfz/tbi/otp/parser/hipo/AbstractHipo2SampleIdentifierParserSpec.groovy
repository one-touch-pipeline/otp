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
package de.dkfz.tbi.otp.parser.hipo

import grails.testing.gorm.DataTest
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.ngsdata.SampleType
import de.dkfz.tbi.otp.parser.ParsedSampleIdentifier
import de.dkfz.tbi.otp.parser.SampleIdentifierParser

abstract class AbstractHipo2SampleIdentifierParserSpec extends Specification implements DataTest, DomainFactoryCore {

    abstract SampleIdentifierParser getParser()

    abstract String getValidProjectPart()

    abstract String getProjectName()

    String transformTissueNumber(String tissueNumber) {
        return tissueNumber
    }

    @Override
    Class[] getDomainClassesToMock() {
        return [
                ProcessingOption,
        ]
    }

    @Unroll
    void 'tryParse, when identifier is #identifier, parses correctly'() {
        given:
        String fullIdentifier = "${validProjectPart}-${identifier}"
        String tissueNumber = transformTissueNumber(tNum)
        String analytePart = aNum ? "-${aNum}" : ""
        String sampleTypeDbName = "${sampleType}${tissueNumber}${analytePart}"

        when:
        ParsedSampleIdentifier parsed = parser.tryParse(fullIdentifier)
        boolean validPid = parser.tryParsePid(fullIdentifier.split("-")[0, 1].join("-"))

        then:
        validPid
        parsed.projectName == projectName
        parsed.pid == fullIdentifier.split("-")[0, 1].join("-")
        parsed.sampleTypeDbName == sampleTypeDbName
        parsed.fullSampleName == fullIdentifier
        parsed.useSpecificReferenceGenome == specificReferenceGenome

        where:
        identifier         || sampleType                         | tNum | aNum   | specificReferenceGenome
        '123ABC-N0-D1'     || 'control'                          | '0'  | '01'   | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT

        //different pids
        '123ABC-T0-D1'     || 'tumor'                            | '0'  | '01'   | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        '123456-T0-D1'     || 'tumor'                            | '0'  | '01'   | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        'ABCDEF-T0-D1'     || 'tumor'                            | '0'  | '01'   | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        '1234-T0-D1'       || 'tumor'                            | '0'  | '01'   | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        'ABCD-T0-D1'       || 'tumor'                            | '0'  | '01'   | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        'AB12-T0-D1'       || 'tumor'                            | '0'  | '01'   | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        '12AB-T0-D1'       || 'tumor'                            | '0'  | '01'   | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT

        //different sample types
        '123ABC-T0-D1'     || 'tumor'                            | '0'  | '01'   | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        '123ABC-M0-D1'     || 'metastasis'                       | '0'  | '01'   | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        '123ABC-S0-D1'     || 'sphere'                           | '0'  | '01'   | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        '123ABC-X0-D1'     || 'xenograft'                        | '0'  | '01'   | SampleType.SpecificReferenceGenome.USE_SAMPLE_TYPE_SPECIFIC
        '123ABC-B0-D1'     || 'blood'                            | '0'  | '01'   | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        '123ABC-N0-D1'     || 'control'                          | '0'  | '01'   | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        '123ABC-C0-D1'     || 'cell'                             | '0'  | '01'   | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        '123ABC-I0-D1'     || 'invasive_margins'                 | '0'  | '01'   | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        '123ABC-P0-D1'     || 'patient_derived_culture'          | '0'  | '01'   | SampleType.SpecificReferenceGenome.USE_SAMPLE_TYPE_SPECIFIC
        '123ABC-Q0-D1'     || 'culture_derived_xenograft'        | '0'  | '01'   | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        '123ABC-L0-D1'     || 'plasma'                           | '0'  | '01'   | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        '123ABC-F0-D1'     || 'buffy_coat'                       | '0'  | '01'   | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        '123ABC-Z0-D1'     || 'normal_sorted_cells'              | '0'  | '01'   | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        '123ABC-E0-D1'     || 'tumor_interval_debulking_surgery' | '0'  | '01'   | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        '123ABC-K0-D1'     || 'external_control'                 | '0'  | '01'   | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        '123ABC-A0-D1'     || 'lymph_nodes'                      | '0'  | '01'   | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        '123ABC-U0-D1'     || 'undefined_neoplasia'              | '0'  | '01'   | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT

        //newly introduced 'P' in the sample name should append '-p' in the sample type
        '123ABC-F0-PD1'    || 'buffy_coat'                       | '0'  | '01-p' | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        'ABCDEF-F2-PR1'    || 'buffy_coat'                       | '2'  | '01-p' | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        'ABCDEF-F2-PD1'    || 'buffy_coat'                       | '2'  | '01-p' | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT

        //different sample type numbers
        '123ABC-T0-D1'     || 'tumor'                            | '0'  | '01'   | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        '123ABC-T1-D1'     || 'tumor'                            | '1'  | '01'   | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        '123ABC-T2-D1'     || 'tumor'                            | '2'  | '01'   | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        '123ABC-T12-D1'    || 'tumor'                            | '12' | '01'   | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        '123ABC-T01-D1'    || 'tumor'                            | '01' | '01'   | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        '123ABC-T00-D1'    || 'tumor'                            | '00' | '01'   | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT

        //different analyte types using digits
        '123ABC-T3-A1'     || 'tumor'                            | '3'  | '01'   | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        '123ABC-T3-B1'     || 'tumor'                            | '3'  | '01'   | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        '123ABC-T3-D1'     || 'tumor'                            | '3'  | '01'   | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        '123ABC-T3-E1'     || 'tumor'                            | '3'  | '01'   | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        '123ABC-T3-L1'     || 'tumor'                            | '3'  | '01'   | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        '123ABC-T3-M1'     || 'tumor'                            | '3'  | '01'   | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        '123ABC-T3-P1'     || 'tumor'                            | '3'  | '01'   | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        '123ABC-T3-R1'     || 'tumor'                            | '3'  | '01'   | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        '123ABC-T3-T1'     || 'tumor'                            | '3'  | '01'   | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        '123ABC-T3-W1'     || 'tumor'                            | '3'  | '01'   | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        '123ABC-T3-Y1'     || 'tumor'                            | '3'  | '01'   | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT

        //different analyte numbers
        '123ABC-T3-L0'     || 'tumor'                            | '3'  | '00'   | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        '123ABC-T3-L1'     || 'tumor'                            | '3'  | '01'   | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        '123ABC-T3-L2'     || 'tumor'                            | '3'  | '02'   | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        '123ABC-T3-L12'    || 'tumor'                            | '3'  | '12'   | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT

        //analyte type: chip seq
        '123ABC-T3-0C00'   || 'tumor'                            | '3'  | null   | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        '123ABC-T3-1C00'   || 'tumor'                            | '3'  | null   | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        '123ABC-T3-0C01'   || 'tumor'                            | '3'  | null   | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        '123ABC-T3-1C02'   || 'tumor'                            | '3'  | null   | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        '123ABC-T3-1C20'   || 'tumor'                            | '3'  | null   | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT

        //analyte type: single cell multiplexed
        '123ABC-T3-G2'     || 'tumor'                            | '3'  | '02'   | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        '123ABC-T3-H2'     || 'tumor'                            | '3'  | '02'   | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        '123ABC-T3-J2'     || 'tumor'                            | '3'  | '02'   | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        '123ABC-T3-S2'     || 'tumor'                            | '3'  | '02'   | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT

        '123ABC-T3-G20'    || 'tumor'                            | '3'  | '20'   | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        '123ABC-T3-H20'    || 'tumor'                            | '3'  | '20'   | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        '123ABC-T3-J20'    || 'tumor'                            | '3'  | '20'   | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        '123ABC-T3-S20'    || 'tumor'                            | '3'  | '20'   | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT

        //analyte type: single cell demultiplexed
        '123ABC-T3-1G2'    || 'tumor'                            | '3'  | null   | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        '123ABC-T3-1H2'    || 'tumor'                            | '3'  | null   | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        '123ABC-T3-1J2'    || 'tumor'                            | '3'  | null   | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        '123ABC-T3-1S2'    || 'tumor'                            | '3'  | null   | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT

        '123ABC-T3-1G20'   || 'tumor'                            | '3'  | null   | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        '123ABC-T3-1H20'   || 'tumor'                            | '3'  | null   | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        '123ABC-T3-1J20'   || 'tumor'                            | '3'  | null   | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        '123ABC-T3-1S20'   || 'tumor'                            | '3'  | null   | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT

        '123ABC-T3-123H20' || 'tumor'                            | '3'  | null   | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        '123ABC-T3-123G20' || 'tumor'                            | '3'  | null   | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        '123ABC-T3-123J20' || 'tumor'                            | '3'  | null   | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
        '123ABC-T3-123S20' || 'tumor'                            | '3'  | null   | SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT
    }

    @Unroll
    void 'tryParse, when identifier is #identifier, returns null'() {
        given:
        String fullIdentifier = "${validProjectPart}-${identifier}"

        expect:
        parser.tryParse(fullIdentifier) == null

        where:
        identifier << [
                //invalid pid
                '123-N0-D1',
                '123AB-N0-D1',
                '123ABCD-N0-D1',

                //invalid sample type
                '123ABC-V0-D1',
                '123ABC-00-D1',

                //invalid sample number
                '123ABC-N-D1',
                '123ABC-N123456-D1',
                '123ABC-NA-D1',

                //invalid analyte type
                '123ABC-N0-Z1',
                '123ABC-N0-01',

                //invalid analyte number
                '123ABC-N0-D',
                'ABCDEF-F2-PA1',
                '123ABC-N0-D123',
                '123ABC-N0-C123',
                '123ABC-N0-123C123',
                '123ABC-N0-C123',
                '123ABC-N0-123C',
                '123ABC-N0-1C123',
                '123ABC-N0-1C0X',
                '123ABC-N0-XC01',
                '123ABC-N0-G123',
                '123ABC-N0-1G',
                '123ABC-N0-1G123',
        ]
    }

    @Unroll
    void "test parsePid invalid input #pid"() {
        when:
        boolean validPid = parser.tryParsePid(pid)

        then:
        !validPid

        where:
        pid  | _
        ''   | _
        null | _
        and: 'Input with invalid pid'
        'INVALID_PID' | _
    }

    @Unroll
    void "test tryParseSingleCellWellLabel valid input #identifier"() {
        given:
        String fullIdentifier = "${validProjectPart}-${identifier}"
        String singleCellWellLabel

        when:
        singleCellWellLabel = parser.tryParseSingleCellWellLabel(fullIdentifier)

        then:
        singleCellWellLabel == expectedSingleCellWellLabel

        where:
        identifier          || expectedSingleCellWellLabel
        "123ABC-T0-1G1"     || "1G1"
        "123ABC-T0-1H1"     || "1H1"
        "123ABC-T0-1J1"     || "1J1"
        "123ABC-T0-1S1"     || "1S1"
        "123ABC-T0-12G3"    || "12G3"
        "123ABC-T3-1J02"    || "1J02"
        "123ABC-T3-1234S02" || "1234S02"
    }

    @Unroll
    void "test tryParseSingleCellWellLabel invalid input #identifier"() {
        given:
        String fullIdentifier = identifier ? "${validProjectPart}-${identifier}" : identifier
        String singleCellWellLabel

        when:
        singleCellWellLabel = parser.tryParseSingleCellWellLabel(fullIdentifier)

        then:
        singleCellWellLabel == null

        where:
        identifier << [
                null,
                "",
                "123ABC-T0-G1",
                "123ABC-T0-H1",
                "123ABC-T0-J1",
                "123ABC-T0-1",
                "123ABC-T0-G1",
                '123ABC-T0-123J',
                '123ABC-T0-1J123',
        ]
    }
}
