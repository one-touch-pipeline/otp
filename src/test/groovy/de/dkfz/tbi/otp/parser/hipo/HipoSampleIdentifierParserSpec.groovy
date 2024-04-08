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
package de.dkfz.tbi.otp.parser.hipo

import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.ngsdata.SampleType

class HipoSampleIdentifierParserSpec extends Specification {

    HipoSampleIdentifierParser parser = new HipoSampleIdentifierParser()

    @Unroll
    void 'tryParse, when H059, uses sample number exactly as given'() {
        given:
        String fullSampleName = "H059-ABCDEF-T${sampleNumber}-D1"

        when:
        HipoSampleIdentifier identifier = parser.tryParse(fullSampleName)
        boolean validPid = parser.tryParsePid(fullSampleName.substring(0, 11))

        then:
        validPid
        identifier.sampleNumber == sampleNumber
        identifier.sampleTypeDbName == "tumor${sampleNumber}".toString()
        identifier.fullSampleName == fullSampleName
        identifier.useSpecificReferenceGenome == SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT

        where:
        sampleNumber << ['0', '00', '1', '01', '2', '02', '10', '11']
    }

    @Unroll
    void 'tryParse, when not H059 and sample number has two digits, returns null'() {
        given:
        String fullSampleName = "H123-ABCDEF-T${sampleNumber}-D1"

        when:
        HipoSampleIdentifier identifier = parser.tryParse(fullSampleName)
        boolean validPid = parser.tryParsePid(fullSampleName.substring(0, 11))

        then:
        validPid
        identifier.sampleNumber == sampleNumber
        identifier.sampleTypeDbName == "tumor${sampleTypeDbName}".toString()
        identifier.fullSampleName == fullSampleName
        identifier.useSpecificReferenceGenome == SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT

        where:
        sampleNumber || sampleTypeDbName
        '0'          || '00'
        '1'          || ''
        '2'          || '02'
        '10'         || '10'
        '11'         || '11'
        '99'         || '99'
    }

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

    @Unroll
    void "test tryParseSingleCellWellLabel is not implemented and always returns null"() {
        given:
        String singleCellWellLabel

        when:
        singleCellWellLabel = parser.tryParseSingleCellWellLabel(sampleIdentifier)

        then:
        singleCellWellLabel == null

        where:
        sampleIdentifier << [
                'H059-ABCDEF-T0-D1',
                'H059-ABCDEF-T01-D1',
                'INVALID_PID',
        ]
    }

    void "test tryParse, invalid sample name, returns null"() {
        expect:
        !parser.tryParse(sampleName)

        where:
        sampleName           || _
        "H004-ABCD-T1"       || _
        ""                   || _
        null                 || _
        "H004-BPF4-D4-D1"    || _
        "P021-EFGH"          || _
        "H035-BPD1-B3-D1"    || _
        "H035-BPDM-T3-D1"    || _
        "H003-BPDK-C8-M1"    || _
        "H003-BPDK-C8-D01"   || _
        "H003-BPDK-C8-C1"    || _
        "H032-PX6D42-T2-B01" || _
        "H123-ABCDEF-T00-D1" || _
        "H123-ABCDEF-T01-D1" || _
        "H123-ABCDEF-T09-D1" || _
        "H456-ABCD-T3-D1-V9" || _
    }

    void "test tryParse, valid sample name, returns identifier"() {
        expect:
        parser.tryParse(sampleName)

        where:
        sampleName           || _
        "H004-ABCD-T1-D1"    || _
        "P021-EFGH-T1-D1"    || _
        "H035-BPDM-B3-D1"    || _
        "H035-BPDM-C4-D1"    || _
        "H035-BPDK-B1-D1"    || _
        "H035-BPDK-C8-D1"    || _
        "H003-BPDK-C8-A1"    || _
        "H00B-BPD1-T1-D1"    || _
        "H00A-BPD1-T1-D1"    || _
        "H003-BPDK-C8-C10"   || _
        "H003-BPDK-C8-C02"   || _
        "H059-BPDK-C80-C02"  || _
        "H003-BPDK-C80-C02"  || _
        "H032-PX6D42-M2-D1"  || _
        "H032-PX6D42-T2-W1"  || _
        "H032-PX6D42-T2-Y1"  || _
        "H032-PX6D42-T2-B1"  || _
        "K032-PX6D42-T2-B1"  || _
        "A032-PX6D42-T2-B1"  || _
        "H456-ABCD-T3-D1-V8" || _
    }

    void "test tryParse, valid sample name, check results"() {
        when:
        HipoSampleIdentifier identifier = parser.tryParse(sampleName)

        then:
        identifier
        projectNumber == identifier.projectNumber
        pid == identifier.pid
        tissueType == identifier.tissueType
        sampleNumber == identifier.sampleNumber
        analyteTypeAndNumber == identifier.analyteTypeAndNumber
        sampleName == identifier.fullSampleName
        sampleName == identifier.toString()

        where:
        sampleName             || projectNumber | pid           | tissueType                | sampleNumber | analyteTypeAndNumber
        "H456-ABCD-T3-D1"      || "456"         | "H456-ABCD"   | HipoTissueType.TUMOR      | '3'          | "D1"
        "C026-EFGH-M2-D1"      || "C026"        | "C026-EFGH"   | HipoTissueType.METASTASIS | '2'          | "D1"
        "H035-IJKLMM-B1-D1"    || "035"         | "H035-IJKLMM" | HipoTissueType.BLOOD      | '1'          | "D1"
        "H035-IJKLMM-B1-D1-V8" || "035"         | "H035-IJKLMM" | HipoTissueType.BLOOD      | '1'          | "D1"
    }

    void "test sampleTypeDbName"() {
        expect:
        tissueTypeExp == parser.tryParse(sampleName).sampleTypeDbName

        where:
        sampleName           || tissueTypeExp
        "H004-ABCD-T1-D1"    || "tumor"
        "H004-ABCD-T1-D1-V8" || "tumor-v8"
        "H456-ABCD-T3-D1"    || "tumor03"
        "H456-ABCD-T3-D1-V8" || "tumor03-v8"
        "H035-BPDM-B3-D1"    || "blood03"
        "H035-BPDM-B1-D1"    || "blood01"
        "H035-BPDM-C1-D1"    || "cell01"
        "H035-IJKLMM-B1-D1"  || "blood01"
        "H001-BPDK-L8-C02"   || "plasma08"
        "H001-BPDK-Z8-C02"   || "normal_sorted_cells08"
        "H003-BPDK-E8-C02"   || "tumor_interval_debulking_surgery08".toLowerCase()
    }
}
