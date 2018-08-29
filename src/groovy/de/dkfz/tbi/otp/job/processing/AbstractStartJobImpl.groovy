package de.dkfz.tbi.otp.job.processing

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName
import de.dkfz.tbi.otp.job.plan.*
import de.dkfz.tbi.otp.job.scheduler.*
import de.dkfz.tbi.otp.tracking.*
import grails.util.*
import org.codehaus.groovy.grails.support.*
import org.hibernate.*
import org.springframework.beans.factory.*
import org.springframework.beans.factory.annotation.*
import org.springframework.context.*


/**
 * Abstract base class for {@link StartJob}s.
 *
 * This base class provides the basic functionality required by the
 * container to use a StartJob.
 *
 * @see StartJob
 */
abstract class AbstractStartJobImpl implements StartJob, ApplicationListener<JobExecutionPlanChangedEvent>, BeanNameAware  {

    @Autowired
    ProcessingOptionService optionService

    @Autowired
    SchedulerService schedulerService

    @Autowired
    TrackingService trackingService

    @Autowired
    PersistenceContextInterceptor persistenceInterceptor

    private JobExecutionPlan plan
    private String beanName

    void setBeanName(String beanName) {
        this.beanName = beanName
    }

    public Object doWithPersistenceInterceptor(@SuppressWarnings("rawtypes") final Closure closure) {
        try {
            persistenceInterceptor.init()
            return closure.call();
        }
        finally {
            persistenceInterceptor.flush()
            persistenceInterceptor.destroy()
        }
    }

    /**
     * Sets the JobExecutionPlan of this StartJob if not already set.
     * @param plan The JobExecutionPlan this Job belongs to
     * @throws InvalidStateException If the plan has already been set
     */
    public final void setJobExecutionPlan(JobExecutionPlan plan) throws InvalidStateException {
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
    private initializeJobExecutionPlan() {
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
    public JobExecutionPlan getJobExecutionPlan() {
        initializeJobExecutionPlan()
        return plan
    }

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

    public static createProcessParameter(Object processParameter) {
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
        return minimumProcessingPriorityForOccupyingASlot <= ProcessingPriority.MAXIMUM_PRIORITY
    }

    /**
     * Returns the minimum {@link ProcessingPriority} value which data needs to have such that this {@link StartJob} is
     * allowed to create a new {@link Process} for processing that data.
     *
     * <p>
     * Takes into account:
     * <ul>
     *     <li>Whether the {@link JobExecutionPlan} (returned from {@link #getJobExecutionPlan()}) is obsolete or disabled.</li>
     *     <li>The number of {@link Process}es which are already running for the {@link JobExecutionPlan}.</li>
     *     <li>The maximum number of parallel {@link Process}es for the {@link JobExecutionPlan} (as configured in the
     *         {@link ProcessingOption} with the name specified by {@link OptionName#MAXIMUM_NUMBER_OF_JOBS}).</li>
     *     <li>The maximum number of slots reserved for fast track (as configured in the {@link ProcessingOption} with
     *         the name specified by {@link OptionName#MAXIMUM_NUMBER_OF_JOBS_RESERVED_FOR_FAST_TRACK}).</li>
     * </ul>
     */
    protected short getMinimumProcessingPriorityForOccupyingASlot() {
        final JobExecutionPlan plan = getJobExecutionPlan()
        if (!plan || plan.obsoleted || !plan.enabled) {
            return ProcessingPriority.SUPREMUM_PRIORITY
        }
        final int occupiedSlots = Process.countByFinishedAndJobExecutionPlan(false, plan)
        final int totalSlots = getConfiguredSlotCount(plan, OptionName.MAXIMUM_NUMBER_OF_JOBS, 1)
        if (occupiedSlots >= totalSlots) {
            return ProcessingPriority.SUPREMUM_PRIORITY
        }
        final int slotsReservedForFastTrack = getConfiguredSlotCount(plan, OptionName.MAXIMUM_NUMBER_OF_JOBS_RESERVED_FOR_FAST_TRACK, 0)
        if (occupiedSlots < totalSlots - slotsReservedForFastTrack) {
            return ProcessingPriority.MINIMUM_PRIORITY
        } else {
            return ProcessingPriority.FAST_TRACK_PRIORITY
        }
    }

    protected int getConfiguredSlotCount(final JobExecutionPlan plan, final OptionName optionName, final int defaultValue) {
        final String optionValue = optionService.findOption(optionName, plan.name, null)
        if (optionValue == null) {
            log.warn "${optionName} is not configured for ${plan.name}. Defaulting to ${defaultValue}."
            return defaultValue
        }
        try {
            final int slotCount = Integer.parseInt(optionValue)
            if (slotCount < 0) {
                throw new NumberFormatException()
            }
            return slotCount
        } catch (final NumberFormatException e) {
            throw new NumberFormatException("Illegal value of ${optionName} for ${plan.name}: \"${optionValue}\"")
        }
    }
}
