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
    PROCESS_FAILED
}
