package de.dkfz.tbi.otp.administration

import org.springframework.security.access.prepost.PostAuthorize
import org.springframework.security.access.prepost.PostFilter
import org.springframework.security.access.prepost.PreAuthorize

import de.dkfz.tbi.otp.notification.Notification
import de.dkfz.tbi.otp.notification.NotificationMedium
import de.dkfz.tbi.otp.notification.NotificationTemplate
import de.dkfz.tbi.otp.notification.NotificationType
import de.dkfz.tbi.otp.notification.Trigger;
import de.dkfz.tbi.otp.notification.TriggerClass;

/**
 * Helper Service to manipulate Notifications from the Web Interface.
 *
 */
class NotificationService {

    /**
     * Accesses the Notification in a security aware manner.
     * @param id The Notification's id
     * @return
     */
    @PostAuthorize("(returnObject == null) or hasPermission(returnObject, read) or hasRole('ROLE_ADMIN')")
    Notification getNotification(long id) {
        return Notification.get(id)
    }

    /**
     * Accesses the NotificationTemplate in a security aware manner.
     * @param id The NotificationTemplate's id
     * @return
     */
    @PostAuthorize("(returnObject == null) or hasPermission(returnObject, read) or hasRole('ROLE_ADMIN')")
    NotificationTemplate getNotificationTemplate(long id) {
        return NotificationTemplate.get(id)
    }

    @PostAuthorize("(returnObject == null) or hasPermission(returnObject, read) or hasRole('ROLE_ADMIN')")
    Trigger getNotificationTrigger(long id) {
        return Trigger.get(id)
    }

    /**
     * Retrieves all Notifications the User has access to.
     * @return
     */
    @PostFilter("hasPermission(filterObject, read) or hasRole('ROLE_ADMIN')")
    List<Notification> getAllNotifications() {
        return Notification.list([sort: 'id', order: "asc"])
    }

    /**
     * Updates the NotificationType of the given Notification
     * @param notification The Notification to update
     * @param type The new NotificationType
     * @return true if saved successfully, false if validation or save failed
     */
    @PreAuthorize("hasPermission(#notification, write) or hasRole('ROLE_ADMIN')")
    boolean updateNotificationType(Notification notification, NotificationType type) {
        notification.type = type
        return updateNotification(notification)
    }

    /**
     * Updates the NotificationMedium of the given Notification
     * @param notification The Notification to update
     * @param medium The new NotificationMedium
     * @return true if saved successfully, false if validation or save failed
     */
    @PreAuthorize("hasPermission(#notification, write) or hasRole('ROLE_ADMIN')")
    boolean updateNotificationMedium(Notification notification, NotificationMedium medium) {
        notification.medium = medium
        return updateNotification(notification)
    }

    /**
     * Enables/Disables the given Notification.
     * @param notification The Notification to update
     * @param enabled The new enabled state of the Notification
     * @return true if saved successfully, false if validation or save failed
     */
    @PreAuthorize("hasPermission(#notification, write) or hasRole('ROLE_ADMIN')")
    boolean enableNotification(Notification notification, boolean enabled) {
        notification.enabled = enabled
        return updateNotification(notification)
    }

    /**
     * Updates the text of the given NotificationTemplate.
     * @param template The Template to update
     * @param text The new text of the template
     * @return true if saved successfully, false if validation or save failed
     */
    @PreAuthorize("hasPermission(#template, write) or hasRole('ROLE_ADMIN')")
    boolean updateTemplate(NotificationTemplate template, String text) {
        template.template = text
        if (!template.validate()) {
            return false
        }
        if (!template.save(flush: true)) {
            return false
        }
        return true
    }

    /**
     * Updates the given Trigger to be triggered by the given object
     * @param trigger The Trigger to update
     * @param object The domain object which is the actual trigger
     * @return true if saved successfully, false if validation or save failed
     */
    boolean updateNotificationTrigger(Trigger trigger, def object) {
        TriggerClass clazz = TriggerClass.findOrSaveByClassName(object.class.getName())
        trigger.clazz = clazz
        trigger.triggerId = object.id
        if (!trigger.validate()) {
            return false
        }
        if (!trigger.save(flush: true)) {
            return false
        }
        return true
    }

    /**
     * Helper function to save the Notification with status.
     * @param notification
     * @return true if saved successfully, false otherwise
     */
    private boolean updateNotification(Notification notification) {
        if (!notification.validate()) {
            return false
        }
        if (!notification.save(flush: true)) {
            return false
        }
        return true
    }
}
