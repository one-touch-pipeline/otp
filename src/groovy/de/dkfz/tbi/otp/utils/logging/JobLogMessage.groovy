package de.dkfz.tbi.otp.utils.logging

import de.dkfz.tbi.otp.job.processing.ProcessingStep

/**
 * Simple wrapper around a message which needs to be logged together
 * with a ProcessingStep.
 *
 * This class is needed to log messages in the Jobs. As the log output
 * of each Job should go into an independent file and the logging should
 * be as convenient to the developers as possible the {@link JobLog} takes
 * care of logging instances of this class to wrap the actual message.
 *
 * The Appender can take care about adjusting the actual logging to the
 * resource by taking the ProcessingStep into account.
 *
 *
 */
class JobLogMessage {

    /**
     * The original message logged by the Job.
     */
    Object message
    /**
     * The ProcessingStep for which the message got logged.
     */
    ProcessingStep processingStep

    public JobLogMessage(Object message, ProcessingStep processingStep) {
        this.message = message
        this.processingStep = processingStep
    }

    @Override
    public String toString() {
        return "[ProcessingStep ${processingStep.id}] ${message}"
    }
}
