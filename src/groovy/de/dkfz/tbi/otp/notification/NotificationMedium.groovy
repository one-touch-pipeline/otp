package de.dkfz.tbi.otp.notification

/**
 * Enumeration of the available media how to send notifications.
 *
 */
enum NotificationMedium {
    /**
     * Notification should be sent by mail.
     */
    EMAIL,
    /**
     * Notification should be sent by XMPP.
     */
    JABBER
}
