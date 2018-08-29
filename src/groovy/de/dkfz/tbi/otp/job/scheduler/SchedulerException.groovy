package de.dkfz.tbi.otp.job.scheduler

import de.dkfz.tbi.otp.job.processing.ProcessingException

/**
* Exception thrown when scheduler fails.
*
* As the scheduler handles the all jobs,
* a failure in it means that the application
* crashes completely. THerefore this is a
* fatal exception.
*/
class SchedulerException extends ProcessingException {

    public SchedulerException() {
        this("unknown")
    }

    public SchedulerException(String message) {
        super(message)
    }

    public SchedulerException(String message, Throwable cause) {
        super(message, cause)
    }

}
