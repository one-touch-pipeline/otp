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
package de.dkfz.tbi.otp.dataprocessing

import grails.testing.gorm.DataTest
import spock.lang.Specification
import spock.lang.Unroll

class WorkflowConfigServiceSpec extends Specification implements DataTest {

    @Override
    Class[] getDomainClassesToMock() {
        return []
    }

    @Unroll
    void "getNextConfigVersion uses v1_0 when config=#prev"() {
        given:
        WorkflowConfigService workflowConfigService = new WorkflowConfigService()

        when:
        String nextVersion = workflowConfigService.getNextConfigVersion(prev)

        then:
        nextVersion == "v1_0"

        where:
        prev << [null, ""]
    }

    @Unroll
    void "getNextConfigVersion increases the version properly (#active -> #expect)"() {
        given:
        WorkflowConfigService workflowConfigService = new WorkflowConfigService()

        when:
        String nextVersion = workflowConfigService.getNextConfigVersion(active)

        then:
        nextVersion == expect

        where:
        active   || expect
        "v1_0"   || "v1_1"
        "v2_0"   || "v2_1"
        "vA_0"   || "vA_1"
        "v4_9"   || "v4_10"
        "v1_2_3" || "v1_3"
        "1_2"    || "1_3"
    }
}
