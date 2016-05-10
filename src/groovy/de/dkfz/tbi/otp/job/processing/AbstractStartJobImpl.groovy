package de.dkfz.tbi.otp.job.processing

import de.dkfz.tbi.otp.tracking.*
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.dataprocessing.ProcessingPriority
import de.dkfz.tbi.otp.job.plan.JobExecutionPlan
import de.dkfz.tbi.otp.job.plan.StartJobDefinition
import de.dkfz.tbi.otp.job.scheduler.SchedulerService
import grails.util.Environment

import org.codehaus.groovy.grails.support.PersistenceContextInterceptor
import org.hibernate.Hibernate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationListener

/**
 * Abstract base class for {@link StartJob}s.
 *
 * This base class provides the basic functionality required by the
 * container to use a StartJob.
 * @see StartJob
 */
abstract class AbstractStartJobImpl implements StartJob, ApplicationListener<JobExecutionPlanChangedEvent> {

    static final String TOTAL_SLOTS_OPTION_NAME = 'numberOfJobs'
    static final String SLOTS_RESERVED_FOR_FAST_TRACK_OPTION_NAME = 'numberOfJobsReservedForFastTrack'

    @Autowired
    ProcessingOptionService optionService

    @Autowired
    SchedulerService schedulerService

    @Autowired
    TrackingService trackingService

    /**
     * This variable is used by code which is generated by the AST transformation {@link de.dkfz.tbi.otp.job.ast.StartJobTransformation}.
     */
    @Autowired
    PersistenceContextInterceptor persistenceInterceptor

    private JobExecutionPlan plan
    /**
     * Whether the JobExecutionPlan needs to be refreshed when it is
     * accessed. This happens when receiving a JobExecutionPlanChangedEvent
     * for the JobExecutionPlan belonging to this StartJob.
     */
    private boolean planNeedsRefresh = false

    protected AbstractStartJobImpl() {}
    protected AbstractStartJobImpl(JobExecutionPlan plan) {
        this.plan = plan
    }

    protected abstract String getJobExecutionPlanName()

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
     * If looks for the start job definition based on the name of the current start job class,
     * then for the job execution plan by that job definition.
     */
    private initializeJobExecutionPlan() {
        if (!schedulerService.active) {
            return
        }
        if (plan == null && Environment.current != Environment.TEST) {
            final String name = jobExecutionPlanName
            if (name == null) {
                return
            }
            final Collection<JobExecutionPlan> plans
            try {
                plans = JobExecutionPlan.findAllByNameAndObsoleted(name, false)
            } catch (MissingMethodException ignored) {
                //This happens if this method is called before Grails created dynamic finders
                //in the domain classes. It doesn't matter because this method will be called later again.
                return
            }
            final int planCount = plans.size()
            if (planCount == 1) {
                plan = plans.first()
            } else if (planCount == 0) {
                log.info "No non-obsolete JobExecutionPlan found for name ${name}."
            } else {
                throw new RuntimeException("${planCount} non-obsoleted JobExecutionPlans found for name ${name}. Expected 1.")
            }
        }
    }

    @Override
    // TODO: OTP-1012
    public JobExecutionPlan getExecutionPlan() {
        initializeJobExecutionPlan()
        if (planNeedsRefresh) {
            plan = JobExecutionPlan.get(plan.id)
            planNeedsRefresh = false
        }
        return plan
    }

    @Override
    public String getVersion() {
        return ""
    }

    void onApplicationEvent(JobExecutionPlanChangedEvent event) {
        if (getExecutionPlan() && event.planId == this.plan.id) {
            planNeedsRefresh = true
        }
    }

    /**
     * Convenient method to access the JobExecutionPlan's StartJobDefinition.
     * @return The StartJobDefiniton
     * @throws InvalidStateException in case the JobExecutionPlan is not yet set.
     */
    protected final StartJobDefinition getStartJobDefinition() throws InvalidStateException {
        if (!plan) {
            throw new InvalidStateException("Execution plan has not been set")
        }
        return StartJobDefinition.get(plan.startJob.id)
    }

    protected void createProcess(List<Parameter> input, ProcessParameter processParameter = null) {
        schedulerService.createProcess(this, input, processParameter)
    }

    protected void createProcess(Object processParameter) {
        createProcess([], new ProcessParameter(value: processParameter.id.toString(), className: Hibernate.getClass(processParameter).name))
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
     *     <li>Whether the {@link JobExecutionPlan} (returned from {@link #getExecutionPlan()}) is obsolete or disabled.</li>
     *     <li>The number of {@link Process}es which are already running for the {@link JobExecutionPlan}.</li>
     *     <li>The maximum number of parallel {@link Process}es for the {@link JobExecutionPlan} (as configured in the
     *         {@link ProcessingOption} with the name specified by {@link #TOTAL_SLOTS_OPTION_NAME}).</li>
     *     <li>The maximum number of slots reserved for fast track (as configured in the {@link ProcessingOption} with
     *         the name specified by {@link #SLOTS_RESERVED_FOR_FAST_TRACK_OPTION_NAME}).</li>
     * </ul>
     */
    protected short getMinimumProcessingPriorityForOccupyingASlot() {
        final JobExecutionPlan plan = getExecutionPlan()
        if (!plan || plan.obsoleted || !plan.enabled) {
            return ProcessingPriority.SUPREMUM_PRIORITY
        }
        final int occupiedSlots = Process.countByFinishedAndJobExecutionPlan(false, plan)
        final int totalSlots = getConfiguredSlotCount(plan, TOTAL_SLOTS_OPTION_NAME, 1)
        if (occupiedSlots >= totalSlots) {
            return ProcessingPriority.SUPREMUM_PRIORITY
        }
        final int slotsReservedForFastTrack = getConfiguredSlotCount(plan, SLOTS_RESERVED_FOR_FAST_TRACK_OPTION_NAME, 0)
        if (occupiedSlots < totalSlots - slotsReservedForFastTrack) {
            return ProcessingPriority.NORMAL_PRIORITY
        } else {
            return ProcessingPriority.FAST_TRACK_PRIORITY
        }
    }

    protected int getConfiguredSlotCount(final JobExecutionPlan plan, final String optionName, final int defaultValue) {
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

   // TODO: OTP-1189: Probably remove this method.
   /**
    * Returns if it is allowed to create a new process.
    *
    * {@code true} is returned if it is allowed to create a new {@link Process}
    * for the given {@link JobExecutionPlan} {@code false} otherwise.
    * @return boolean indicating if it is allowed to create new {@link Process}
    *
    * @deprecated Use {@link #getMinimumProcessingPriorityForOccupyingASlot()} instead.
    */
   @Deprecated
   protected boolean isNewProcessAllowed() {
       if (plan.numberOfAllowedProcesses <= -1) {
           return true
       }
       return Process.countByJobExecutionPlanAndFinished(plan, false) < plan.numberOfAllowedProcesses
   }
}
