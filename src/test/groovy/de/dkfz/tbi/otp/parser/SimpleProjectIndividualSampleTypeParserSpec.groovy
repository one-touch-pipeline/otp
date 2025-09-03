/*
 * Copyright 2011-2025 The OTP authors
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
import de.dkfz.tbi.otp.ngsdata.SampleTypePerProject
import de.dkfz.tbi.otp.project.Project

class SimpleProjectIndividualSampleTypeParserSpec extends Specification {

    private final SimpleProjectIndividualSampleTypeParser simpleProjectIndividualSampleTypeParser = new SimpleProjectIndividualSampleTypeParser()

    Class[] getDomainClassesToMock() {
        return [
                SampleTypePerProject,
                Project,
                SampleType,
        ]
    }

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

        where:
        input                                                    || pid         | sampleType  | project         | identifier
        '[some_project][some_pid][tumor01][DisplayIdentifier]'   || 'some_pid'  | 'tumor01'   | 'some_project'  | 'DisplayIdentifier'
        '[some_project][some_pid][tumor01][with space]'          || 'some_pid'  | 'tumor01'   | 'some_project'  | 'with space'
        '[some_project][some_pid][tumor01][with_underscore]'     || 'some_pid'  | 'tumor01'   | 'some_project'  | 'with_underscore'
        '[some_project][some_pid][tumor01][(with_brackets)]'     || 'some_pid'  | 'tumor01'   | 'some_project'  | '(with_brackets)'
        '[some_proj+ect][som+e_pid][tu+mor01][with+plus]'        || 'som+e_pid' | 'tu+mor01'  | 'some_proj+ect' | 'with+plus'
        '[some_project][some_pid][tumor01-x][DisplayIdentifier]' || 'some_pid'  | 'tumor01-x' | 'some_project'  | 'DisplayIdentifier'
        '[some_project][some_pid][tumor01-X][DisplayIdentifier]' || 'some_pid'  | 'tumor01-X' | 'some_project'  | 'DisplayIdentifier'
    }

    @Unroll('identifier #input is parsed to PID #pid, #sampleType, #project, #identifier and #sampleTypeCategory')
    void "test parse valid input2"() {
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
        defaultParsedSampleIdentifier.sampleTypeCategory.toString() == sampleTypeCategory

        where:
        input                                                             || pid        | sampleType  | project        | identifier          | sampleTypeCategory
        '[some_project][some_pid][tumor01][DisplayIdentifier][UNDEFINED]' || 'some_pid' | 'tumor01'   | 'some_project' | 'DisplayIdentifier' | 'UNDEFINED'
        '[some_project][some_pid][tumor01][DisplayIdentifier][IGNORED]'   || 'some_pid' | 'tumor01'   | 'some_project' | 'DisplayIdentifier' | 'IGNORED'
        '[some_project][some_pid][tumor01][DisplayIdentifier][DISEASE]'   || 'some_pid' | 'tumor01'   | 'some_project' | 'DisplayIdentifier' | 'DISEASE'
        '[some_project][some_pid][control01][DisplayIdentifier][CONTROL]' || 'some_pid' | 'control01' | 'some_project' | 'DisplayIdentifier' | 'CONTROL'
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
        input                                                           | problem
        ''                                                              | 'empty'
        null                                                            | 'null'
        'some_project_some_pid_tumor01_DisplayIdentifier'               | 'no brackets found'
        '[some_project][some_pid][tumor_01][DisplayIdentifier]'         | 'underscore in sample Type'
        '[some_project][some_pid][tumor_01]'                            | 'one group missing'
        '[some_project][some_pid][tumor_01][Display][category]'         | 'invalid category'
        '[some_project][some_pid][tumor_01][Display][DISEASE][tooMuch]' | 'one group too much'
        '(some_project)(some_pid)(tumor_01)(Display)'                   | 'wrong brackets'
    }
}
