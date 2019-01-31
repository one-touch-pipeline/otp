package de.dkfz.tbi.otp.job.processing

enum NextAction {
    /**
     * Finish the execution of this job and mark it as succeeded.
     */
    SUCCEED,
    /**
     * Wait for the submitted cluster jobs to finish and then notify this job.
     */
    WAIT_FOR_CLUSTER_JOBS,
}
