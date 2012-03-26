package de.dkfz.tbi.otp.notification

/**
 * Enumeration of the possible Notifications supported by OTP.
 *
 */
enum NotificationType {
    /**
     * A Process for a JobExecutionPlan started.
     */
    PROCESS_STARTED,
    /**
     * A Process for a JobExecutionPlan finished successfully.
     */
    PROCESS_SUCCEEDED,
    /**
     * A Process for a JobExecutionPlan failed
     */
    PROCESS_FAILED,
    /**
     * A ProcessingStep for a Process started.
     */
    PROCESS_STEP_STARTED,
    /**
     * A ProcessingStep for a Process finished.
     */
    PROCESS_STEP_FINISHED,
    /**
     * A ProcessingStep for a Process failed
     */
    PROCESS_STEP_FAILED
}
