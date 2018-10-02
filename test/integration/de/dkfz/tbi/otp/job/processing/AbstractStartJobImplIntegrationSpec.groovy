package de.dkfz.tbi.otp.job.processing

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.job.plan.*
import de.dkfz.tbi.otp.ngsdata.*
import grails.test.spock.*
import spock.lang.*

class AbstractStartJobImplIntegrationSpec extends IntegrationSpec {

    AbstractStartJobImpl job
    JobExecutionPlan jep

    void setup() {
        jep = DomainFactory.createJobExecutionPlan(enabled: true)

        job = [
                getJobExecutionPlan : { -> jep },
        ] as AbstractStartJobImpl
        job.optionService = new ProcessingOptionService()

        3.times {
            DomainFactory.createProcess(jobExecutionPlan: jep, finished: true)
        }
    }

    void "test getMinimumProcessingPriorityForOccupyingASlot, no JobExecutionPlan"() {
        given:
        job = [
                getJobExecutionPlan : { -> null },
        ] as AbstractStartJobImpl

        expect:
        job.minimumProcessingPriorityForOccupyingASlot == ProcessingPriority.SUPREMUM
    }

    @Unroll
    void "test getMinimumProcessingPriorityForOccupyingASlot, JobExecutionPlan obsoleted #obsoleted enabled #enabled"() {
        given:
        jep.enabled = enabled
        jep.obsoleted = obsoleted

        expect:
        job.minimumProcessingPriorityForOccupyingASlot == priority

        where:
        obsoleted | enabled || priority
        true      | true    || ProcessingPriority.SUPREMUM
        true      | false   || ProcessingPriority.SUPREMUM
        false     | true    || ProcessingPriority.MINIMUM
        false     | false   || ProcessingPriority.SUPREMUM
    }

    @Unroll
    void "test getMinimumProcessingPriorityForOccupyingASlot with #runningProcesses running processes "() {
        given:
        prepareTestGetMinimumProcessingPriorityForOccupyingASlot(runningProcesses)

        expect:
        job.minimumProcessingPriorityForOccupyingASlot == priority

        where:
        runningProcesses || priority
        3                || ProcessingPriority.SUPREMUM
        2                || ProcessingPriority.FAST_TRACK
        1                || ProcessingPriority.MINIMUM
    }

    private void prepareTestGetMinimumProcessingPriorityForOccupyingASlot(final int runningProcesses) {
        runningProcesses.times {
            DomainFactory.createProcess(jobExecutionPlan: jep)
        }

        DomainFactory.createProcessingOptionLazy(
                name: ProcessingOption.OptionName.MAXIMUM_NUMBER_OF_JOBS,
                value: 3,
                type: jep.name,
        )
        DomainFactory.createProcessingOptionLazy(
                name: ProcessingOption.OptionName.MAXIMUM_NUMBER_OF_JOBS_RESERVED_FOR_FAST_TRACK,
                value: 1,
                type: jep.name,
        )
    }
}
