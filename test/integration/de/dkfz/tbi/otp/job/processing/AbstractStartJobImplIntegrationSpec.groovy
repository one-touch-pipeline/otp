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
        job.minimumProcessingPriorityForOccupyingASlot == ProcessingPriority.SUPREMUM_PRIORITY
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
        true      | true    || ProcessingPriority.SUPREMUM_PRIORITY
        true      | false   || ProcessingPriority.SUPREMUM_PRIORITY
        false     | true    || ProcessingPriority.MINIMUM_PRIORITY
        false     | false   || ProcessingPriority.SUPREMUM_PRIORITY
    }

    @Unroll
    void "test getMinimumProcessingPriorityForOccupyingASlot with #runningProcesses running processes "() {
        given:
        prepareTestGetMinimumProcessingPriorityForOccupyingASlot(runningProcesses)

        expect:
        job.minimumProcessingPriorityForOccupyingASlot == priority

        where:
        runningProcesses || priority
        3                || ProcessingPriority.SUPREMUM_PRIORITY
        2                || ProcessingPriority.FAST_TRACK_PRIORITY
        1                || ProcessingPriority.MINIMUM_PRIORITY
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
