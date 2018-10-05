package de.dkfz.tbi.otp.job.processing

import de.dkfz.tbi.otp.job.scheduler.SchedulerException

/**
* Exception thrown when processing fails
*
* Exception is thrown when processing has error and
* therefore fails. Errors could be wrong parameters
* or a wrong processing step ordering, for example.
*/
class IncorrectProcessingException extends SchedulerException {

    IncorrectProcessingException() {
        this("unknown")
    }

    IncorrectProcessingException(String message) {
        super(message)
    }

    IncorrectProcessingException(String message, Throwable cause) {
        super(message, cause)
    }

}
