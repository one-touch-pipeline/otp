package de.dkfz.tbi.otp.notification

import de.dkfz.tbi.otp.job.plan.JobExecutionPlan
import de.dkfz.tbi.otp.job.processing.Process
import de.dkfz.tbi.otp.job.processing.ProcessingStep
import de.dkfz.tbi.otp.security.Role
import de.dkfz.tbi.otp.security.User
import de.dkfz.tbi.otp.security.UserRole
import groovy.text.SimpleTemplateEngine
import grails.plugin.mail.MailService
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.plugins.springsecurity.acl.AclSid
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Scope
import org.springframework.context.ApplicationEvent
import org.springframework.context.ApplicationListener
import org.springframework.stereotype.Component

/**
 * The NotificationListener is an {@link ApplicationLister} for {@link NotificationEvent}s.
 * When a notification event is intercepted it starts a thread to process the
 * event.
 *
 * If there is a (or many) {@link Notification}(s) configured for this event (depending on the
 * payload and the type) a notification is sent to the users configured for the found Notification.
 * The message text of the notification is generated through the groovy template engine using the
 * configured {@link NotificationTemplate}. The template engine gets some parameters added to the
 * binding so that personalized messages can be generated.
 *
 * The actual message is then passed to a specific implementation for sending out the message. At
 * that point it is still possible that no notification is sent as the user might be missing e.g.
 * an email adddress.
 *
 *
 */
@Component("notificationListener")
@Scope("singleton")
class NotificationListener implements ApplicationListener {
    @Autowired
    ExecutorService executorService
    @Autowired
    MailService mailService
    @Autowired
    JabberService jabberService
    @Autowired
    GrailsApplication grailsApplication

    /**
     * The Application Event listener waiting for NotificationEvents and passing them
     * to {@link handleNotification} in an own Thread.
     */
    public void onApplicationEvent(ApplicationEvent event) {
        if (!(event instanceof NotificationEvent)) {
            return
        }
        executorService.submit({
            handleNotification(event)
        } as Callable)
    }

    /**
     * Dispatcher for NotificationEvents
     * @param event The NotificationEvent.
     */
    private void handleNotification(NotificationEvent event) {
        switch (event.type) {
        case NotificationType.PROCESS_STARTED:
        case NotificationType.PROCESS_SUCCEEDED:
        case NotificationType.PROCESS_FAILED:
            handleProcessNotification(event)
            break
        case NotificationType.PROCESS_STEP_STARTED:
        case NotificationType.PROCESS_STEP_FAILED:
            handleProcessingStepNotification(event)
            break
        default:
            // type which we cannot handle
            break
        }
    }

    /**
     * Method for Process related events.
     * It gets the Notifications and sets up the binding for Process related notifications.
     * @param event The NotificationEvent to process
     */
    private void handleProcessNotification(NotificationEvent event) {
        if (!(event.payload instanceof Process) && (!event.payload instanceof Map)) {
            return
        }
        Process process = Process.get((event.payload instanceof Process) ? event.payload.id : event.payload.process.id)
        // for a Process the Trigger is the JobExecutionPlan
        List<Notification> notifications = resolveNotifications(JobExecutionPlan.class.getName(), process.jobExecutionPlan.id, event.type)
        if (notifications.isEmpty()) {
            // No Notifications configured for the JobExecutionPlan
            return
        }
        Map binding = [process: process, error: (event.payload instanceof Map) ? event.payload.error : null]
        sendNotifications(notifications, binding)
    }

    /**
     * Method for Processing Step related events.
     * It gets the Notifications and sets up the binding containing Process, ProcessingStep, Input Parameters and JobDefinition.
     * @param event
     */
    private void handleProcessingStepNotification(NotificationEvent event) {
        if (!(event.payload instanceof ProcessingStep) && (!event.payload instanceof Map)) {
            return
        }
        ProcessingStep step = ProcessingStep.get((event.payload instanceof ProcessingStep) ? event.payload.id : event.payload.processingStep.id)
        // for a ProcessingStep the Trigger is the JobExecutionPlan
        List<Notification> notifications = resolveNotifications(JobExecutionPlan.class.getName(), step.process.jobExecutionPlan.id, event.type)
        if (notifications.isEmpty()) {
            // No Notifications configured for the JobExecutionPlan
            return
        }
        Map binding = [process: step.process, step: step, input: step.input, jobDefinition: step.jobDefinition, error: (event.payload instanceof Map) ? event.payload.error : null]
        sendNotifications(notifications, binding)
    }

    /**
     * Helper method to retrieve all Notifications for a given Trigger.
     * @param className The trigger's class name (just package.ClassName)
     * @param id The Object Id
     * @param type The Type of the Notification
     * @return List of Notification configured for the Trigger
     */
    private List<Notification> resolveNotifications(String className, long id, NotificationType type) {
        TriggerClass clazz = TriggerClass.findByClassName(className)
        if (!clazz) {
            return []
        }
        Trigger trigger = Trigger.findByClazzAndTriggerId(clazz, id)
        if (!trigger) {
            return []
        }
        return Notification.findAllByTriggerAndType(trigger, type)
    }

    /**
     * Helper class to send out Notifications with the specific binding for the template engine.
     * @param notifications List of Notifications
     * @param binding The binding to be used for the Notifications
     */
    private void sendNotifications(List<Notification> notifications, Map binding) {
        notifications.each { Notification notification ->
            if (!notification.enabled) {
                return
            }
            // subject
            String subject = ""
            if (notification.subjectTemplate) {
                subject = applyTemplate(notification.subjectTemplate, binding)
            }
            Set<User> users = getUsers(notification)
            users.each { User user ->
                binding.put("user", user)
                String text = applyTemplate(notification.template, binding)
                if (notification.medium == NotificationMedium.EMAIL) {
                    executorService.submit({
                        sendMailNotification(user, subject, text)
                    } as Callable)
                } else if (notification.medium == NotificationMedium.JABBER) {
                    executorService.submit({
                        jabberService.sendNotification(user, text)
                    } as Callable)
                }
            }
        }
    }

    /**
     * Sends a mail notification
     * @param user The recipient of the mail
     * @param subjectText The Subject
     * @param bodyText The body
     */
    private void sendMailNotification(User user, String subjectText, String bodyText) {
        if (!user.email) {
            return
        }
        mailService.sendMail {
            to user.email
            from grailsApplication.config.otp.mail.sender
            subject subjectText
            body bodyText
        }
    }

    /**
     * Retrieves all Users configured to receive the Notification
     * @param notication The Notification for which the configured Users should be retrieved
     * @return List of Users for this Notification.
     */
    private Set<User> getUsers(Notification notication) {
        Set<User> users = []
        notication.recipients.each { Recipient recipient ->
            if (recipient instanceof UsersRecipient) {
                users.addAll((recipient as UsersRecipient).users)
            } else if (recipient instanceof SidRecipient) {
                AclSid sid = (recipient as SidRecipient).sid
                if (sid.principal) {
                    users.add(User.findByUsername(sid.sid))
                } else {
                    // sid is a role
                    Role role = Role.findByAuthority(sid.sid)
                    List<UserRole> roles = UserRole.findAllByRole(role)
                    roles.each { UserRole userRole ->
                        users.add(userRole.user)
                    }
                }
            }
        }
        return users
    }

    /**
     * Helper method to apply a Template
     * @param template The Template
     * @param binding The Binding
     * @return Templated String
     */
    private String applyTemplate(NotificationTemplate template, Map binding) {
        SimpleTemplateEngine engine = new SimpleTemplateEngine()
        return engine.createTemplate(template.template).make(binding).toString()
    }
}
