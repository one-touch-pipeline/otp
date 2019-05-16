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

package de.dkfz.tbi.otp.hipo

import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.ngsdata.ParsedSampleIdentifier

class Hipo2SampleIdentifierParserSpec extends Specification {

    Hipo2SampleIdentifierParser parser = new Hipo2SampleIdentifierParser()

    @SuppressWarnings(['CyclomaticComplexity', 'MethodSize'])
    @Unroll
    void 'tryParse, when identifier is #identifier, parses correctly'() {
        when:
        ParsedSampleIdentifier parsed = parser.tryParse(identifier)
        boolean validPid = parser.tryParsePid(identifier.split("-")[0, 1].join("-"))

        then:
        validPid
        parsed.projectName == "hipo_${identifier.substring(0, 4)}"
        parsed.pid == identifier.split("-")[0, 1].join("-")
        parsed.sampleTypeDbName == sampleTypeDbName
        parsed.fullSampleName == identifier

        where:
        identifier              || sampleTypeDbName
        'K12A-123ABC-N0-D1'     || 'CONTROL0-01'

        //different project prefixes
        'S12A-123ABC-T0-D1'     || 'TUMOR0-01'
        'T12A-123ABC-T0-D1'     || 'TUMOR0-01'
        'A12A-123ABC-T0-D1'     || 'TUMOR0-01'
        'B12A-123ABC-T0-D1'     || 'TUMOR0-01'
        'H12A-123ABC-T0-D1'     || 'TUMOR0-01'
        'P12A-123ABC-T0-D1'     || 'TUMOR0-01'
        'K123-123ABC-T0-D1'     || 'TUMOR0-01'

        //different pids
        'K12A-123ABC-T0-D1'     || 'TUMOR0-01'
        'K12A-123456-T0-D1'     || 'TUMOR0-01'
        'K12A-ABCDEF-T0-D1'     || 'TUMOR0-01'
        'K12A-1234-T0-D1'       || 'TUMOR0-01'
        'K12A-ABCD-T0-D1'       || 'TUMOR0-01'
        'K12A-AB12-T0-D1'       || 'TUMOR0-01'
        'K12A-12AB-T0-D1'       || 'TUMOR0-01'

        //different sample types
        'K12A-123ABC-T0-D1'     || 'TUMOR0-01'
        'K12A-123ABC-M0-D1'     || 'METASTASIS0-01'
        'K12A-123ABC-S0-D1'     || 'SPHERE0-01'
        'K12A-123ABC-X0-D1'     || 'XENOGRAFT0-01'
        'K12A-123ABC-B0-D1'     || 'BLOOD0-01'
        'K12A-123ABC-N0-D1'     || 'CONTROL0-01'
        'K12A-123ABC-C0-D1'     || 'CELL0-01'
        'K12A-123ABC-I0-D1'     || 'INVASIVE_MARGINS0-01'
        'K12A-123ABC-P0-D1'     || 'PATIENT_DERIVED_CULTURE0-01'
        'K12A-123ABC-Q0-D1'     || 'CULTURE_DERIVED_XENOGRAFT0-01'
        'K12A-123ABC-L0-D1'     || 'PLASMA0-01'
        'K12A-123ABC-F0-D1'     || 'BUFFY_COAT0-01'
        'K12A-123ABC-Z0-D1'     || 'NORMAL_SORTED_CELLS0-01'
        'K12A-123ABC-E0-D1'     || 'TUMOR_INTERVAL_DEBULKING_SURGERY0-01'
        'K12A-123ABC-K0-D1'     || 'EXTERNAL_CONTROL0-01'
        'K12A-123ABC-A0-D1'     || 'LYMPH_NODES0-01'

        //different sample type numbers
        'K12A-123ABC-T0-D1'     || 'TUMOR0-01'
        'K12A-123ABC-T1-D1'     || 'TUMOR1-01'
        'K12A-123ABC-T2-D1'     || 'TUMOR2-01'
        'K12A-123ABC-T12-D1'    || 'TUMOR12-01'
        'K12A-123ABC-T01-D1'    || 'TUMOR01-01'
        'K12A-123ABC-T00-D1'    || 'TUMOR00-01'

        //different analyte types using digits
        'K12A-123ABC-T3-A1'     || 'TUMOR3-01'
        'K12A-123ABC-T3-B1'     || 'TUMOR3-01'
        'K12A-123ABC-T3-D1'     || 'TUMOR3-01'
        'K12A-123ABC-T3-E1'     || 'TUMOR3-01'
        'K12A-123ABC-T3-L1'     || 'TUMOR3-01'
        'K12A-123ABC-T3-M1'     || 'TUMOR3-01'
        'K12A-123ABC-T3-P1'     || 'TUMOR3-01'
        'K12A-123ABC-T3-R1'     || 'TUMOR3-01'
        'K12A-123ABC-T3-T1'     || 'TUMOR3-01'
        'K12A-123ABC-T3-W1'     || 'TUMOR3-01'
        'K12A-123ABC-T3-Y1'     || 'TUMOR3-01'

        //different analyte numbers
        'K12A-123ABC-T3-L0'     || 'TUMOR3-00'
        'K12A-123ABC-T3-L1'     || 'TUMOR3-01'
        'K12A-123ABC-T3-L2'     || 'TUMOR3-02'
        'K12A-123ABC-T3-L12'    || 'TUMOR3-12'

        //analyte type: chip seq
        'K12A-123ABC-T3-0C00'   || 'TUMOR3'
        'K12A-123ABC-T3-1C00'   || 'TUMOR3'
        'K12A-123ABC-T3-0C01'   || 'TUMOR3'
        'K12A-123ABC-T3-1C02'   || 'TUMOR3'
        'K12A-123ABC-T3-1C20'   || 'TUMOR3'

        //analyte type: single cell
        'K12A-123ABC-T3-G2'     || 'TUMOR3'
        'K12A-123ABC-T3-H2'     || 'TUMOR3'
        'K12A-123ABC-T3-J2'     || 'TUMOR3'
        'K12A-123ABC-T3-S2'     || 'TUMOR3'

        'K12A-123ABC-T3-G20'    || 'TUMOR3'
        'K12A-123ABC-T3-H20'    || 'TUMOR3'
        'K12A-123ABC-T3-J20'    || 'TUMOR3'
        'K12A-123ABC-T3-S20'    || 'TUMOR3'

        'K12A-123ABC-T3-1G20'   || 'TUMOR3'
        'K12A-123ABC-T3-1H20'   || 'TUMOR3'
        'K12A-123ABC-T3-1J20'   || 'TUMOR3'
        'K12A-123ABC-T3-1S20'   || 'TUMOR3'

        'K12A-123ABC-T3-123H20' || 'TUMOR3'
        'K12A-123ABC-T3-123G20' || 'TUMOR3'
        'K12A-123ABC-T3-123J20' || 'TUMOR3'
        'K12A-123ABC-T3-123S20' || 'TUMOR3'
    }


