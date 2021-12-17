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
package de.dkfz.tbi.otp.job.processing

import grails.testing.gorm.DataTest
import spock.lang.Specification

import de.dkfz.tbi.otp.job.plan.JobDefinition
import de.dkfz.tbi.otp.job.plan.JobExecutionPlan
import de.dkfz.tbi.otp.ngsdata.DomainFactory

class ParameterMappingSpec extends Specification implements DataTest {

    @Override
    Class[] getDomainClassesToMock() {
        [
                JobDefinition,
                JobExecutionPlan,
                ParameterType,
                ParameterMapping,
        ]
    }

    void testConstraints() {
        given:
        ParameterMapping mapping = new ParameterMapping()

        JobDefinition jobDefinition = DomainFactory.createJobDefinition()
        JobDefinition jobDefinition2 = DomainFactory.createJobDefinition()
        ParameterType type = new ParameterType(jobDefinition: jobDefinition, parameterUsage: ParameterUsage.INPUT, name: 'input1').save(flush: true)
        ParameterType type2 = new ParameterType(jobDefinition: jobDefinition, parameterUsage: ParameterUsage.INPUT, name: 'input2').save(flush: true)
        ParameterType type3 = new ParameterType(jobDefinition: jobDefinition2, parameterUsage: ParameterUsage.INPUT, name: 'input3').save(flush: true)
        ParameterType type4 = new ParameterType(jobDefinition: jobDefinition2, parameterUsage: ParameterUsage.OUTPUT, name: 'input4').save(flush: true)

        expect:
        !mapping.validate()
        "nullable" == mapping.errors["from"].code
        "nullable" == mapping.errors["to"].code
        "nullable" == mapping.errors["job"].code

        when:
        mapping.job = jobDefinition

        then:
        !mapping.validate()
        "nullable" == mapping.errors["from"].code
        "nullable" == mapping.errors["to"].code
        "validator.invalid" == mapping.errors["job"].code

        when: // use the from
        mapping.from = type

        then:
        !mapping.validate()
        "nullable" == mapping.errors["to"].code
        "validator.invalid" == mapping.errors["job"].code
        "parameterUsage" == mapping.errors["from"].code

        when: // use the to - same as from
        mapping.to = type

        then:
        !mapping.validate()
        "jobDefinition" == mapping.errors["from"].code
        "jobDefinition" == mapping.errors["to"].code
        null == mapping.errors["job"]

        when: // change the to to a wrong jobDefinition
        mapping.to = type3

        then:

        !mapping.validate()
        "validator.invalid" == mapping.errors["job"].code
        "parameterUsage" == mapping.errors["from"].code
        null == mapping.errors["to"]

        when: // use correct type for from, but same jobDefinition for to
        mapping.from = type2
        mapping.to = type

        then:
        !mapping.validate()
        "jobDefinition" == mapping.errors["from"].code
        "jobDefinition" == mapping.errors["to"].code
        null == mapping.errors["job"]

        when: // finally something useful
        mapping.from = type4

        then:
        mapping.validate()
    }
}
