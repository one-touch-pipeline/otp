package de.dkfz.tbi.otp.job.processing

/**
 * Interface for Jobs which use the Portable Batch System for executing their task.
 *
 * A groovy.de.dkfz.tbi.otp.job.processing.Job using the PBS is special in that way that it does not control the execution of its actual
 * task. This is completely controlled by the PBS. The groovy.de.dkfz.tbi.otp.job.processing.Job could gain information about the state of
 * the execution of the task scheduled on the PBS, but this would increase the critical path of the
 * groovy.de.dkfz.tbi.otp.job.processing.Job execution where it is not save to shutdown the application. After a non-clean shutdown it would
 * be impossible to know whether the groovy.de.dkfz.tbi.otp.job.processing.Job has started the task on the PBS or not.
 *
 * Because of that it is recommended to keep a PbsJob as simple as possible by just triggering the
 * task execution on the PBS and returning afterwards. A PbsJob by that cannot decide whether the
 * execution of the job on the PBS succeeded or failed. A {@link ValidatingJob} is required to verify
 * the execution.
 *
 **/
public interface PbsJob extends Job {
    /**
     * List of Pbs Process IDs started by the PbsJob.
     * As long as the Job has not yet started the list may be empty.
     * But when the job has executed this list may not be empty.
     * @return List of process Ids started on the Pbs.
     **/
    List<String> getPbsIds()
    /**
     * The ID of the PBS Realm this PbsJob is operating on.
     * @return The Id of the PBS Realm
     **/
    Long getRealm()
}
