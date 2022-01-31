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
package de.dkfz.tbi.otp.job.plan

import grails.testing.gorm.DataTest
import spock.lang.Specification

import de.dkfz.tbi.otp.job.processing.ProcessParameter

class JobExecutionPlanSpec extends Specification implements DataTest {

    @Override
    Class<?>[] getDomainClassesToMock() {
        return [
                JobExecutionPlan,
        ]
    }

    void testConstraints() {
        when:
        JobExecutionPlan jobExecutionPlan = new JobExecutionPlan()

        then:
        jobExecutionPlan.validate() == false
        jobExecutionPlan.errors["name"].code == "nullable"

        when:
        JobDefinition jobDefinition = new JobDefinition()
        jobExecutionPlan.firstJob = jobDefinition

        then:
        jobExecutionPlan.validate() == false

        when:
        StartJobDefinition startJobDefinition = new StartJobDefinition()
        jobExecutionPlan.startJob = startJobDefinition

        then:
        jobExecutionPlan.validate() == false

        when:
        JobExecutionPlan previous = new JobExecutionPlan()
        jobExecutionPlan.previousPlan = previous

        then:
        jobExecutionPlan.validate() == false
        jobExecutionPlan.errors["previousPlan"].code == "validator.invalid"

        when:
        jobExecutionPlan.name = "testPlan"
        // Assign smallest possible planVersion
        jobExecutionPlan.planVersion = 0

        then:
        jobExecutionPlan.validate() == false

        when:
        // Set previousPlan to null to pass validation
        jobExecutionPlan.previousPlan = null

        then:
        jobExecutionPlan.validate() == true

        when:
        jobExecutionPlan.previousPlan = previous
        // Assign smallest possible planVersion
        jobExecutionPlan.planVersion = 0
        // Assign higher planVersion value for previousPlan
        jobExecutionPlan.previousPlan.planVersion = 1

        then:
        jobExecutionPlan.validate() == false

        when:
        // Assign higher value to planVersion
        jobExecutionPlan.planVersion = 1
        // Assign small planVersion value for previousPlan
        jobExecutionPlan.previousPlan.planVersion = 0
        jobExecutionPlan.previousPlan.name = "testPlan"

        then:
        jobExecutionPlan.validate() == true
    }

    void testProcessParameters() {
        when:
        JobExecutionPlan jobExecutionPlan = new JobExecutionPlan()

        then:
        jobExecutionPlan.validate() == false

        when:
        jobExecutionPlan.name = "testPlan"

        then:
        jobExecutionPlan.validate() == true

        when:
        ProcessParameter processParameter = new ProcessParameter(value: "test")
        jobExecutionPlan.processParameter = processParameter

        then:
        jobExecutionPlan.validate() == true
        jobExecutionPlan.processParameter.is(processParameter) == true
        processParameter.value == "test"
    }
}
