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
package de.dkfz.tbi.otp.parser.covid19

import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.ngsdata.SampleType
import de.dkfz.tbi.otp.parser.DefaultParsedSampleIdentifier

class Covid19SampleIdentifierParserSpec extends Specification implements DomainFactoryCore {

    Covid19SampleIdentifierParser covid19SampleIdentifierParser = new Covid19SampleIdentifierParser()

    @Unroll('Covid19 identifier #input is parsed to Project #project PID #pid, sample type name #sampleTypeDbName')
    void "test parse valid input"() {
        given:
        DefaultParsedSampleIdentifier defaultParsedSampleIdentifier
        boolean validPid

        when:
        defaultParsedSampleIdentifier = covid19SampleIdentifierParser.tryParse(input)
        validPid = covid19SampleIdentifierParser.tryParsePid(pid)

        then:
        validPid
        defaultParsedSampleIdentifier.projectName == project
        defaultParsedSampleIdentifier.pid == pid
        defaultParsedSampleIdentifier.sampleTypeDbName == sampleTypeDbName
        defaultParsedSampleIdentifier.fullSampleName == input
        defaultParsedSampleIdentifier.useSpecificReferenceGenome == SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT

        where:
        input                || project                | pid           | sampleTypeDbName
        'SC2-10001SR-NS001'  || 'covid-19_single-cell' | 'SC2-10001SR' | 'nasal_swab001'
        'SC2-20001SR-TS010'  || 'covid-19_single-cell' | 'SC2-20001SR' | 'throat_swab010'
        'SC2-15001SR-BL100'  || 'covid-19_single-cell' | 'SC2-15001SR' | 'bronchial_lavage100'
        'SC2-25001SR-B123'   || 'covid-19_single-cell' | 'SC2-25001SR' | 'blood123'
        'SC2-19999SR-PSB321' || 'covid-19_single-cell' | 'SC2-19999SR' | 'protected_specimen_brush321'
        'SC2-10001MD-NS666'  || 'covid-19_methyl-seq'  | 'SC2-10001MD' | 'nasal_swab666'
        'SC2-10001BR-NS999'  || 'covid-19_cite-seq'    | 'SC2-10001BR' | 'nasal_swab999'
        and: "alternative schema:"
        'SC2-1-0001SR-NS-01'  || 'covid-19_single-cell' | 'SC2-10001SR' | 'nasal_swab001'
        'SC2-1-0001SR-NS-001' || 'covid-19_single-cell' | 'SC2-10001SR' | 'nasal_swab001'
        'SC2-2-0001SR-TS-10'  || 'covid-19_single-cell' | 'SC2-20001SR' | 'throat_swab010'
        'SC2-1-5001SR-BL-10'  || 'covid-19_single-cell' | 'SC2-15001SR' | 'bronchial_lavage010'
        'SC2-2-5001SR-B-23'   || 'covid-19_single-cell' | 'SC2-25001SR' | 'blood023'
        'SC2-1-9999SR-PSB-21' || 'covid-19_single-cell' | 'SC2-19999SR' | 'protected_specimen_brush021'
        'SC2-1-0001MD-NS-66'  || 'covid-19_methyl-seq'  | 'SC2-10001MD' | 'nasal_swab066'
        'SC2-1-0001BR-NS-99'  || 'covid-19_cite-seq'    | 'SC2-10001BR' | 'nasal_swab099'
    }

    @Unroll
    void "test parse invalid input #input (problem: #problem)"() {
        given:
        DefaultParsedSampleIdentifier defaultParsedSampleIdentifier

        when:
        defaultParsedSampleIdentifier = covid19SampleIdentifierParser.tryParse(input)

        then:
        defaultParsedSampleIdentifier == null

        where:
        input                 | problem
        ''                    | 'empty'
        null                  | 'null'
        'SC1-10001SR-NS001'   | 'wrong project'
        'SC2-1001SR-NS001'    | 'pid too short'
        'SC2-100001SR-NS001'  | 'pid too long'
        'SC2-10001XX-NS001'   | 'invalid seqtype'
        'SC2-10001SR-XX001'   | 'invlaid sampletype'
        'SC2-10001SR-NS1'     | 'sampleType order number too short'
        'SC2-10001SR-NS-1'    | 'sampleType order number too short'
        'SC2-10001SR-NS0001'  | 'sampleType order number too long'
        'SC2-10001SR-NS-0001' | 'sampleType order number too long'
    }

    @Unroll
    void "test tryParseSingleCellWellLabel is not implemented and always returns null (#identifier)"() {
        given:
        String singleCellWellLabel

        when:
        singleCellWellLabel = covid19SampleIdentifierParser.tryParseSingleCellWellLabel(identifier)

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
