package de.dkfz.tbi.otp.notification

/**
 * A Notification describes who should be notified when about what in which way.
 *
 */
class Notification {
    /**
     * The recipients of this Notification.
     */
    static hasMany = [recipients: Recipient]
    /**
     * The Trigger indicating that a Notification has to be sent.
     * This is the class which is used when a notification event occurs.
     * E.g. a JobExecutionPlan
     */
    Trigger trigger
    /**
     * The Template to be used for the Body message
     */
    NotificationTemplate template
    /**
     * The optional Template for a subject. Only used by email.
     */
    NotificationTemplate subjectTemplate
    /**
     * The type of the notification. E.g ProcessFinished.
     */
    NotificationType type
    /**
     * How to send the notification.
     */
    NotificationMedium medium
    /**
     * Whether this Notification is enabled.
     */
    boolean enabled

    static mapping = {
        recipients(lazy: false)
    }

    static constraints = {
        trigger(nullable: false)
        template(nullable: false)
        subjectTemplate(nullable: true)
    }
}
