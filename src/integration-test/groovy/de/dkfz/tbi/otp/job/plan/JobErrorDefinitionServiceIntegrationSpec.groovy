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

import grails.testing.mixin.integration.Integration
import grails.transaction.Rollback
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.ngsdata.DomainFactory

@Rollback
@Integration
class JobErrorDefinitionServiceIntegrationSpec extends Specification {

    @Unroll
    void "getAllJobErrorDefinition with one JobErrorDefinition"() {
        given:
        DomainFactory.createJobErrorDefinition()
        JobErrorDefinitionService service = new JobErrorDefinitionService()

        when:
        Map jobErrorDefinitions = service.allJobErrorDefinition

        then:
        jobErrorDefinitions.size() == 1
    }

    @Unroll
    void "getAllJobErrorDefinition with multiple JobErrorDefinitions"() {
        given:
        DomainFactory.createJobErrorDefinition()
        DomainFactory.createJobErrorDefinition(
                type: JobErrorDefinition.Type.STACKTRACE,
                action: JobErrorDefinition.Action.RESTART_WF,
                errorExpression: "ERROR_EXPRESSION",
        )
        DomainFactory.createJobErrorDefinition(
                type: JobErrorDefinition.Type.CLUSTER_LOG,
                action: JobErrorDefinition.Action.RESTART_JOB,
                errorExpression: "ERROR_EXPRESSION",
        )
        JobErrorDefinitionService service = new JobErrorDefinitionService()

        when:
        Map jobErrorDefinitions = service.allJobErrorDefinition

        then:
        jobErrorDefinitions.size() == 3
    }

    @Unroll
    void "getJobDefinition with one JobErrorDefinition"() {
        given:
        DomainFactory.createJobErrorDefinition(
                jobDefinitions: DomainFactory.createJobDefinition().list()
        )
        JobErrorDefinitionService service = new JobErrorDefinitionService()

        when:
        List<JobDefinition> jobDefinitions = service.getJobDefinition(service.allJobErrorDefinition)

        then:
        jobDefinitions.size() == 1
    }

    @Unroll
    void "getJobDefinition with multiple JobErrorDefinitions"() {
        given:
        DomainFactory.createJobErrorDefinition()
        JobErrorDefinitionService service = new JobErrorDefinitionService()

        when:
        List<JobDefinition> jobDefinitions = service.getJobDefinition(service.allJobErrorDefinition)

        then:
        jobDefinitions.size() == 3
    }

    @Unroll
    void "add JobErrorDefinitions to first level"() {
        given:
        JobErrorDefinitionService service = new JobErrorDefinitionService()

        when:
        service.addErrorExpressionFirstLevel(type, action, errorExpression)

        then:
        service.allJobErrorDefinition.get(JobErrorDefinition.findByErrorExpression(errorExpression)) == errorExpression

        where:
        type                            | action        | errorExpression
        JobErrorDefinition.Type.MESSAGE | "STOP"        | "ERROR_TEXT"
        JobErrorDefinition.Type.MESSAGE | "RESTART_JOB" | "ERROR_EXPRESSION"
        JobErrorDefinition.Type.MESSAGE | "RESTART_WF"  | "FAKE_TEXT"
    }

    @Unroll
    void "add JobErrorDefinitions to second level"() {
        given:
        JobErrorDefinitionService service = new JobErrorDefinitionService()
        JobErrorDefinition jobErrorDefinition = DomainFactory.createJobErrorDefinition(
                action: JobErrorDefinition.Action.CHECK_FURTHER,
                checkFurtherJobErrors: [],
        )

        when:
        service.addErrorExpression(type, action, errorExpression, jobErrorDefinition)

        then:
        service.allJobErrorDefinition.get(jobErrorDefinition).get(JobErrorDefinition.findByErrorExpression(errorExpression)) == errorExpression

        where:
        type          | action        | errorExpression
        "CLUSTER_LOG" | "STOP"        | "ERROR_TEXT"
        "STACKTRACE"  | "RESTART_JOB" | "ERROR_EXPRESSION"
        "MESSAGE"     | "RESTART_WF"  | "FAKE_TEXT"
    }

    @Unroll
    void "UpdateErrorExpression with one JobErrorDefinition"() {
        given:
        JobErrorDefinition jobErrorDefinition = DomainFactory.createJobErrorDefinition()
        JobErrorDefinitionService service = new JobErrorDefinitionService()
        String errorExpression = "ERROR_EXPRESSION"

        when:
        service.updateErrorExpression(jobErrorDefinition, errorExpression)

        then:
        jobErrorDefinition.errorExpression == errorExpression
    }

    @Unroll
    void "UpdateErrorExpression with multiple JobErrorDefinitions"() {
        given:
        JobErrorDefinition jobErrorDefinition = DomainFactory.createJobErrorDefinition()
        JobErrorDefinitionService service = new JobErrorDefinitionService()

        when:
        service.updateErrorExpression(jobErrorDefinition, errorExpression)

        then:
        jobErrorDefinition.errorExpression == errorExpression

        where:
        errorExpression         | _
        "ERROR_EXPRESSION"      | _
        "ERROR_EXPRESSION_TWO"  | _
        "ERROR_EXPRESSION_MORE" | _
    }

    @Unroll
    void "addNewJob with one JobErrorDefinition"() {
        given:
        JobErrorDefinition jobErrorDefinition = DomainFactory.createJobErrorDefinition()
        JobErrorDefinitionService service = new JobErrorDefinitionService()
        JobDefinition jobDefinition = DomainFactory.createJobDefinition(name: "NAME")

        when:
        service.addNewJob(jobErrorDefinition, jobDefinition)

        then:
        jobErrorDefinition.jobDefinitions.contains(jobDefinition)
    }

    @SuppressWarnings('SpaceInsideParentheses') //auto-format and codenarc clash
    @Unroll
    void "addNewJob with multiple JobErrorDefinitions"() {
        given:
        JobErrorDefinition jobErrorDefinition = DomainFactory.createJobErrorDefinition()
        JobErrorDefinitionService service = new JobErrorDefinitionService()
        JobDefinition jobDefinition = jobDefinitionClosure()

        when:
        service.addNewJob(jobErrorDefinition, jobDefinition)

        then:
        jobErrorDefinition.jobDefinitions.contains(jobDefinition)

        where:
        jobDefinitionClosure                                        | _
        ( { DomainFactory.createJobDefinition(name: "NAME") } )     | _
        ( { DomainFactory.createJobDefinition(name: "NEW_NAME") } ) | _
    }
}
