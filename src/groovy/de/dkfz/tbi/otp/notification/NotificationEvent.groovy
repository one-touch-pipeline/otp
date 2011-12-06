package de.dkfz.tbi.otp.notification

import org.springframework.context.ApplicationEvent

/**
 * This event class can be used to publish notification events. That is publishing an
 * instance of this class might result in Notifications sent to users.
 *
 */
class NotificationEvent extends ApplicationEvent {
    /**
     * The Payload - can be generic and is specific to the type. Because of that of dynamic type.
     */
    def payload
    /**
     * The type of the event.
     */
    NotificationType type

    NotificationEvent(Object source, def payload, NotificationType type) {
        super(source)
        this.payload = payload
        this.type = type
    }
}
