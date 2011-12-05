package de.dkfz.tbi.otp.notification

/**
 * A Trigger represents a Domain object for which Notifications are sent.
 * E.g. a JobExecutionPlan and it's Id.
 *
 */
class Trigger {
    /**
     * The Class for the Trigger.
     */
    TriggerClass clazz
    /**
     * The Domain Object id.
     */
    long triggerId

    static constraints = {
        triggerId(unique: 'clazz')
    }
}
