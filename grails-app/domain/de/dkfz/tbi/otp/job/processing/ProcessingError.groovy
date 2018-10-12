package de.dkfz.tbi.otp.job.processing

import de.dkfz.tbi.otp.job.plan.*
import de.dkfz.tbi.otp.utils.*

/**
 * Domain class to store error information for a failure ProcessingStepUpdate.
 *
 * An error can either be just a message provided by the Job itself or in cause of an exception
 * in which case the class also contains an identifier for the stack trace. The stack trace itself
 * is not stored in the database but should be stored to a log file by the container. The identifier
 * allows to find the stack trace in the corresponding log file by an administrator.
 *
 * @see ProcessingStepUpdate
 */
class ProcessingError implements Entity {
    static belongsTo = [processingStepUpdate: ProcessingStepUpdate]
    /**
     * The ProcessingStepUpdate this error has been logged for.
     */
    ProcessingStepUpdate processingStepUpdate
    /**
     * The error message which can be shown in the user interface.
     */
    String errorMessage
    /**
     * A stack trace identifier to find the corresponding stack trace in a log file.
     * This could be a MD5 sum for example.
     *
     * If the error is not an exception this field should be {@code null}.
     */
    String stackTraceIdentifier

    static mapping = {
        errorMessage type: 'text'
    }

    static constraints = {
        processingStepUpdate(nullable: false, validator: { val ->
            return val.state == ExecutionState.FAILURE
        })
        errorMessage(nullable: false, blank: false)
        stackTraceIdentifier(nullable: true)
    }

    ProcessParameterObject getProcessParameterObject() {
        return processingStepUpdate.processParameterObject
    }

    ProcessingStep getProcessingStep() {
        return processingStepUpdate.processingStep
    }

    JobDefinition getJobDefinition() {
        return processingStepUpdate.jobDefinition
    }

}
