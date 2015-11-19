package de.dkfz.tbi.otp.job.processing

public enum ExecutionState {
    /** The groovy.de.dkfz.tbi.otp.job.processing.Job has been created but not yet started*/
    CREATED,
    /** The groovy.de.dkfz.tbi.otp.job.processing.Job has been started, that is the processing is running*/
    STARTED,
    /** The execution of the groovy.de.dkfz.tbi.otp.job.processing.Job finished, but it is still unknown whether it succeeded or failed*/
    FINISHED,
    /** The execution of the groovy.de.dkfz.tbi.otp.job.processing.Job finished successfully*/
    SUCCESS,
    /** The execution of the groovy.de.dkfz.tbi.otp.job.processing.Job failed*/
    FAILURE,
    /** The groovy.de.dkfz.tbi.otp.job.processing.Job has been restarted manually.
     *  The only allowed state before is FAILURE. No further update can follow after restarted. A new ProcessingStep is created.
     **/
    RESTARTED,
    /** The execution of the groovy.de.dkfz.tbi.otp.job.processing.Job has been suspended for a save shutdown. This state is only allowed after STARTED or RESUMED*/
    SUSPENDED,
    /** The execution of a suspended groovy.de.dkfz.tbi.otp.job.processing.Job has been resumed. This state is only allowed after the state SUSPENDED.*/
    RESUMED
}
