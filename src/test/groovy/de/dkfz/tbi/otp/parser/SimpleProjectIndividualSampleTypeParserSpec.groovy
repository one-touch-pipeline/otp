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
package de.dkfz.tbi.otp.parser

import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.ngsdata.SampleType

class SimpleProjectIndividualSampleTypeParserSpec extends Specification {

    private SimpleProjectIndividualSampleTypeParser simpleProjectIndividualSampleTypeParser = new SimpleProjectIndividualSampleTypeParser()

    @Unroll('identifier #input is parsed to PID #pid, #sampleType, #project and #identifier')
    void "test parse valid input"() {
        given:
        DefaultParsedSampleIdentifier defaultParsedSampleIdentifier
        boolean validPid

        when:
        defaultParsedSampleIdentifier = simpleProjectIndividualSampleTypeParser.tryParse(input)
        validPid = simpleProjectIndividualSampleTypeParser.tryParsePid(pid)

        then:
        validPid
        defaultParsedSampleIdentifier.projectName == project
        defaultParsedSampleIdentifier.pid == pid
        defaultParsedSampleIdentifier.sampleTypeDbName == sampleType
        defaultParsedSampleIdentifier.fullSampleName == identifier
        defaultParsedSampleIdentifier.useSpecificReferenceGenome == SampleType.SpecificReferenceGenome.UNKNOWN

        where:
        input                                              || pid        | sampleType | project    | identifier
        '(hipo_021)(some_pid)(TUMOR01)(DisplayIdentifier)' || 'some_pid' | 'TUMOR01'  | 'hipo_021' | 'DisplayIdentifier'
        '(hipo_021)(some_pid)(TUMOR01)(with space)'        || 'some_pid' | 'TUMOR01'  | 'hipo_021' | 'with space'
        '(hipo_021)(some_pid)(TUMOR01)(with_underscore)'   || 'some_pid' | 'TUMOR01'  | 'hipo_021' | 'with_underscore'
    }

    @Unroll
    void "test parse invalid input #input (problem: #problem)"() {
        given:
        DefaultParsedSampleIdentifier defaultParsedSampleIdentifier

        when:
        defaultParsedSampleIdentifier = simpleProjectIndividualSampleTypeParser.tryParse(input)

        then:
        defaultParsedSampleIdentifier == null

        where:
        input                                               | problem
        ''                                                  | 'empty'
        null                                                | 'null'
        'hipo_021_some_pid_TUMOR01_DisplayIdentifier'       | 'no brackets found'
        '(hipo_021)(some_pid)(TUMOR_01)(DisplayIdentifier)' | 'underscore in sample Type'
        '(hipo_021)(some_pid)(TUMOR_01)'                    | 'one group missing'
        '(hipo_021)(some_pid)(TUMOR_01)(Display)(toMuch)'   | 'one group to much'
    }
}
