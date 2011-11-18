package de.dkfz.tbi.otp.job.processing

import de.dkfz.tbi.otp.job.plan.JobExecutionPlan

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

    /**
     * Returns a unique version identifier of this class version.
     *
     * The version is the git SHA hash of the last change. The code of this method in implementing
     * classes gets auto-generated through an AST Transformation.
     *
     * Although the code gets generated a dummy implementation can be added just to make the IDE
     * happy.
     * @return Unique identifier of the source code version of the class.
     **/
    public String getVersion()
    /**
     * This method can be implemented by the StartJob for the checking.
     * The implementing method is wrapped into a construct so that a hibernate session is bound in
     * case that the Job extends the AbstractJobImpl.
     *
     * It is recommended to use the {@code @Scheduled} annotation on this method.
     */
    public void execute()
}
