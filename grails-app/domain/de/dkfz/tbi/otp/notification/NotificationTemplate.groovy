package de.dkfz.tbi.otp.notification

import de.dkfz.tbi.otp.utils.Entity

/**
 * A template to be used by Notifications.
 * It just wraps the text and is used to be able to use one template for many Notifications.
 * E.g. a generic Template for all JobExecutionPlans.
 *
 *
 */
class NotificationTemplate implements Entity {
    String template

    static constraints = {
        template(size: 1..10000)
    }
}
