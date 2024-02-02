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
package de.dkfz.tbi.otp.job.processing

import spock.lang.Specification

class ClusterJobSubmissionOptionsServiceSpec extends Specification {

    void "test jsonStringToMap"() {
        expect:
        expected == ClusterJobSubmissionOptionsService.convertJsonObjectStringToMap(jsonString)

        where:
        jsonString                           || expected
        ''                                   || [:]
        '{"WALLTIME": "30"}'                 || [(JobSubmissionOption.WALLTIME): "30"]
        '{"WALLTIME": "30", "NODES": "1"}'   || [(JobSubmissionOption.NODES): "1", (JobSubmissionOption.WALLTIME): "30"]
        '{"WALLTIME": "30", "NODES": "1", }' || [(JobSubmissionOption.NODES): "1", (JobSubmissionOption.WALLTIME): "30"]
    }

    void "test validateOptionString, valid string"() {
        expect:
        !ClusterJobSubmissionOptionsService.validateJsonObjectString(jsonString)

        where:
        jsonString                           | _
        ''                                   | _
        '{"WALLTIME": "30"}'                 | _
        '{"WALLTIME": "30", "NODES": "1"}'   | _
        '{"WALLTIME": "30", "NODES": "1", }' | _
    }

    void "test validateOptionString, invalid string"() {
        expect:
        ClusterJobSubmissionOptionsService.validateJsonObjectString(jsonString)

        where:
        jsonString                           | _
        'asdf'                               | _
        '"WALLTIME": "30"'                   | _
        '[]'                                 | _
        '["WALLTIME", "30"]'                 | _
        '{}'                                 | _
        '{"WALL_TIME": "30"}'                | _
        '{"WALLTIME": 30}'                   | _
        '{"WALLTIME": "30", "NODES: 1}'      | _
        '{"WALLTIME": "30", {"NODES": "1"}}' | _
    }
}
