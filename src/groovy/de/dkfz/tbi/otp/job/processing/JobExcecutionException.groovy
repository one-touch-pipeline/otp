package de.dkfz.tbi.otp.job.processing

/**
* Exception thrown when jobs run into failure
*
* Exception is thrown when jobs run into failure and terminate therefore.
* As jobs themselves shall not catch exceptions they are caught by the container.
*/
class JobExcecutionException extends ProcessingException {

    public JobExcecutionException() {
        this("unknown")
    }

    public JobExcecutionException(String message) {
        super(message)
    }

    public JobExcecutionException(String message, Throwable cause) {
        super(message, cause)
    }

}
