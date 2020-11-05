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

class JobDefinitionSpec extends Specification implements DataTest {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                JobDefinition,
        ]
    }

    void testConstraints() {
        given:
        JobDefinition jobDefinition = new JobDefinition()

        expect:
        !jobDefinition.validate()
        "nullable" == jobDefinition.errors["name"].code
        "nullable" == jobDefinition.errors["bean"].code
        "nullable" == jobDefinition.errors["plan"].code

        when:
        JobExecutionPlan jobExecutionPlan = new JobExecutionPlan(name: 'some name')
        jobDefinition.plan = jobExecutionPlan

        then:
        !jobDefinition.validate()

        when:
        jobDefinition.name = "testDefinition"

        then:
        !jobDefinition.validate()

        when:
        jobDefinition.bean = "testBean"

        then:
        jobDefinition.validate()

        when:
        JobDefinition previous = new JobDefinition([
                plan: jobExecutionPlan,
                bean: 'bean',
                name: 'name',
        ])
        jobDefinition.previous = previous
        jobDefinition.validate()

        then:
        !jobDefinition.errors.hasErrors()


        when:
        JobDefinition next = new JobDefinition([
                plan: jobExecutionPlan,
                bean: 'bean',
                name: 'name',
        ])
        jobDefinition.next = next
        jobDefinition.validate()

        then:
        !jobDefinition.errors.hasErrors()
    }
}
