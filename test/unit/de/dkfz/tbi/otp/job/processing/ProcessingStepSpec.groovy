package de.dkfz.tbi.otp.job.processing

import de.dkfz.tbi.otp.job.plan.JobDefinition
import de.dkfz.tbi.otp.job.plan.JobExecutionPlan
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.ngsdata.Realm
import grails.test.mixin.Mock
import spock.lang.Specification

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
        ProcessingStep processingStep = createProcessingStepWithUpdates()

        when:
        ProcessingStepUpdate update = processingStep.firstProcessingStepUpdate

        then:
        update.state == ExecutionState.CREATED
    }

    void "test latestProcessingStepUpdate return last update"() {
        given:
        ProcessingStep processingStep = createProcessingStepWithUpdates()

        when:
        ProcessingStepUpdate update = processingStep.latestProcessingStepUpdate

        then:
        update.state == ExecutionState.SUCCESS
    }


    private ProcessingStep createProcessingStepWithUpdates() {
        ProcessingStep processingStep = DomainFactory.createProcessingStep()
        ProcessingStepUpdate last = DomainFactory.createProcessingStepUpdate(processingStep: processingStep, state: ExecutionState.CREATED)
        last = DomainFactory.createProcessingStepUpdate(processingStep: processingStep, state: ExecutionState.STARTED, previous: last)
        last = DomainFactory.createProcessingStepUpdate(processingStep: processingStep, state: ExecutionState.FINISHED, previous: last)
        DomainFactory.createProcessingStepUpdate(processingStep: processingStep, state: ExecutionState.SUCCESS, previous: last)
        return processingStep
    }

}
