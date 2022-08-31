/*
 * Copyright 2011-2022 The OTP authors
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
package de.dkfz.tbi.otp.workflowExecution

import grails.testing.gorm.DataTest
import groovy.json.JsonException
import spock.lang.Specification
import spock.lang.Unroll

class ExternalWorkflowConfigSelectorServiceSpec extends Specification implements DataTest {

    ExternalWorkflowConfigSelectorService service = new ExternalWorkflowConfigSelectorService()

    @SuppressWarnings("LineLength")
    @Unroll()
    void "getAllConflictingConfigValues, should return conflicting key with corresponding values in json formatted strings"() {
        when:
        List<JsonConflictingParameters> conflictingValues = service.getAllConflictingConfigValues(string1, string2)

        then:
        conflictingValues.toString() == expectedConflictingValues.toString()

        where:
        string1                                   | string2                                    | expectedConflictingValues
        '{"arg1":"test","arg2":{}}'               | '{"arg1":"test","arg2":{}}'                | []
        '{"arg1":"test2"}'                        | '{"arg1":"test","arg2":{}}'                | [new JsonConflictingParameters('arg1', "test2", "test")]
        '{"arg1":{"subarg1":1,"subarg2":"test2"}' | '{"arg1":{"subarg1":true}}'                | [new JsonConflictingParameters('arg1.subarg1', "1", "true")]
        '{"arg1":{"subarg1":true,"subarg2":{}}}'  | '{"arg1":{"subarg1":{},"subarg2":"test"}}' | [new JsonConflictingParameters('arg1.subarg1', "true", "{}"),
                                                                                                  new JsonConflictingParameters('arg1.subarg2', "{}", "test")]
        null                                      | null                                       | []
    }

    @Unroll()
    void "getAllConflictingConfigValues, should throw JsonException when string is not in json format"() {
        when:
        service.getAllConflictingConfigValues(string1, string2)

        then:
        thrown(JsonException)

        where:
        string1                                            | string2
        '{"arg1: "test"}'                                  | '{"arg1": "test"}'
        '{"arg1": "test2"}'                                | '{"arg1": test"}'
        '{"arg1": {"subarg1" : "test", "subarg2": "test2"' | '{"arg1": {"subarg1" : "test2"},}'
    }
}
