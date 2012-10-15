package de.dkfz.tbi.otp.administration

import de.dkfz.tbi.otp.job.plan.JobDefinition
import de.dkfz.tbi.otp.job.plan.JobExecutionPlan
import de.dkfz.tbi.otp.notification.Notification
import de.dkfz.tbi.otp.notification.NotificationMedium
import de.dkfz.tbi.otp.notification.NotificationTemplate
import de.dkfz.tbi.otp.notification.NotificationType
import de.dkfz.tbi.otp.notification.Trigger
import de.dkfz.tbi.otp.utils.DataTableCommand
import grails.converters.JSON
import grails.plugins.springsecurity.Secured
import grails.util.GrailsNameUtils

/**
 * Controller to configure the Notifications.
 */
@Secured(['ROLE_ADMIN'])
class NotificationController {
    def jobExecutionPlanService
    def notificationService

    def index() {
        [workflows: jobExecutionPlanService.getAllJobExecutionPlans()]
    }

    def dataTableSource(DataTableCommand cmd) {
        Map dataToRender = cmd.dataToRender()
        dataToRender.iTotalRecords = Notification.count()
        dataToRender.iTotalDisplayRecords = dataToRender.iTotalRecords

        notificationService.getAllNotifications().each {
            dataToRender.aaData << [
                notification: it,
                trigger: triggerToMap(it.trigger),
                subject: it.subjectTemplate,
                template: it.template
            ]
        }

        render dataToRender as JSON
    }

    /**
     * Updates the NotifcationType of the Notification identified by the passed in id.
     * @return JSON structure with success: true or error: message
     */
    def updateType(UpdateTypeCommand cmd) {
        if (cmd.hasErrors()) {
            render cmd.errors as JSON
            return
        }
        Notification notification = notificationService.getNotification(cmd.id)
        if (!notification) {
            response.sendError(404)
            return
        }
        def data = [:]
        if (notificationService.updateNotificationType(notification, cmd.type)) {
            data.put("success", true)
        } else {
            data.put("error", g.message(code: "notification.administration.error.save"))
        }
        render data as JSON
    }

    /**
     * Updates the NotificationMedium of the Notification identified by the passed in id.
     * @return JSON structure with success: true or error: message
     */
    def updateMedium(UpdateMediumCommand cmd) {
        if (cmd.hasErrors()) {
            render cmd.errors as JSON
            return
        }
        Notification notification = notificationService.getNotification(cmd.id)
        if (!notification) {
            response.sendError(404)
            return
        }
        def data = [:]
        if (notificationService.updateNotificationMedium(notification, cmd.medium)) {
            data.put("success", true)
        } else {
            data.put("error", g.message(code: "notification.administration.error.save"))
        }
        render data as JSON
    }

    /**
     * Enables/disables the Notification identified by the passed in id.
     * @return JSON structure with success: true or error: message
     */
    def enableNotification(EnableCommand cmd) {
        if (cmd.hasErrors()) {
            render cmd.errors as JSON
            return
        }
        Notification notification = notificationService.getNotification(cmd.id)
        if (!notification) {
            response.sendError(404)
            return
        }
        def data = [:]
        if (notificationService.enableNotification(notification, cmd.enabled)) {
            data.put("success", true)
        } else {
            data.put("error", g.message(code: "notification.administration.error.save"))
        }
        render data as JSON
    }

    /**
     * Updates the text of the NotificationTemplate identified by the passed in id.
     * @return JSON structure with success: true or error: message
     */
    def updateTemplate(UpdateTemplateCommand cmd) {
        if (cmd.hasErrors()) {
            render cmd.errors as JSON
            return
        }
        NotificationTemplate template = notificationService.getNotificationTemplate(cmd.id)
        if (!template) {
            response.sendError(404)
            return
        }
        def data = [:]
        if (notificationService.updateTemplate(template, cmd.text)) {
            data.put("success", true)
        } else {
            data.put("error", g.message(code: "notification.administration.error.save"))
        }
        render data as JSON
    }

