package de.dkfz.tbi.otp.job.processing

import grails.test.mixin.Mock
import spock.lang.Specification

import de.dkfz.tbi.otp.job.plan.JobDefinition
import de.dkfz.tbi.otp.job.plan.JobExecutionPlan
import de.dkfz.tbi.otp.ngsdata.DomainFactory

@Mock([
        JobDefinition,
        JobExecutionPlan,
        Process,
        ProcessingStep,
        ProcessingStepUpdate,
])
class ProcessingStepSpec extends Specification {

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
}
