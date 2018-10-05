package de.dkfz.tbi.otp.job.scheduler

/**
* Exception thrown when a scheduler's part cannot be persisted.
*
* As the Job System is designed to be always is sync with the
* database it is a rather critical exception when working parts
* of the scheduler cannot be persisted.
*/
class SchedulerPersistencyException extends SchedulerException {

    SchedulerPersistencyException() {
        this("unknown")
    }

    SchedulerPersistencyException(String message) {
        super(message)
    }

    SchedulerPersistencyException(String message, Throwable cause) {
        super(message, cause)
    }

}
