package de.dkfz.tbi.otp.job.processing

import de.dkfz.tbi.otp.job.plan.*
import de.dkfz.tbi.otp.ngsdata.*
import grails.test.mixin.*
import spock.lang.*

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
