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
package de.dkfz.tbi.otp.job.plan

import grails.testing.gorm.DataTest
import grails.validation.ValidationException
import spock.lang.Specification

import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.utils.CollectionUtils

class JobErrorDefinitionSpec extends Specification implements DataTest {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                JobDefinition,
                JobErrorDefinition,
                JobExecutionPlan,
        ]
    }

    void setup() {
        DomainFactory.createJobDefinition()

        JobErrorDefinition jobErrorDefinition = new JobErrorDefinition(
                errorExpression: "jobErrorDefinition",
                type: JobErrorDefinition.Type.MESSAGE,
                action: JobErrorDefinition.Action.STOP
        )
        jobErrorDefinition.save(flush: true)
    }

    void 'tries to add JobDefinition, succeeds'() {
        given:
        JobDefinition jobDefinition = CollectionUtils.exactlyOneElement(JobDefinition.findAll())
        JobErrorDefinition jobErrorDefinition = CollectionUtils.exactlyOneElement(JobErrorDefinition.findAll())

        when:
        jobErrorDefinition.addToJobDefinitions(jobDefinition)
        jobErrorDefinition.save(flush: true)
        jobErrorDefinition.addToJobDefinitions(jobDefinition)
        jobErrorDefinition.save(flush: true)

        then:
        jobErrorDefinition.jobDefinitions.contains(jobDefinition)
        jobErrorDefinition.jobDefinitions.size() == 1
    }

    void 'tries to add JobErrorDefinition, when action = furtherCheck, succeeds'() {
        given:
        JobErrorDefinition jobErrorDefinition = CollectionUtils.exactlyOneElement(JobErrorDefinition.findAll())
        JobErrorDefinition jobErrorDefinition1 = new JobErrorDefinition(
                errorExpression: "jobErrorDefinition1",
                type: JobErrorDefinition.Type.MESSAGE,
                action: JobErrorDefinition.Action.CHECK_FURTHER
        )

        when:
        jobErrorDefinition1.addToCheckFurtherJobErrors(jobErrorDefinition)
        jobErrorDefinition1.save(flush: true)

        then:
        jobErrorDefinition1.checkFurtherJobErrors.contains(jobErrorDefinition)
    }

    void 'create JobErrorDefinition with invalid errorExpression, should fail'() {
        given:
        JobErrorDefinition jobErrorDefinition = new JobErrorDefinition(
                errorExpression: "*",
                type: JobErrorDefinition.Type.MESSAGE,
                action: JobErrorDefinition.Action.STOP
        )

        when:
        jobErrorDefinition.save(flush: true)

        then:
        ValidationException e = thrown()
        e.message.contains("invalid")
    }
}