    /**
     * Updates the Trigger identified by the passed in id
     * @param cmd
     * @return JSON structure with success: true or error: message
     */
    def updateTrigger(UpdateTriggerCommand cmd) {
        if (cmd.hasErrors()) {
            render cmd.errors as JSON
            return
        }

        Trigger trigger = notificationService.getNotificationTrigger(cmd.id)
        if (!trigger) {
            response.sendError(404)
            return
        }
        def object = null
        if (cmd.jobDefinition) {
            object = jobExecutionPlanService.getJobDefinition(cmd.jobDefinition)
        } else {
            object = jobExecutionPlanService.getPlan(cmd.jobExecutionPlan)
        }
        if (!object) {
            response.sendError(404)
            return
        }
        def data = [:]
        if (notificationService.updateNotificationTrigger(trigger, object)) {
            data.put("success", true)
        } else {
            data.put("error", g.message(code: "notification.administration.error.save"))
        }
        render data as JSON
    }

    /**
     * Retrieves information about the JobDefinition passed in as the id.
     * This includes the id of the job's jobExecutionPlan and the list of all
     * JobDefinitions in this jobExecution plan as element job
     * @return JSON structure with element jobExecutionPlan (id) and element jobs as List of JobDefinitions
     */
    def jobDefinition(IdCommand cmd) {
        if (cmd.hasErrors()) {
            render cmd.errors as JSON
            return
        }
        JobDefinition jobDefinition = jobExecutionPlanService.getJobDefinition(cmd.id)
        if (!jobDefinition) {
            response.sendError(404)
            return
        }
        def data = [
            jobExecutionPlan: jobDefinition.plan.id,
            jobs: jobExecutionPlanService.jobDefinitions(jobDefinition.plan)
        ]
        render data as JSON
    }

    /**
     * Retrieves all jobDefinitions for a JobExecutionPlan passed in as the id.
     * @return JSON list
     */
    def jobDefinitions(IdCommand cmd) {
        if (cmd.hasErrors()) {
            render cmd.errors as JSON
            return
        }
        JobExecutionPlan plan = jobExecutionPlanService.getPlan(cmd.id)
        if (!plan) {
            response.sendError(404)
            return
        }
        def data = [jobs: jobExecutionPlanService.jobDefinitions(plan)]
        render data as JSON
    }

    private Map triggerToMap(Trigger trigger) {
        def object = trigger.toTriggerObject()
        String link = null
        if (trigger.clazz.className == "de.dkfz.tbi.otp.job.plan.JobExecutionPlan") {
            link = g.createLink(controller: "processes", action: "plan", id: trigger.triggerId)
        }
        return [
            id: trigger.id,
            clazz: trigger.clazz.className,
            triggerId: trigger.triggerId,
            value: object.toString(),
            link: link
        ]
    }
}

/**
 * Simple command object for validating that we have an Id.
 */
class IdCommand {
    Long id

    static constraints = {
        id(nullable: false, min: 0L)
    }
}

/**
 * Command Object for updating the NotificationType
 */
class UpdateTypeCommand {
    Long id
    NotificationType type

    static constraints = {
        id(nullable: false, min: 0L)
        type(nullable: false)
    }
}

/**
 * Command Object for updating the NotificationMedium
 */
class UpdateMediumCommand {
    Long id
    NotificationMedium medium

    static constraints = {
        id(nullable: false, min: 0L)
        medium(nullable: false)
    }
}

/**
 * Command Object for enabling/disabling the Notification
 */
class EnableCommand {
    Long id
    Boolean enabled

    static constraints = {
        id(nullable: false, min: 0L)
        enabled(nullable: false)
    }
}

/**
 * Command Object for updating a Notification Template
 */
class UpdateTemplateCommand {
    Long id
    String text

    static constraints = {
        id(nullable: false, min: 0L)
        text(nullable: false, size: 1..10000)
    }
}

/**
 * Command Object for updating a Notification Trigger
 */
class UpdateTriggerCommand {
    Long id
    Long jobExecutionPlan
    Long jobDefinition

    static constraints = {
        id(nullable: false, min: 0L)
        jobExecutionPlan(nullable: false, min: 0L)
        jobDefinition(nullable: true, min: 0L)
    }
}
