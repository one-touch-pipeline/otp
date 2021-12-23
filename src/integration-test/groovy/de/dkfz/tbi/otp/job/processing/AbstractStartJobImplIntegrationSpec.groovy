/*
 * Copyright 2011-2020 The OTP authors
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

import grails.testing.mixin.integration.Integration
import grails.gorm.transactions.Rollback
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.job.plan.JobExecutionPlan
import de.dkfz.tbi.otp.job.scheduler.SchedulerService
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.workflowExecution.ProcessingPriority

@Rollback
@Integration
class AbstractStartJobImplIntegrationSpec extends Specification {

    AbstractStartJobImpl job
    JobExecutionPlan jep

    void setupData() {
        jep = DomainFactory.createJobExecutionPlan(enabled: true)

        job = [
                getJobExecutionPlan: { -> jep },
        ] as AbstractStartJobImpl
        job.optionService = new ProcessingOptionService()

        job.schedulerService = Mock(SchedulerService) {
            _ * isActive() >> true
        }

        3.times {
            DomainFactory.createProcess(jobExecutionPlan: jep, finished: true)
        }
    }

    void "test getMinimumProcessingPriorityForOccupyingASlot, no JobExecutionPlan"() {
        given:
        setupData()
        job = [
                getJobExecutionPlan: { -> null },
        ] as AbstractStartJobImpl
        job.schedulerService = Mock(SchedulerService) {
            _ * isActive() >> true
        }

        expect:
        job.minimumProcessingPriorityForOccupyingASlot == ProcessingPriority.SUPREMUM
    }

    void "test getMinimumProcessingPriorityForOccupyingASlot, job system is disabled"() {
        given:
        setupData()
        job.schedulerService = Mock(SchedulerService) {
            _ * isActive() >> false
        }

        expect:
        job.minimumProcessingPriorityForOccupyingASlot == ProcessingPriority.SUPREMUM
    }

    @Unroll
    void "test getMinimumProcessingPriorityForOccupyingASlot, JobExecutionPlan obsoleted #obsoleted enabled #enabled"() {
        given:
        setupData()
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
        setupData()
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
