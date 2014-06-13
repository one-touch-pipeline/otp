package de.dkfz.tbi.otp.utils

import grails.plugin.springsecurity.acl.AclClass
import grails.plugin.springsecurity.acl.AclSid

import de.dkfz.tbi.otp.notification.Notification
import de.dkfz.tbi.otp.notification.NotificationMedium
import de.dkfz.tbi.otp.notification.NotificationTemplate
import de.dkfz.tbi.otp.notification.NotificationType
import de.dkfz.tbi.otp.notification.Recipient
import de.dkfz.tbi.otp.notification.SidRecipient
import de.dkfz.tbi.otp.notification.Trigger
import de.dkfz.tbi.otp.notification.TriggerClass
import de.dkfz.tbi.otp.notification.UsersRecipient
import de.dkfz.tbi.otp.security.User

/**
 * The NotificationDSL allows to easily configure Notifications.
 *
 * An example DSL looks like the following:
 * {@code
 * import static de.dkfz.tbi.otp.utils.NotificationDSL.*
 * import de.dkfz.tbi.otp.notification.NotificationMedium
 * import de.dkfz.tbi.otp.notification.NotificationType
 *
 * notification(NotificationType.PROCESS_FAILED, NotificationMedium.EMAIL) {
 *     subject("Test Notification")
 *     message("A ProcessingStep failed")
 *     user("username")
 *     sid("ROLE_ADMIN")
 *     trigger(JobExecutionPlan.findByName("example"))
 * }
 * }
 *
 * This example sends out an email notification whenever a Process for the
 * JobExecutionPlan called "example" fails. The Notification is sent to the
 * user with the name "username" and to each user having the "ROLE_ADMIN".
 *
 *
 */
class NotificationDSL {

    /**
     * Creates the subject template for the Notification
     */
    private static def subjectClosure = { Notification n, String text ->
        NotificationTemplate subjectTemplate = new NotificationTemplate(template: text)
        assert(subjectTemplate.save())
        n.subjectTemplate = subjectTemplate
    }

    /**
     * Creates the message body template for the Notification
     */
    private static def messageClosure = { Notification n, String text ->
        NotificationTemplate messageTemplate = new NotificationTemplate(template: text)
        assert(messageTemplate.save())
        n.template = messageTemplate
    }

    /**
     * Configures the Trigger for the Notification
     */
    private static def triggerClosure = { Notification n, object, aclField ->
        TriggerClass triggerClass = TriggerClass.findOrSaveByClassName(object.class.getName())
        Trigger trigger = new Trigger(clazz: triggerClass, triggerId: object.id, aclField: aclField)
        trigger.save()
        n.trigger = trigger
    }

    /**
     * Adds the given role to the recipients of the Notification.
     * Requires that there is an AclSid for the Role.
     */
    private static def sidClosure = { Notification n, String roleName ->
        AclSid aclSid = AclSid.findBySid(roleName)
        assert(aclSid)
        SidRecipient sidRecipient = SidRecipient.findOrSaveBySid(aclSid)
        n.addToRecipients(sidRecipient)
    }

    /**
     * Adds the user identified by the given user name to the recipients of the Notification.
     * Requires that the User exists.
     */
    private static def userClosure = { Notification n, String userName ->
        User user = User.findByUsername(userName)
        assert(user)
        for (Recipient r in n.recipients) {
            if (r instanceof UsersRecipient) {
                (r as UsersRecipient).addToUsers(user)
                return
            }
        }
        UsersRecipient ur = new UsersRecipient()
        ur.addToUsers(user)
        ur.save()
        n.addToRecipients(ur)
    }

    /**
     * The Notification DSL. See class description on how to use it.
     */
    public static def notification = { NotificationType type, NotificationMedium medium, c ->
        Notification.withTransaction {
            Notification n = new Notification(type: type, medium: medium, enabled: true)
            c.metaClass = new ExpandoMetaClass(c.class)
            c.metaClass.subject = { String text ->
                NotificationDSL.subjectClosure(n, text)
            }
            c.metaClass.message = { String text ->
                NotificationDSL.messageClosure(n, text)
            }
            c.metaClass.trigger = { object, aclField = null ->
                NotificationDSL.triggerClosure(n, object, aclField)
            }
            c.metaClass.sid = { String roleName ->
                NotificationDSL.sidClosure(n, roleName)
            }
            c.metaClass.user = { String userName ->
                NotificationDSL.userClosure(n, userName)
            }
            c.metaClass.initialize()
            c()
            assert(n.save(flush: true))
        }
    }

}
