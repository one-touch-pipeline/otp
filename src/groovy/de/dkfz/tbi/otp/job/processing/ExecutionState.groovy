package de.dkfz.tbi.otp.job.processing

public enum ExecutionState {
    /** The Job has been created but not yet started*/
    CREATED,
    /** The Job has been started, that is the processing is running*/
    STARTED,
    /** The execution of the Job finished, but it is still unknown whether it succeeded or failed*/
    FINISHED,
    /** The execution of the Job finished successfully*/
    SUCCESS,
    /** The execution of the Job failed*/
    FAILURE,
    /** The Job has been restarted manually.
     *  The only allowed state before is FAILURE. No further update can follow after restarted. A new ProcessingStep is created.
     */
    RESTARTED,
    /** The execution of the Job has been suspended for a save shutdown. This state is only allowed after STARTED or RESUMED*/
    SUSPENDED,
    /** The execution of a suspended Job has been resumed. This state is only allowed after the state SUSPENDED.*/
    RESUMED
}
