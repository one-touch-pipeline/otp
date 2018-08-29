package de.dkfz.tbi.otp.job.processing

/**
 * Interface for a Job which is allowed to update the state of a previously run Job.
 *
 * This interface can be used to verify that other Job(s) executed successfully or failed. The Job
 * should use its execute method to perform the check. It is possible that the Job execution takes
 * a long time as it has to be checked whether a long-running task finished. In such a case the Job
 * should ensure that it sleeps but never throw an exception due to threading. Remember throwing an
 * Exception will trigger that the Process fails and will require manual interaction.
 *
 * A validating job should never fail. It should always succeed. Because of that it is an
 * EndStateAwareJob. It knows that it succeeded when the end of the execute method is reached.
 */
public interface ValidatingJob extends EndStateAwareJob {
    /**
     * @return ProcessingStep this Job is validating.
     */
    public ProcessingStep getValidatorFor()

    /**
     * Used by the Scheduler to add a concrete Processing Step to validate
     * based on the information of the ValidatingJobDefinition.
     *
     * @param step A Processing Step this Job is validating
     */
    public void setValidatorFor(ProcessingStep step)

    /**
     * Used by the Scheduler to determine whether the validated job succeeded
     * or failed.
     * In case the job failed the validated processing step is set to FAILURE and the Process is
     * marked as failed.
     * @return Whether the validated Job succeeded
     */
    public boolean hasValidatedJobSucceeded()
}
