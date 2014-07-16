package de.dkfz.tbi.otp.job.processing

import de.dkfz.tbi.otp.job.plan.JobExecutionPlan
import de.dkfz.tbi.otp.job.plan.StartJobDefinition
import de.dkfz.tbi.otp.job.scheduler.SchedulerService
import grails.util.Environment

import java.util.concurrent.ExecutorService

import org.codehaus.groovy.grails.support.PersistenceContextInterceptor
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
    @Autowired
    SchedulerService schedulerService
    /**
     * Dependency Injection of PersistenceContextInterceptor
     */
    @Autowired
    PersistenceContextInterceptor persistenceInterceptor
    @Autowired
    ExecutorService executorService
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

    protected void createProcess(ProcessParameter processParameter) {
        createProcess([], processParameter)
    }

   /**
    * Returns if it is allowed to create a new process.
    *
    * {@code true} is returned if it is allowed to create a new {@link Process}
    * for the given {@link JobExecutionPlan} {@code false} otherwise.
    * @return boolean indicating if it is allowed to create new {@link Process}
    */
   protected boolean isNewProcessAllowed() {
       if (plan.numberOfAllowedProcesses <= -1) {
           return true
       }
       return Process.countByJobExecutionPlanAndFinished(plan, false) < plan.numberOfAllowedProcesses
   }
}
