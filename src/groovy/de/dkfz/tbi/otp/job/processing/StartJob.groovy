package groovy.de.dkfz.tbi.otp.job.processing

/**
 * Interface for a groovy.de.dkfz.tbi.otp.job.processing.Job which triggers the processing of a JobExecutionPlan.
 *
 * This interface can be used by any groovy.de.dkfz.tbi.otp.job.processing.Job which decides whether a JobExecutionPlan can be executed
 * and transferred into a Process. A common example is a watchdog for a file in a specified directory.
 * If the file exists the StartJob will trigger the execution of its JobExecutionPlan.
 *
 * How the StartJob is triggered is completely open to the implementing class. It may be a cron job
 * or a thread just sleeping for some time.
 *
 **/
public interface StartJob {
    /**
     * @return The JobExecutionPlan which is triggered by this StartJob.
     **/
    public JobExecutionPlan getExecutionPlan()
}