    @Unroll
    void 'tryParse, when identifier is #identifier, returns null'() {
        expect:
        parser.tryParse(identifier) == null

        where:
        identifier << [
                //invalid project
                'K12-123ABC-N0-D1',
                'K12AB-123ABC-N0-D1',
                '212A-123ABC-N0-D1',
                'KABC-123ABC-N0-D1',

                //invalid pid
                'K12A-123-N0-D1',
                'K12A-123AB-N0-D1',
                'K12A-123ABCD-N0-D1',

                //invalid sample type
                'K12A-123ABC-V0-D1',
                'K12A-123ABC-00-D1',

                //invalid sample number
                'K12A-123ABC-N-D1',
                'K12A-123ABC-N123456-D1',
                'K12A-123ABC-NA-D1',

                //invalid analyte type
                'K12A-123ABC-N0-Z1',
                'K12A-123ABC-N0-01',

                //invalid analyte number
                'K12A-123ABC-N0-D',
                'K12A-123ABC-N0-D123',
                'K12A-123ABC-N0-C123',
                'K12A-123ABC-N0-123C123',
                'K12A-123ABC-N0-C123',
                'K12A-123ABC-N0-123C',
                'K12A-123ABC-N0-1C123',
                'K12A-123ABC-N0-1C0X',
                'K12A-123ABC-N0-XC01',
                'K12A-123ABC-N0-1G',
        ]
    }

    @SuppressWarnings('JUnitTestMethodWithoutAssert')
    @Unroll
    void "test parsePid invalid input #pid"() {
        given:
        boolean validPid

        when:
        validPid = parser.tryParsePid(pid)

        then:
        !validPid

        where:
        pid  | _
        ''   | _
        null | _
        and: 'Input with invalid pid'
        'INVALID_PID' | _
    }

    @SuppressWarnings('JUnitTestMethodWithoutAssert')
    @Unroll
    void "test tryParseCellPosition valid input #identifier"() {
        given:
        String cellPosition

        when:
        cellPosition = parser.tryParseCellPosition(identifier)

        then:
        cellPosition == expectedCellPosition

        where:
        identifier            | expectedCellPosition
        "K12A-123ABC-T0-G1"   | "G1"
        "K12A-123ABC-T0-12C3" | "12C3"
        "K12A-123ABC-T3-1J02" | "1J02"
    }

    @SuppressWarnings('JUnitTestMethodWithoutAssert')
    @Unroll
    void "test tryParseCellPosition invalid input #identifier"() {
        given:
        String cellPosition

        when:
        cellPosition = parser.tryParseCellPosition(identifier)

        then:
        cellPosition == null

        where:
        identifier << [
                null,
                "",
                'K12A-123ABC-T0-123C',
                'K12A-123ABC-T0-1C123',
        ]
    }
}
