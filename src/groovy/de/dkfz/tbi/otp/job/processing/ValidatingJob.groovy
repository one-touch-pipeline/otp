package de.dkfz.tbi.otp.job.processing

/**
 * Interface for a groovy.de.dkfz.tbi.otp.job.processing.Job which is allowed to update the state of a previously run groovy.de.dkfz.tbi.otp.job.processing.Job.
 *
 * This interface can be used to verify that other groovy.de.dkfz.tbi.otp.job.processing.Job(s) executed successfully or failed. The groovy.de.dkfz.tbi.otp.job.processing.Job
 * should use its execute method to perform the check. It is possible that the groovy.de.dkfz.tbi.otp.job.processing.Job execution takes
 * a long time as it has to be checked whether a long-running task finished. In such a case the groovy.de.dkfz.tbi.otp.job.processing.Job
 * should ensure that it sleeps but never throw an exception due to threading. Remember throwing an
 * Exception will trigger that the Process fails and will require manual interaction.
 *
 * A validating job should never fail. It should always succeed. Because of that it is an
 * groovy.de.dkfz.tbi.otp.job.processing.EndStateAwareJob. It knows that it succeeded when the end of the execute method is reached.
 *
 **/
public interface ValidatingJob extends EndStateAwareJob {
    /**
     * @return List of the ProcessingStep this groovy.de.dkfz.tbi.otp.job.processing.Job is validating.
     **/
    public List<ProcessingStep> getValidatorFor();
}
