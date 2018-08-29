package de.dkfz.tbi.otp.job.processing

import de.dkfz.tbi.otp.job.plan.JobExecutionPlan

/**
 * Interface for a Job which triggers the processing of a JobExecutionPlan.
 *
 * This interface can be used by any Job which decides whether a JobExecutionPlan can be executed
 * and transferred into a Process. A common example is a watchdog for a file in a specified directory.
 * If the file exists the StartJob will trigger the execution of its JobExecutionPlan.
 *
 * How the StartJob is triggered is completely open to the implementing class. It may be a cron job
 * or a thread just sleeping for some time.
 */
public interface StartJob {
    /**
     * @return The JobExecutionPlan which is triggered by this StartJob. May be null (for example when Grails is not
     * fully initialized yet).
     */
    public JobExecutionPlan getJobExecutionPlan()

    /**
     * This method can be implemented by the StartJob for the checking.
     * The implementing method must be wrapped so that a hibernate session is bound,
     * e.g. by using {@link AbstractStartJobImpl#doWithPersistenceInterceptor}
     *
     * It is recommended to use the {@code @Scheduled} annotation on this method.
     */
    public void execute()
}
