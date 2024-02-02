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

import grails.testing.gorm.DataTest
import grails.validation.ValidationException
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.job.plan.JobDefinition
import de.dkfz.tbi.otp.job.plan.JobExecutionPlan
import de.dkfz.tbi.otp.ngsdata.DomainFactory

class ProcessingStepSpec extends Specification implements DataTest {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                JobDefinition,
                JobExecutionPlan,
                Process,
                ProcessingStep,
                ProcessingStepUpdate,
        ]
    }

    void "test firstProcessingStepUpdate return first update"() {
        given:
        ProcessingStep processingStep = DomainFactory.createProcessingStepWithUpdates()

        when:
        ProcessingStepUpdate update = processingStep.firstProcessingStepUpdate

        then:
        update.state == ExecutionState.CREATED
    }

    void "test latestProcessingStepUpdate return last update"() {
        given:
        ProcessingStep processingStep = DomainFactory.createProcessingStepWithUpdates()

        when:
        ProcessingStepUpdate update = processingStep.latestProcessingStepUpdate

        then:
        update.state == ExecutionState.SUCCESS
    }

    @Unroll
    void "test nullable"() {
        given:
        ProcessingStep step = DomainFactory.createProcessingStep()
        step.jobDefinition = jobDefinition ? step.jobDefinition : null
        step.process = process ? step.process : null

        when:
        step.save(flush: true)

        then:
        ValidationException e = thrown()

        if (!jobDefinition) {
            assert e.message.contains("on field 'jobDefinition': rejected value [null]")
        }

        if (!process) {
            assert e.message.contains("on field 'process': rejected value [null]")
        }

        where:
        jobDefinition | process
        null          | null
        null          | "notNull"
        "notNull"     | null
    }

    @Unroll
    void "test previous expects same Process but different JobDefinition but has problem with #errorField"() {
        given:
        Process process = DomainFactory.createProcess()
        JobDefinition jobDefinition = DomainFactory.createJobDefinition(plan: process.jobExecutionPlan)
        Process process2 = DomainFactory.createProcess()
        JobDefinition jobDefinition2 = DomainFactory.createJobDefinition(plan: process2.jobExecutionPlan)

        ProcessingStep step = DomainFactory.createProcessingStep(process: process, jobDefinition: jobDefinition)

        Map<String, ProcessingStep> errorCausingProcessingSteps = [
                "process": DomainFactory.createProcessingStep(process: process2, jobDefinition: jobDefinition2),
                "jobDefinition": DomainFactory.createProcessingStep(process: process, jobDefinition: jobDefinition),
        ]

        when:
        step.previous = errorCausingProcessingSteps[errorField]
        step.save(flush: true)

        then:
        ValidationException e = thrown()
        e.message.contains("on field 'previous': rejected value")
        step.errors["previous"].code == errorField

        where:
        errorField      | _
        "process"       | _
        "jobDefinition" | _
    }

    @Unroll
    void "test next expects same Process but different JobDefinition but has problem with #errorField"() {
        given:
        Process process = DomainFactory.createProcess()
        JobDefinition jobDefinition = DomainFactory.createJobDefinition(plan: process.jobExecutionPlan)
        Process process2 = DomainFactory.createProcess()
        JobDefinition jobDefinition2 = DomainFactory.createJobDefinition(plan: process2.jobExecutionPlan)

        ProcessingStep step = DomainFactory.createProcessingStep(process: process, jobDefinition: jobDefinition)

        Map<String, ProcessingStep> errorCausingProcessingSteps = [
                "process": DomainFactory.createProcessingStep(process: process2, jobDefinition: jobDefinition2),
                "jobDefinition": DomainFactory.createProcessingStep(process: process, jobDefinition: jobDefinition),
        ]

        when:
        step.next = errorCausingProcessingSteps[errorField]
        step.save(flush: true)

        then:
        ValidationException e = thrown()
        e.message.contains("on field 'next': rejected value")
        step.errors["next"].code == errorField

        where:
        errorField      | _
        "process"       | _
        "jobDefinition" | _
    }

    void "test next and previous of a ProcessingStep can not be the same"() {
        given:
        Process process = DomainFactory.createProcess()
        Process process2 = DomainFactory.createProcess()
        JobDefinition jobDefinition = DomainFactory.createJobDefinition(plan: process.jobExecutionPlan)
        JobDefinition jobDefinition2 = DomainFactory.createJobDefinition(plan: process2.jobExecutionPlan)

        ProcessingStep step = DomainFactory.createProcessingStep(process: process, jobDefinition: jobDefinition)

        when:
        jobDefinition2.plan = process.jobExecutionPlan
        ProcessingStep otherProcessingStep = DomainFactory.createProcessingStep(process: process, jobDefinition: jobDefinition2)
        step.previous = otherProcessingStep
        step.next = otherProcessingStep
        step.save(flush: true)

        then:
        ValidationException e = thrown()
        e.message.contains("on field 'previous': rejected value")
        step.errors["previous"].code == "next"
        e.message.contains("on field 'next': rejected value")
        step.errors["next"].code == "previous"
    }

    void "test Process and JobDefinition of ProcessingStep reference the same JobExecutionPlan"() {
        given: "valid ProcessingStep and Process and JobDefinition with unequal JobExecutionPlans"
        Process process = DomainFactory.createProcess()
        JobDefinition jobDefinition = DomainFactory.createJobDefinition()

        ProcessingStep step = DomainFactory.createProcessingStep(process: process)

        expect: "created ProcessingStep is valid and Plans differ"
        step.validate()
        step.process.jobExecutionPlan != jobDefinition.plan

        when: "using objects with differing JobExecutionPlans"
        step.jobDefinition = jobDefinition
        step.save(flush: true)

        then: "custom validator fails"
        ValidationException e = thrown()
        e.message.contains("on field 'process': rejected value")
        e.message.contains("does not pass custom validation")
    }
}
