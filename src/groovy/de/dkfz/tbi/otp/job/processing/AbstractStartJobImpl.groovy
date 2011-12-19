package de.dkfz.tbi.otp.job.processing

import de.dkfz.tbi.otp.job.plan.JobExecutionPlan
import de.dkfz.tbi.otp.job.plan.StartJobDefinition
import de.dkfz.tbi.otp.job.scheduler.SchedulerService

import org.codehaus.groovy.grails.support.PersistenceContextInterceptor
import org.springframework.beans.factory.annotation.Autowired

/**
 * Abstract base class for {@link StartJob}s.
 *
 * This base class provides the basic functionality required by the
 * container to use a StartJob.
 * @see StartJob
 */
abstract class AbstractStartJobImpl implements StartJob {
    @Autowired
    SchedulerService schedulerService
    /**
     * Dependency Injection of PersistenceContextInterceptor
     */
    @Autowired
    PersistenceContextInterceptor persistenceInterceptor
    private JobExecutionPlan plan

    protected AbstractStartJobImpl() {}
    protected AbstractStartJobImpl(JobExecutionPlan plan) {
        this.plan = plan
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

    @Override
    public JobExecutionPlan getExecutionPlan() {
        return plan
    }

    @Override
    public String getVersion() {
        return ""
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
       List<Process> processes = Process.findAllByJobExecutionPlan(plan)
       if(processes.size() < plan.numberOfAllowedProcesses) {
           return true
       }
       return false
   }
}
