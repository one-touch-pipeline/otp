/*
 * Copyright 2011-2026 The OTP authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package de.dkfz.tbi.otp.workflow.notification

import grails.gorm.transactions.Transactional
import groovy.transform.CompileDynamic
import groovy.transform.TupleConstructor
import org.hibernate.LockMode
import org.hibernate.LockOptions
import org.hibernate.Session

import de.dkfz.tbi.otp.administration.MailHelperService
import de.dkfz.tbi.otp.notification.CreateNotification
import de.dkfz.tbi.otp.notification.Notification
import de.dkfz.tbi.otp.notification.NotificationScope
import de.dkfz.tbi.otp.notification.NotificationState
import de.dkfz.tbi.otp.notification.NotificationStatus
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.tracking.Ticket
import de.dkfz.tbi.otp.tracking.TicketService
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.utils.MessageSourceService
import de.dkfz.tbi.otp.utils.StackTraceUtils
import de.dkfz.tbi.otp.workflow.WorkflowCreateState
import de.dkfz.tbi.otp.workflowExecution.Workflow
import de.dkfz.tbi.otp.workflowExecution.WorkflowRun
import de.dkfz.tbi.otp.workflowExecution.WorkflowRunInputArtefact

import java.time.ZonedDateTime

@Transactional
class NotificationService {

    static final Set<WorkflowRun.State> FINISHED_WORKFLOW_RUN_STATES = [
            WorkflowRun.State.SKIPPED_MISSING_PRECONDITION,
            WorkflowRun.State.SUCCESS,
            WorkflowRun.State.FAILED_FINAL,
    ] as Set

    private static final Set<NotificationState> FINAL_NOTIFICATION_STATES = [
            NotificationState.CREATED,
            NotificationState.SKIPPED,
    ] as Set

    private static final int PESSIMISTIC_WRITE_TIME_OUT = 10000

    // Conservative batch size for the IN-list of workflow runs, which can reach five digits for a single import.
    // Not the JDBC limit (65,535, see WorkflowRunService#getCriteria) -- just a safe chunk size, as in DeletionService.
    private static final int WORKFLOW_RUN_QUERY_CHUNK_SIZE = 1000

    // the run list can hold thousands of entries, so it goes into an attachment instead of the mail body
    private static final String WORKFLOW_RUN_ATTACHMENT_NAME = "workflowRuns.txt"

    private static final String QUERY_NEXT_WAITING_CREATE_NOTIFICATION = """
        select cn
        from CreateNotification cn
        where cn.state = :waiting
        and not exists (
            select cn2.id
            from CreateNotification cn2
            where cn2.state = :processing
            and (
                cn2.ticket = cn.ticket
                or exists (
                    select wr2.id
                    from cn2.workflowRuns wr2
                    where wr2.project in (
                        select wr.project
                        from cn.workflowRuns wr
                    )
                )
            )
        )
        order by cn.id asc
        """

    private static final String QUERY_NOTIFICATIONS_REFERENCING_RESTARTED_WORKFLOW_RUN = """
        select distinct n
        from Notification n
        join n.workflowRuns wr
        where wr.state = :restarted
        """

    private static final String QUERY_READY_CHECKING_NOTIFICATIONS = """
        select n
        from Notification n
        where n.notificationState = :checking
        and not exists (
            select wr.id
            from n.workflowRuns wr
            where wr.state not in (:finishedStates)
        )
        and not exists (
            select dn.id
            from n.dependingNotification dn
            where dn.notificationState not in (:finalStates)
        )
        """

    private static final String QUERY_NOTIFICATION_STATUSES_NEEDING_MAIL = """
        select distinct ns
        from NotificationStatus ns
        left join fetch ns.notifications
        where ns.finalSend = false
        and (
            select count(finishedNotification.id)
            from NotificationStatus statusToCount
            join statusToCount.notifications finishedNotification
            where statusToCount = ns
            and finishedNotification.notificationState in (:finalStates)
        ) <> ns.finishedWorkflowCount
        """

    // projects the workflow pairs directly, so neither the input artefacts nor the producing runs have to be loaded;
    // the distinct collapses the many runs of the same workflow pair into one row
    private static final String QUERY_WORKFLOW_DEPENDENCIES = """
        select distinct consumingRun.workflow, producingRun.workflow
        from WorkflowRunInputArtefact inputArtefact
        join inputArtefact.workflowRun consumingRun
        join inputArtefact.workflowArtefact artefact
        join artefact.producedBy producingRun
        where consumingRun in (:workflowRuns)
        and consumingRun.workflow <> producingRun.workflow
        """

    private static final String QUERY_OLD_UNREFERENCED_NOTIFICATIONS = """
        select n
        from Notification n
        where n.notificationState in (:finalStates)
        and n.lastUpdated < :cutoff
        and not exists (
            select ns.id
            from NotificationStatus ns
            join ns.notifications sn
            where sn = n
        )
        and not exists (
            select n2.id
            from Notification n2
            join n2.dependingNotification dn
            where dn = n
        )
        """

    MailHelperService mailHelperService
    MessageSourceService messageSourceService
    TicketService ticketService

    // excludes tickets/projects already PROCESSING, since createNotifications looks up and reuses existing rows per project/ticket/workflow
    CreateNotification nextWaitingCreateNotification() {
        return CollectionUtils.atMostOneElement(CreateNotification.executeQuery(
                QUERY_NEXT_WAITING_CREATE_NOTIFICATION,
                [
                        waiting   : WorkflowCreateState.WAITING,
                        processing: WorkflowCreateState.PROCESSING,
                ],
                [max: 1],
        ) as List<CreateNotification>)
    }

    @CompileDynamic
    int countProcessingCreateNotifications() {
        return CreateNotification.countByState(WorkflowCreateState.PROCESSING)
    }

    void updateCreateNotificationState(long createNotificationId, WorkflowCreateState state) {
        CreateNotification createNotification = CreateNotification.get(createNotificationId)
        createNotification.state = state
        createNotification.save(flush: true)
    }

    // recovers entries stuck PROCESSING by a crash/shutdown; only meant to be called during startup
    @CompileDynamic
    void changeProcessToWait() {
        CreateNotification.findAllByState(WorkflowCreateState.PROCESSING).each { CreateNotification createNotification ->
            log.info("Change notification creation for ticket ${createNotification.ticket.ticketNumber} from " +
                    "${WorkflowCreateState.PROCESSING} back to ${WorkflowCreateState.WAITING}")
            createNotification.state = WorkflowCreateState.WAITING
            createNotification.save(flush: true)
        }
    }

    void createNotifications(long createNotificationId) {
        CreateNotification createNotification = getCreateNotification(createNotificationId)
        Ticket ticket = createNotification.ticket
        Set<WorkflowRun> workflowRuns = createNotification.workflowRuns

        List<Notification> createdOrAdaptedNotifications = []
        boolean anythingChanged = false
        Map<Project, List<WorkflowRun>> workflowRunsByProject = groupByProject(workflowRuns)
        workflowRunsByProject.each { Project project, List<WorkflowRun> projectWorkflowRuns ->
            ProjectNotifications projectNotifications = createNotificationsForProject(ticket, project, projectWorkflowRuns)
            createdOrAdaptedNotifications.addAll(projectNotifications.notifications)
            anythingChanged = projectNotifications.changed || anythingChanged
        }

        createOrUpdateNotificationStatus(ticket, createdOrAdaptedNotifications, anythingChanged)

        createNotification.state = WorkflowCreateState.SUCCESS
        createNotification.save(flush: true)

        sendNotificationCreateSuccessMail(ticket, workflowRuns)
    }

    void sendNotificationCreateErrorMail(long createNotificationId, Throwable throwable) {
        CreateNotification createNotification = getCreateNotification(createNotificationId)
        Ticket ticket = createNotification.ticket

        String subject = messageSourceService.createMessage('notification.create.error.subject', [
                ticketNumber: ticketService.getPrefixedTicketNumber(ticket),
        ])
        String body = messageSourceService.createMessage('notification.create.error.body', [
                ticketNumber: ticket.ticketNumber,
                stackTrace  : StackTraceUtils.getStackTrace(throwable),
                retryCommand: "ctx.notificationService.updateCreateNotificationState(${createNotificationId}L, " +
                        "de.dkfz.tbi.otp.workflow.WorkflowCreateState.WAITING)",
        ])
        mailHelperService.saveErrorMailInNewTransaction(subject, body)
    }

    // a RESTARTED run itself will never finish, so notifications must switch to whatever run replaced it
    void updateNotificationsReferencingRestartedWorkflowRun() {
        notificationsReferencingRestartedWorkflowRun().each { Notification notification ->
            replaceRestartedWorkflowRuns(notification)
        }
    }

    void markCheckingNotificationsReady() {
        List<Notification> notifications = Notification.executeQuery(
                QUERY_READY_CHECKING_NOTIFICATIONS,
                [
                        checking      : NotificationState.CHECKING,
                        finishedStates: FINISHED_WORKFLOW_RUN_STATES,
                        finalStates   : FINAL_NOTIFICATION_STATES,
                ],
        ) as List<Notification>
        notifications.each { Notification notification ->
            notification.notificationState = NotificationState.READY
            notification.save(flush: true)
        }
    }

    @CompileDynamic
    Notification nextReadyNotification() {
        return CollectionUtils.atMostOneElement(Notification.findAllByNotificationState(
                NotificationState.READY, [sort: 'id', order: 'asc', max: 1],
        ) as List<Notification>)
    }

    List<NotificationStatus> notificationStatusesNeedingMail() {
        return NotificationStatus.executeQuery(
                QUERY_NOTIFICATION_STATUSES_NEEDING_MAIL,
                [finalStates: FINAL_NOTIFICATION_STATES],
        ) as List<NotificationStatus>
    }

    static int finishedNotificationCount(NotificationStatus notificationStatus) {
        Set<Notification> notifications = notificationStatus.notifications ?: [] as Set<Notification>
        return notifications.count { Notification notification -> notification.notificationState in FINAL_NOTIFICATION_STATES } as int
    }

    void deleteOldNotifications(int delayInDays) {
        Date cutoff = Date.from(ZonedDateTime.now().minusDays(delayInDays).toInstant())
        deleteOldFinalNotificationStatuses(cutoff)
        deleteOldUnreferencedNotifications(cutoff)
    }

    @CompileDynamic
    private void deleteOldFinalNotificationStatuses(Date cutoff) {
        NotificationStatus.findAllByFinalSendAndLastUpdatedLessThan(true, cutoff).each { NotificationStatus notificationStatus ->
            log.info("Deleting old notification status ${notificationStatus.id} of ticket ${notificationStatus.ticket.ticketNumber}")
            notificationStatus.delete(flush: true)
        }
    }

    private void deleteOldUnreferencedNotifications(Date cutoff) {
        List<Notification> notifications = Notification.executeQuery(
                QUERY_OLD_UNREFERENCED_NOTIFICATIONS,
                [
                        finalStates: FINAL_NOTIFICATION_STATES,
                        cutoff     : cutoff,
                ],
        ) as List<Notification>
        notifications.each { Notification notification ->
            log.info("Deleting old notification ${notification.id}")
            notification.delete(flush: true)
        }
    }

    private List<Notification> notificationsReferencingRestartedWorkflowRun() {
        return Notification.executeQuery(
                QUERY_NOTIFICATIONS_REFERENCING_RESTARTED_WORKFLOW_RUN,
                [restarted: WorkflowRun.State.RESTARTED],
        ) as List<Notification>
    }

    @CompileDynamic
    private void replaceRestartedWorkflowRuns(Notification notification) {
        Collection<WorkflowRun> restartedRuns = notification.workflowRuns.findAll { it.state == WorkflowRun.State.RESTARTED }
        restartedRuns.each { WorkflowRun restartedRun ->
            WorkflowRun replacement = CollectionUtils.atMostOneElement(WorkflowRun.findAllByRestartedFrom(restartedRun))
            if (replacement) {
                notification.removeFromWorkflowRuns(restartedRun)
                notification.addToWorkflowRuns(replacement)
            }
        }
        notification.save(flush: true)
    }

    private static Map<Project, List<WorkflowRun>> groupByProject(Collection<WorkflowRun> workflowRuns) {
        return workflowRuns.groupBy { WorkflowRun workflowRun -> workflowRun.project } as Map<Project, List<WorkflowRun>>
    }

    private static Map<Workflow, List<WorkflowRun>> groupByWorkflow(Collection<WorkflowRun> workflowRuns) {
        return workflowRuns.groupBy { WorkflowRun workflowRun -> workflowRun.workflow } as Map<Workflow, List<WorkflowRun>>
    }

    private ProjectNotifications createNotificationsForProject(Ticket ticket, Project project, List<WorkflowRun> projectWorkflowRuns) {
        Map<Workflow, List<WorkflowRun>> workflowRunsByWorkflow = groupByWorkflow(projectWorkflowRuns)
        Map<Workflow, Set<Workflow>> producingWorkflowsByWorkflow = collectProducingWorkflows(projectWorkflowRuns)

        boolean changed = false
        Map<Workflow, Notification> notificationByWorkflow = [:]
        workflowRunsByWorkflow.each { Workflow workflow, List<WorkflowRun> runs ->
            Notification notification = findOrCreateCheckingNotification(ticket, project, workflow)
            changed = addWorkflowRuns(notification, runs) || changed
            notification.save(flush: true)
            notificationByWorkflow[workflow] = notification
        }

        notificationByWorkflow.each { Workflow workflow, Notification notification ->
            Set<Workflow> producingWorkflows = producingWorkflowsByWorkflow[workflow] ?: [] as Set<Workflow>
            producingWorkflows.each { Workflow producingWorkflow ->
                Notification producingNotification = notificationByWorkflow[producingWorkflow]
                if (producingNotification && producingNotification != notification) {
                    notification.addToDependingNotification(producingNotification)
                }
            }
            notification.save(flush: true)
        }

        Notification allWorkflowsNotification = findOrCreateCheckingNotification(ticket, project, null)
        projectWorkflowRuns.each { allWorkflowsNotification.addToWorkflowRuns(it) }
        // NotificationHandlerService builds the summary body solely from dependingNotification, and the summary must go out
        // only once every per-workflow notification of this project reached a final state
        notificationByWorkflow.values().each { allWorkflowsNotification.addToDependingNotification(it) }
        allWorkflowsNotification.save(flush: true)

        return new ProjectNotifications(notificationByWorkflow.values(), changed)
    }

    private static boolean addWorkflowRuns(Notification notification, Collection<WorkflowRun> runs) {
        int sizeBefore = notification.workflowRuns?.size() ?: 0
        runs.each { notification.addToWorkflowRuns(it) }
        return (notification.workflowRuns?.size() ?: 0) > sizeBefore
    }

    /**
     * What one project's batch touched: the per-workflow {@link Notification}s the ticket's {@link NotificationStatus}
     * has to report on, and whether any of them was created or actually gained a workflow run.
     */
    @TupleConstructor
    private static class ProjectNotifications {
        Collection<Notification> notifications
        boolean changed
    }

    // there is no "workflow X depends on workflow Y" domain concept in OTP; dependencies only show up as artefact-graph edges between actual runs
    private static Map<Workflow, Set<Workflow>> collectProducingWorkflows(List<WorkflowRun> workflowRuns) {
        Map<Workflow, Set<Workflow>> producingWorkflowsByWorkflow = [:]
        workflowRuns.collate(WORKFLOW_RUN_QUERY_CHUNK_SIZE).each { List<WorkflowRun> chunk ->
            List<Object[]> workflowPairs = WorkflowRunInputArtefact.executeQuery(
                    QUERY_WORKFLOW_DEPENDENCIES,
                    [workflowRuns: chunk],
            ) as List<Object[]>
            for (Object[] workflowPair : workflowPairs) {
                Workflow consumingWorkflow = workflowPair[0] as Workflow
                Workflow producingWorkflow = workflowPair[1] as Workflow
                producingWorkflowsByWorkflow.computeIfAbsent(consumingWorkflow) { [] as Set<Workflow> }.add(producingWorkflow)
            }
        }
        return producingWorkflowsByWorkflow
    }

    @CompileDynamic
    private Notification findOrCreateCheckingNotification(Ticket ticket, Project project, Workflow workflow) {
        Notification notification = workflow ?
                CollectionUtils.atMostOneElement(Notification.findAllByNotificationStateAndNotificationScopeAndNotificationScopeIdAndProjectAndWorkflow(
                        NotificationState.CHECKING, NotificationScope.TICKET, ticket.id, project, workflow)) :
                CollectionUtils.atMostOneElement(Notification.findAllByNotificationStateAndNotificationScopeAndNotificationScopeIdAndProjectAndWorkflowIsNull(
                        NotificationState.CHECKING, NotificationScope.TICKET, ticket.id, project))

        if (notification) {
            lockNotification(notification)
            return notification
        }

        return new Notification(
                notificationState: NotificationState.CHECKING,
                notificationScope: NotificationScope.TICKET,
                notificationScopeId: ticket.id,
                workflow: workflow,
                project: project,
        ).save(flush: true)
    }

    // only the notifications of this batch are attached: re-attaching earlier ones would make the status mail report
    // runs a previous status already covered
    private void createOrUpdateNotificationStatus(Ticket ticket, Collection<Notification> workflowNotifications, boolean anythingChanged) {
        NotificationStatus notificationStatus = findNotificationStatusInProgress(ticket)
        if (notificationStatus) {
            lockNotificationStatus(notificationStatus)
            workflowNotifications.each { notificationStatus.addToNotifications(it) }
            if (anythingChanged) {
                notificationStatus.finishedWorkflowCount = 0
            }
            notificationStatus.save(flush: true)
            return
        }

        notificationStatus = new NotificationStatus(
                ticket: ticket,
                finishedWorkflowCount: 0,
                finalSend: false,
        )
        workflowNotifications.each { notificationStatus.addToNotifications(it) }
        notificationStatus.save(flush: true)
    }

    @CompileDynamic
    private static NotificationStatus findNotificationStatusInProgress(Ticket ticket) {
        return CollectionUtils.atMostOneElement(NotificationStatus.findAllByTicketAndFinalSend(ticket, false))
    }

    private static CreateNotification getCreateNotification(long createNotificationId) {
        CreateNotification createNotification = CreateNotification.get(createNotificationId)
        assert createNotification
        return createNotification
    }

    private void sendNotificationCreateSuccessMail(Ticket ticket, Collection<WorkflowRun> workflowRuns) {
        String subject = messageSourceService.createMessage('notification.create.success.subject', [
                ticketNumber: ticketService.getPrefixedTicketNumber(ticket),
        ])
        String workflowRunHint = workflowRuns ?
                messageSourceService.createMessage('notification.create.success.attachmentHint', [
                        attachmentName: WORKFLOW_RUN_ATTACHMENT_NAME,
                ]) :
                messageSourceService.createMessage('notification.create.success.noWorkflowRuns')
        String body = messageSourceService.createMessage('notification.create.success.body', [
                ticketNumber    : ticket.ticketNumber,
                workflowRunCount: workflowRuns.size(),
                workflowRunHint : workflowRunHint,
        ])

        // Attachment.content must not be blank, so an empty run list gets no attachment at all
        Map<String, String> attachments = workflowRuns ?
                [(WORKFLOW_RUN_ATTACHMENT_NAME): workflowRunAttachmentContent(workflowRuns)] :
                [:]
        mailHelperService.saveMail(subject, body, [], [], [], attachments)
    }

    private static String workflowRunAttachmentContent(Collection<WorkflowRun> workflowRuns) {
        return workflowRuns.sort { WorkflowRun workflowRun -> workflowRun.id }*.toString().join('\n')
    }

    private static void lockNotification(Notification notification) {
        Notification.withSession { Session session ->
            session.refresh(notification, new LockOptions(LockMode.PESSIMISTIC_WRITE).setTimeOut(PESSIMISTIC_WRITE_TIME_OUT))
        }
    }

    private static void lockNotificationStatus(NotificationStatus notificationStatus) {
        NotificationStatus.withSession { Session session ->
            session.refresh(notificationStatus, new LockOptions(LockMode.PESSIMISTIC_WRITE).setTimeOut(PESSIMISTIC_WRITE_TIME_OUT))
        }
    }
}
