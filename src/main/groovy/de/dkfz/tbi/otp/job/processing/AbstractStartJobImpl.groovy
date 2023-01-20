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

import grails.gorm.transactions.Transactional
import grails.util.Environment
import groovy.transform.CompileDynamic
import org.hibernate.Hibernate
import org.springframework.beans.factory.BeanNameAware
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationListener

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName
import de.dkfz.tbi.otp.job.plan.JobExecutionPlan
import de.dkfz.tbi.otp.job.plan.StartJobDefinition
import de.dkfz.tbi.otp.job.scheduler.SchedulerService
import de.dkfz.tbi.otp.tracking.NotificationCreator
import de.dkfz.tbi.otp.workflowExecution.ProcessingPriority

/**
 * Abstract base class for {@link StartJob}s.
 *
 * This base class provides the basic functionality required by the
 * container to use a StartJob.
 *
 * @see StartJob
 */
@CompileDynamic
abstract class AbstractStartJobImpl implements StartJob, ApplicationListener<JobExecutionPlanChangedEvent>, BeanNameAware {

    @Autowired
    ProcessingOptionService optionService

    @Autowired
    SchedulerService schedulerService

    @Autowired
    NotificationCreator notificationCreator

    private JobExecutionPlan plan
    private String beanName

    @Override
    void setBeanName(String beanName) {
        this.beanName = beanName
    }

    /**
     * Sets the JobExecutionPlan of this StartJob if not already set.
     * @param plan The JobExecutionPlan this Job belongs to
     * @throws InvalidStateException If the plan has already been set
     */
    final void setJobExecutionPlan(JobExecutionPlan plan) throws InvalidStateException {
        if (this.plan) {
            throw new InvalidStateException("Execution plan has already been set")
        }
        this.plan = plan
    }

    /**
     * This method initializes the job execution plan if it is not set.
     * If looks for the start job definition based on the bean name of the current start job class,
     * then for the job execution plan by that job definition.
     */
    @Transactional(readOnly = true)
    @SuppressWarnings("ThrowRuntimeException") // ignored: will be removed with the old workflow system
    private void initializeJobExecutionPlan() {
        if (!schedulerService.active) {
            return
        }
        if (plan == null && Environment.current != Environment.TEST) {
            final String beanName = this.beanName //necessary for criteria
            final Collection<JobExecutionPlan> plans
            try {
                plans = JobExecutionPlan.createCriteria().list {
                    eq('obsoleted', false)
                    startJob {
                        eq('bean', beanName)
                    }
                }
            } catch (MissingMethodException ignored) {
                //This happens if this method is called before Grails created dynamic finders
                //in the domain classes. It doesn't matter because this method will be called later again.
                return
            }
            final int planCount = plans.size()
            if (planCount == 1) {
                plan = plans.first()
            } else if (planCount > 0) {
                throw new RuntimeException("${planCount} non-obsoleted JobExecutionPlans found for ${beanName}. Expected 1.")
            }
        }
    }

    @Override
    JobExecutionPlan getJobExecutionPlan() {
        initializeJobExecutionPlan()
        return plan
    }

    @SuppressWarnings("UnusedMethodParameter")
    void onApplicationEvent(JobExecutionPlanChangedEvent event) {
        // If the plan has been changed, clear the cache.
        plan = null
    }

    /**
     * Convenient method to access the JobExecutionPlan's StartJobDefinition.
     * @return The StartJobDefinition
     * @throws InvalidStateException in case the JobExecutionPlan is not yet set.
     */
    protected final StartJobDefinition getStartJobDefinition() throws InvalidStateException {
        if (!plan) {
            throw new InvalidStateException("Execution plan has not been set")
        }
        return StartJobDefinition.get(plan.startJob.id)
    }

    protected Process createProcess(Object processParameter) {
        return createProcess([], createProcessParameter(processParameter))
    }

    protected Process createProcess(List<Parameter> input, ProcessParameter processParameter = null) {
        return schedulerService.createProcess(this, input, processParameter)
    }

    static ProcessParameter createProcessParameter(Object processParameter) {
        return new ProcessParameter(value: processParameter.id.toString(), className: Hibernate.getClass(processParameter).name)
    }

    /**
     * Returns whether this {@link StartJob} is allowed to create a new {@link Process}.
     *
     * <p>
     * {@link StartJob}s which take processing priorities into account should use
     * {@link #getMinimumProcessingPriorityForOccupyingASlot()} instead.
     */
    protected boolean isFreeSlotAvailable() {
        return minimumProcessingPriorityForOccupyingASlot < ProcessingPriority.SUPREMUM
    }

    /**
     * Returns the minimum priority value which data needs to have such that this {@link StartJob} is
     * allowed to create a new {@link Process} for processing that data.
     *
     * <p>
     * Takes into account:
     * <ul>
     *     <li>Whether the {@link JobExecutionPlan} (returned from {@link #getJobExecutionPlan()}) is obsolete or disabled.</li>
     *     <li>The number of {@link Process}es which are already running for the {@link JobExecutionPlan}.</li>
     *     <li>The maximum number of parallel {@link Process}es for the {@link JobExecutionPlan} (as configured in the
     * {@link ProcessingOption} with the name specified by {@link OptionName#MAXIMUM_NUMBER_OF_JOBS}).</li>
     *     <li>The maximum number of slots reserved for fast track (as configured in the {@link ProcessingOption} with
     *         the name specified by {@link OptionName#MAXIMUM_NUMBER_OF_JOBS_RESERVED_FOR_FAST_TRACK}).</li>
     * </ul>
     */
    protected int getMinimumProcessingPriorityForOccupyingASlot() {
        if (!schedulerService.active) {
            return ProcessingPriority.SUPREMUM
        }
        final JobExecutionPlan plan = jobExecutionPlan
        if (!plan || plan.obsoleted || !plan.enabled) {
            return ProcessingPriority.SUPREMUM
        }

        final int occupiedSlots = Process.countByFinishedAndJobExecutionPlan(false, plan)
        final int totalSlots = optionService.findOptionAsInteger(OptionName.MAXIMUM_NUMBER_OF_JOBS, plan.name)
        if (occupiedSlots >= totalSlots) {
            return ProcessingPriority.SUPREMUM
        }
        final int slotsReservedForFastTrack = optionService.findOptionAsInteger(OptionName.MAXIMUM_NUMBER_OF_JOBS_RESERVED_FOR_FAST_TRACK, plan.name)
        if (occupiedSlots < totalSlots - slotsReservedForFastTrack) {
            return ProcessingPriority.MINIMUM
        }
        return ProcessingPriority.FAST_TRACK
    }
}
