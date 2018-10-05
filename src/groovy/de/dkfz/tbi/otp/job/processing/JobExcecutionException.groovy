package de.dkfz.tbi.otp.job.processing

/**
* Exception thrown when jobs run into failure
*
* Exception is thrown when jobs run into failure and terminate therefore.
* As jobs themselves shall not catch exceptions they are caught by the container.
*/
class JobExcecutionException extends ProcessingException {

    JobExcecutionException() {
        this("unknown")
    }

    JobExcecutionException(String message) {
        super(message)
    }

    JobExcecutionException(String message, Throwable cause) {
        super(message, cause)
    }

}
