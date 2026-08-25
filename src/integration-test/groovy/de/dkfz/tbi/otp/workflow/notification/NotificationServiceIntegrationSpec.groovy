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

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import org.hibernate.Hibernate
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.administration.Mail
import de.dkfz.tbi.otp.administration.MailHelperService
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.domainFactory.notification.NotificationDomainFactory
import de.dkfz.tbi.otp.notification.CreateNotification
import de.dkfz.tbi.otp.notification.Notification
import de.dkfz.tbi.otp.notification.NotificationScope
import de.dkfz.tbi.otp.notification.NotificationState
import de.dkfz.tbi.otp.notification.NotificationStatus
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.tracking.Ticket
import de.dkfz.tbi.otp.utils.Entity
import de.dkfz.tbi.otp.workflow.WorkflowCreateState
import de.dkfz.tbi.otp.workflowExecution.Workflow
import de.dkfz.tbi.otp.workflowExecution.WorkflowArtefact
import de.dkfz.tbi.otp.workflowExecution.WorkflowRun

import java.time.ZonedDateTime

@Rollback
@Integration
class NotificationServiceIntegrationSpec extends Specification implements NotificationDomainFactory {

    NotificationService notificationService

    private void setUpMailOptions() {
        findOrCreateProcessingOption(ProcessingOption.OptionName.TICKET_SYSTEM_NUMBER_PREFIX, 'OTP')
        findOrCreateProcessingOption(ProcessingOption.OptionName.EMAIL_TICKET_SYSTEM, 'ticket@example.com')
    }

    void "nextWaitingCreateNotification skips entries that share a ticket or a project with a processing one"() {
        given:
        Project sharedProject = createProject()

        CreateNotification blockedByTicket = createCreateNotification()
        blockedByTicket.addToWorkflowRuns(createWorkflowRun())
        blockedByTicket.save(flush: true)
        CreateNotification blockingByTicket = createCreateNotification(ticket: blockedByTicket.ticket, state: WorkflowCreateState.PROCESSING)
        blockingByTicket.addToWorkflowRuns(createWorkflowRun())
        blockingByTicket.save(flush: true)

        CreateNotification blockedByProject = createCreateNotification()
        blockedByProject.addToWorkflowRuns(createWorkflowRun(project: sharedProject))
        blockedByProject.save(flush: true)
        CreateNotification blockingByProject = createCreateNotification(state: WorkflowCreateState.PROCESSING)
        blockingByProject.addToWorkflowRuns(createWorkflowRun(project: sharedProject))
        blockingByProject.save(flush: true)

        when: 'only blocked entries exist'
        CreateNotification blockedResult = notificationService.nextWaitingCreateNotification()

        then: 'nothing is handed out, instead of falling back to a blocked entry'
        blockedResult == null

        when: 'an entry sharing neither ticket nor project is added'
        CreateNotification free = createCreateNotification()
        free.addToWorkflowRuns(createWorkflowRun())
        free.save(flush: true)
        CreateNotification freeResult = notificationService.nextWaitingCreateNotification()

        then: 'it is picked, even though the blocked entries have lower ids'
        freeResult.id == free.id
    }

    void "nextWaitingCreateNotification skips an entry overlapping a processing one in only one of its projects"() {
        given:
        Project sharedProject = createProject()
        Project separateProject = createProject()

        // spans two projects, only one of which the processing entry touches: one shared project is enough to block
        CreateNotification partlyOverlapping = createCreateNotification()
        partlyOverlapping.addToWorkflowRuns(createWorkflowRun(project: separateProject))
        partlyOverlapping.addToWorkflowRuns(createWorkflowRun(project: sharedProject))
        partlyOverlapping.save(flush: true)

        CreateNotification blocking = createCreateNotification(state: WorkflowCreateState.PROCESSING)
        blocking.addToWorkflowRuns(createWorkflowRun(project: sharedProject))
        blocking.save(flush: true)

        expect:
        notificationService.nextWaitingCreateNotification() == null
    }

    void "countProcessingCreateNotifications counts only the entries in state PROCESSING"() {
        given:
        createCreateNotification(state: WorkflowCreateState.PROCESSING)
        createCreateNotification(state: WorkflowCreateState.PROCESSING)
        createCreateNotification(state: WorkflowCreateState.WAITING)
        createCreateNotification(state: WorkflowCreateState.SUCCESS)
        createCreateNotification(state: WorkflowCreateState.FAILED)

        expect:
        notificationService.countProcessingCreateNotifications() == 2
    }

    void "updateCreateNotificationState writes the given state"() {
        given:
        CreateNotification createNotification = createCreateNotification(state: WorkflowCreateState.WAITING)

        when:
        notificationService.updateCreateNotificationState(createNotification.id, WorkflowCreateState.PROCESSING)

        then:
        createNotification.refresh().state == WorkflowCreateState.PROCESSING
    }

    void "createNotifications builds per-workflow, dependent and all-workflow notifications plus the ticket status"() {
        given:
        setUpMailOptions()
        Project project = createProject()
        Ticket ticket = createTicket()

        Workflow upstreamWorkflow = createWorkflow(name: "upstream_${nextId}")
        WorkflowRun upstreamRun = createWorkflowRun(workflow: upstreamWorkflow, project: project)
        WorkflowArtefact producedArtefact = createWorkflowArtefact(producedBy: upstreamRun)

        Workflow downstreamWorkflow = createWorkflow(name: "downstream_${nextId}")
        WorkflowRun downstreamRun = createWorkflowRun(workflow: downstreamWorkflow, project: project)
        createWorkflowRunInputArtefact(workflowRun: downstreamRun, workflowArtefact: producedArtefact)

        CreateNotification createNotification = createCreateNotification(ticket: ticket)
        createNotification.addToWorkflowRuns(upstreamRun)
        createNotification.addToWorkflowRuns(downstreamRun)
        createNotification.save(flush: true)
        assert Mail.count() == 0

        when:
        notificationService.createNotifications(createNotification.id)

        then:
        Notification upstreamNotification = exactlyOneNotificationFor(ticket, project, upstreamWorkflow)
        upstreamNotification.notificationState == NotificationState.CHECKING
        upstreamNotification.workflowRuns == [upstreamRun] as Set

        Notification downstreamNotification = exactlyOneNotificationFor(ticket, project, downstreamWorkflow)
        downstreamNotification.notificationState == NotificationState.CHECKING
        downstreamNotification.workflowRuns == [downstreamRun] as Set
        downstreamNotification.dependingNotification == [upstreamNotification] as Set

        Notification allWorkflowsNotification = exactlyOneNotificationFor(ticket, project, null)
        allWorkflowsNotification.workflowRuns == [upstreamRun, downstreamRun] as Set
        // the summary mail body is rendered from these, and it must not be sent before the per-workflow notifications
        allWorkflowsNotification.dependingNotification == [upstreamNotification, downstreamNotification] as Set

        NotificationStatus notificationStatus = NotificationStatus.findByTicket(ticket)
        notificationStatus.finalSend == false
        notificationStatus.finishedWorkflowCount == 0
        notificationStatus.notifications == [upstreamNotification, downstreamNotification] as Set

        createNotification.refresh().state == WorkflowCreateState.SUCCESS

        Mail.count() == 1
        List<Mail> successMails = Mail.findAllBySubjectLike("%Notification objects created successfully%")
        successMails.size() == 1

        Mail successMail = successMails.first()
        successMail.body.contains("Workflow runs: 2")
        !successMail.body.contains(upstreamRun.toString())
        successMail.attachments*.name == ['workflowRuns.txt']
        successMail.attachments.first().content.split('\n') as List == [upstreamRun, downstreamRun]
                .sort { WorkflowRun workflowRun -> workflowRun.id }*.toString()
    }

    void "createNotifications builds an independent notification graph per project"() {
        given:
        setUpMailOptions()
        Ticket ticket = createTicket()
        Project projectOne = createProject()
        Project projectTwo = createProject()
        Workflow workflow = createWorkflow(name: "shared_${nextId}")

        WorkflowRun runOne = createWorkflowRun(workflow: workflow, project: projectOne)
        WorkflowRun runTwo = createWorkflowRun(workflow: workflow, project: projectTwo)

        CreateNotification createNotification = createCreateNotification(ticket: ticket)
        createNotification.addToWorkflowRuns(runOne)
        createNotification.addToWorkflowRuns(runTwo)
        createNotification.save(flush: true)

        when:
        notificationService.createNotifications(createNotification.id)

        then: 'the same workflow in two projects gets one notification per project, each with only that project runs'
        Notification notificationOne = exactlyOneNotificationFor(ticket, projectOne, workflow)
        Notification notificationTwo = exactlyOneNotificationFor(ticket, projectTwo, workflow)
        notificationOne.workflowRuns == [runOne] as Set
        notificationTwo.workflowRuns == [runTwo] as Set

        and: 'each project gets its own all-workflows notification'
        exactlyOneNotificationFor(ticket, projectOne, null).workflowRuns == [runOne] as Set
        exactlyOneNotificationFor(ticket, projectTwo, null).workflowRuns == [runTwo] as Set
        Notification.count() == 4

        and: 'the single ticket status spans the per-workflow notifications of both projects'
        NotificationStatus.count() == 1
        NotificationStatus.findByTicket(ticket).notifications == [notificationOne, notificationTwo] as Set
    }

    void "createNotifications maps a multi level workflow dependency graph"() {
        given:
        setUpMailOptions()
        Ticket ticket = createTicket()
        Project project = createProject()

        Workflow workflowA = createWorkflow(name: "a_${nextId}")
        Workflow workflowB = createWorkflow(name: "b_${nextId}")
        Workflow workflowC = createWorkflow(name: "c_${nextId}")
        Workflow workflowD = createWorkflow(name: "d_${nextId}")
        Workflow workflowIndependent = createWorkflow(name: "independent_${nextId}")

        WorkflowRun runA = createWorkflowRun(workflow: workflowA, project: project)
        WorkflowRun runB = createWorkflowRun(workflow: workflowB, project: project)
        WorkflowRun runC = createWorkflowRun(workflow: workflowC, project: project)
        WorkflowRun runD = createWorkflowRun(workflow: workflowD, project: project)
        WorkflowRun runIndependent = createWorkflowRun(workflow: workflowIndependent, project: project)

        // B <- A, C <- A and B, D <- C, and one workflow without any edge
        dependOn(runB, runA)
        dependOn(runC, runA)
        dependOn(runC, runB)
        dependOn(runD, runC)

        CreateNotification createNotification = createCreateNotification(ticket: ticket)
        [runA, runB, runC, runD, runIndependent].each { createNotification.addToWorkflowRuns(it) }
        createNotification.save(flush: true)

        when:
        notificationService.createNotifications(createNotification.id)

        then:
        Notification notificationA = exactlyOneNotificationFor(ticket, project, workflowA)
        Notification notificationB = exactlyOneNotificationFor(ticket, project, workflowB)
        Notification notificationC = exactlyOneNotificationFor(ticket, project, workflowC)
        Notification notificationD = exactlyOneNotificationFor(ticket, project, workflowD)
        Notification notificationIndependent = exactlyOneNotificationFor(ticket, project, workflowIndependent)

        !notificationA.dependingNotification
        notificationB.dependingNotification == [notificationA] as Set
        notificationC.dependingNotification == [notificationA, notificationB] as Set
        notificationD.dependingNotification == [notificationC] as Set
        !notificationIndependent.dependingNotification

        and: 'the summary waits for every workflow of the project'
        exactlyOneNotificationFor(ticket, project, null).dependingNotification ==
                [notificationA, notificationB, notificationC, notificationD, notificationIndependent] as Set
    }

    void "createNotifications reuses the in progress NotificationStatus and adds the new notification to it"() {
        given:
        setUpMailOptions()
        Ticket ticket = createTicket()
        Project project = createProject()

        Workflow earlierWorkflow = createWorkflow(name: "earlier_${nextId}")
        Notification earlierNotification = new Notification(
                notificationState: NotificationState.CHECKING,
                notificationScope: NotificationScope.TICKET,
                notificationScopeId: ticket.id,
                workflow: earlierWorkflow,
                project: project,
        ).addToWorkflowRuns(createWorkflowRun(workflow: earlierWorkflow, project: project)).save(flush: true)
        NotificationStatus existingStatus = new NotificationStatus(
                ticket: ticket,
                finishedWorkflowCount: 2,
                finalSend: false,
        ).addToNotifications(earlierNotification).save(flush: true)

        Workflow laterWorkflow = createWorkflow(name: "later_${nextId}")
        WorkflowRun laterRun = createWorkflowRun(workflow: laterWorkflow, project: project)
        CreateNotification createNotification = createCreateNotification(ticket: ticket)
        createNotification.addToWorkflowRuns(laterRun)
        createNotification.save(flush: true)

        when:
        notificationService.createNotifications(createNotification.id)

        then: 'no second status is opened for the ticket'
        NotificationStatus.count() == 1

        and: 'the status keeps what it already tracked and gains the new notification'
        Notification laterNotification = exactlyOneNotificationFor(ticket, project, laterWorkflow)
        existingStatus.refresh().notifications == [earlierNotification, laterNotification] as Set
        existingStatus.finishedWorkflowCount == 0
    }

    void "createNotifications reuses the notification of one workflow while creating one for another"() {
        given:
        setUpMailOptions()
        Ticket ticket = createTicket()
        Project project = createProject()

        Workflow reusedWorkflow = createWorkflow(name: "reused_${nextId}")
        WorkflowRun firstRun = createWorkflowRun(workflow: reusedWorkflow, project: project)
        Notification reusableNotification = new Notification(
                notificationState: NotificationState.CHECKING,
                notificationScope: NotificationScope.TICKET,
                notificationScopeId: ticket.id,
                workflow: reusedWorkflow,
                project: project,
        ).addToWorkflowRuns(firstRun).save(flush: true)

        WorkflowRun secondRun = createWorkflowRun(workflow: reusedWorkflow, project: project)
        Workflow freshWorkflow = createWorkflow(name: "fresh_${nextId}")
        WorkflowRun freshRun = createWorkflowRun(workflow: freshWorkflow, project: project)

        CreateNotification createNotification = createCreateNotification(ticket: ticket)
        createNotification.addToWorkflowRuns(secondRun)
        createNotification.addToWorkflowRuns(freshRun)
        createNotification.save(flush: true)

        when:
        notificationService.createNotifications(createNotification.id)

        then: 'the CHECKING notification is extended instead of duplicated'
        Notification reused = exactlyOneNotificationFor(ticket, project, reusedWorkflow)
        reused.id == reusableNotification.id
        reused.workflowRuns == [firstRun, secondRun] as Set

        and: 'the workflow without one gets a new notification'
        Notification fresh = exactlyOneNotificationFor(ticket, project, freshWorkflow)
        fresh.id != reusableNotification.id
        fresh.workflowRuns == [freshRun] as Set

        and: 'reused, fresh and the all-workflows notification of the project'
        Notification.count() == 3
    }

    void "createNotifications does not make a workflow depend on itself when a run consumes another run of the same workflow"() {
        given:
        setUpMailOptions()
        Project project = createProject()
        Ticket ticket = createTicket()

        Workflow workflow = createWorkflow(name: "self_${nextId}")
        WorkflowRun producingRun = createWorkflowRun(workflow: workflow, project: project)
        WorkflowArtefact producedArtefact = createWorkflowArtefact(producedBy: producingRun)
        WorkflowRun consumingRun = createWorkflowRun(workflow: workflow, project: project)
        createWorkflowRunInputArtefact(workflowRun: consumingRun, workflowArtefact: producedArtefact)

        CreateNotification createNotification = createCreateNotification(ticket: ticket)
        createNotification.addToWorkflowRuns(producingRun)
        createNotification.addToWorkflowRuns(consumingRun)
        createNotification.save(flush: true)

        when:
        notificationService.createNotifications(createNotification.id)

        then:
        // a self-link would strand the notification in CHECKING forever, waiting on its own mail; enforced by the
        // producingNotification != notification check in createNotificationsForProject, with the query filtering it too
        Notification notification = exactlyOneNotificationFor(ticket, project, workflow)
        !notification.dependingNotification
    }

    void "createNotifications links a dependency whose producing run is not itself part of the batch"() {
        given:
        setUpMailOptions()
        Project project = createProject()
        Ticket ticket = createTicket()

        // the producing run stays out of the CreateNotification, so the edge can only come from the artefact graph
        Workflow upstreamWorkflow = createWorkflow(name: "upstream_${nextId}")
        WorkflowRun upstreamRun = createWorkflowRun(workflow: upstreamWorkflow, project: project)
        WorkflowArtefact producedArtefact = createWorkflowArtefact(producedBy: upstreamRun)

        Workflow downstreamWorkflow = createWorkflow(name: "downstream_${nextId}")
        WorkflowRun downstreamRun = createWorkflowRun(workflow: downstreamWorkflow, project: project)
        createWorkflowRunInputArtefact(workflowRun: downstreamRun, workflowArtefact: producedArtefact)

        CreateNotification createNotification = createCreateNotification(ticket: ticket)
        createNotification.addToWorkflowRuns(downstreamRun)
        createNotification.save(flush: true)

        when:
        notificationService.createNotifications(createNotification.id)

        then:
        // no notification exists for the upstream workflow in this batch, so there is nothing to link to, but the query must not fail either
        Notification downstreamNotification = exactlyOneNotificationFor(ticket, project, downstreamWorkflow)
        downstreamNotification.workflowRuns == [downstreamRun] as Set
        !downstreamNotification.dependingNotification
    }

    void "createNotifications attaches only this batch's notifications to the ticket status"() {
        given:
        setUpMailOptions()
        Project project = createProject()
        Ticket ticket = createTicket()

        // same ticket, but from an earlier batch and already mailed, so the new status must not report on it again
        Workflow earlierWorkflow = createWorkflow(name: "earlier_${nextId}")
        Notification alreadyMailed = new Notification(
                notificationState: NotificationState.CREATED,
                notificationScope: NotificationScope.TICKET,
                notificationScopeId: ticket.id,
                workflow: earlierWorkflow,
                project: project,
        ).addToWorkflowRuns(createWorkflowRun(workflow: earlierWorkflow, project: project)).save(flush: true)

        Workflow currentWorkflow = createWorkflow(name: "current_${nextId}")
        WorkflowRun currentRun = createWorkflowRun(workflow: currentWorkflow, project: project)
        CreateNotification createNotification = createCreateNotification(ticket: ticket)
        createNotification.addToWorkflowRuns(currentRun)
        createNotification.save(flush: true)

        when:
        notificationService.createNotifications(createNotification.id)

        then:
        Notification currentNotification = exactlyOneNotificationFor(ticket, project, currentWorkflow)
        NotificationStatus notificationStatus = NotificationStatus.findByTicket(ticket)
        notificationStatus.notifications == [currentNotification] as Set
        !notificationStatus.notifications.contains(alreadyMailed)
    }

    void "createNotifications reuses an existing CHECKING notification and resets the ticket status count"() {
        given:
        setUpMailOptions()
        Project project = createProject()
        Ticket ticket = createTicket()
        Workflow workflow = createWorkflow()

        WorkflowRun firstRun = createWorkflowRun(workflow: workflow, project: project)
        Notification existingNotification = new Notification(
                notificationState: NotificationState.CHECKING,
                notificationScope: NotificationScope.TICKET,
                notificationScopeId: ticket.id,
                workflow: workflow,
                project: project,
        ).addToWorkflowRuns(firstRun).save(flush: true)
        NotificationStatus existingStatus = new NotificationStatus(
                ticket: ticket,
                finishedWorkflowCount: 3,
                finalSend: false,
        ).addToNotifications(existingNotification).save(flush: true)

        WorkflowRun secondRun = createWorkflowRun(workflow: workflow, project: project)
        CreateNotification createNotification = createCreateNotification(ticket: ticket)
        createNotification.addToWorkflowRuns(secondRun)
        createNotification.save(flush: true)

        when:
        notificationService.createNotifications(createNotification.id)

        then:
        Notification.count() == 2 // the reused per-workflow notification + the new all-workflows one
        NotificationStatus.count() == 1 // the existing status was reused, not duplicated
        existingNotification.refresh().workflowRuns == [firstRun, secondRun] as Set
        existingStatus.refresh().finishedWorkflowCount == 0
    }

    void "createNotifications keeps the ticket status count when the batch adds no new workflow run"() {
        given:
        setUpMailOptions()
        Project project = createProject()
        Ticket ticket = createTicket()
        Workflow workflow = createWorkflow()

        WorkflowRun alreadyCoveredRun = createWorkflowRun(workflow: workflow, project: project)
        Notification existingNotification = new Notification(
                notificationState: NotificationState.CHECKING,
                notificationScope: NotificationScope.TICKET,
                notificationScopeId: ticket.id,
                workflow: workflow,
                project: project,
        ).addToWorkflowRuns(alreadyCoveredRun).save(flush: true)
        NotificationStatus existingStatus = new NotificationStatus(
                ticket: ticket,
                finishedWorkflowCount: 3,
                finalSend: false,
        ).addToNotifications(existingNotification).save(flush: true)

        // the very same run once more, so the batch changes nothing and must not force another status mail
        CreateNotification createNotification = createCreateNotification(ticket: ticket)
        createNotification.addToWorkflowRuns(alreadyCoveredRun)
        createNotification.save(flush: true)

        when:
        notificationService.createNotifications(createNotification.id)

        then:
        existingNotification.refresh().workflowRuns == [alreadyCoveredRun] as Set
        existingStatus.refresh().finishedWorkflowCount == 3
    }

    // the GString-looking text in the exception message is the whole point of this test, so it must stay a plain String
    @SuppressWarnings('GStringExpressionWithinString')
    void "sendNotificationCreateErrorMail renders subject and body, leaving template characters of the stack trace intact"() {
        given:
        setUpMailOptions()
        Ticket ticket = createTicket()
        CreateNotification createNotification = createCreateNotification(ticket: ticket, state: WorkflowCreateState.PROCESSING)
        createNotification.addToWorkflowRuns(createWorkflowRun())
        createNotification.save(flush: true)

        // Groovy stack traces are full of '$' and may contain '${...}', which must reach the mail verbatim
        Throwable throwable = new RuntimeException('boom in NotificationService$_closure5 with ${notAVariable}')

        MailHelperService originalMailHelperService = notificationService.mailHelperService
        String capturedSubject = null
        String capturedBody = null
        notificationService.mailHelperService = Mock(MailHelperService) {
            1 * saveErrorMailInNewTransaction(_, _) >> { String subject, String body ->
                capturedSubject = subject
                capturedBody = body
                return null
            }
        }

        when:
        notificationService.sendNotificationCreateErrorMail(createNotification.id, throwable)

        then:
        capturedSubject.contains('Failed to create notification objects')
        capturedSubject.contains(ticket.ticketNumber)
        capturedBody.contains('NotificationService$_closure5')
        capturedBody.contains('${notAVariable}')
        capturedBody.contains("ctx.notificationService.updateCreateNotificationState(${createNotification.id}L")

        cleanup:
        notificationService.mailHelperService = originalMailHelperService
    }

    void "updateNotificationsReferencingRestartedWorkflowRun swaps a restarted run for its replacement"() {
        given:
        Workflow workflow = createWorkflow()
        WorkflowRun restartedRun = createWorkflowRun(workflow: workflow, state: WorkflowRun.State.RESTARTED)
        WorkflowRun replacementRun = createWorkflowRun(workflow: workflow, restartedFrom: restartedRun)
        Notification notification = createNotification().addToWorkflowRuns(restartedRun).save(flush: true)

        when:
        notificationService.updateNotificationsReferencingRestartedWorkflowRun()

        then:
        notification.refresh().workflowRuns == [replacementRun] as Set
    }

    @Unroll
    void "markCheckingNotificationsReady, when the workflow run is #state, then it becomes READY is #becomesReady"() {
        given:
        Notification notification = createNotification(notificationState: NotificationState.CHECKING)
                .addToWorkflowRuns(createWorkflowRun(state: state))
                .save(flush: true)

        when:
        notificationService.markCheckingNotificationsReady()

        then:
        notification.refresh().notificationState == (becomesReady ? NotificationState.READY : NotificationState.CHECKING)

        where:
        state                                          || becomesReady
        WorkflowRun.State.PENDING                      || false
        WorkflowRun.State.WAITING_FOR_USER             || false
        WorkflowRun.State.RUNNING_WES                  || false
        WorkflowRun.State.RUNNING_OTP                  || false
        WorkflowRun.State.FAILED                       || false
        // an operator parks a run in these two before restarting it, so they must not count as finished
        WorkflowRun.State.FAILED_WAITING               || false
        WorkflowRun.State.KILLED                       || false
        // the successor takes over, see updateNotificationsReferencingRestartedWorkflowRun
        WorkflowRun.State.RESTARTED                    || false
        WorkflowRun.State.LEGACY                       || false
        WorkflowRun.State.SKIPPED_MISSING_PRECONDITION || true
        WorkflowRun.State.SUCCESS                      || true
        WorkflowRun.State.FAILED_FINAL                 || true
    }

    @Unroll
    void "markCheckingNotificationsReady, when the depending notification is #dependencyState, then it becomes READY is #becomesReady"() {
        given:
        Notification dependency = createNotification(notificationState: dependencyState).save(flush: true)
        Notification notification = createNotification(notificationState: NotificationState.CHECKING)
                .addToWorkflowRuns(createWorkflowRun(state: WorkflowRun.State.SUCCESS))
                .addToDependingNotification(dependency)
                .save(flush: true)

        when:
        notificationService.markCheckingNotificationsReady()

        then: 'the mail of a dependency must be out before the depending one may follow'
        notification.refresh().notificationState == (becomesReady ? NotificationState.READY : NotificationState.CHECKING)

        where:
        dependencyState            || becomesReady
        NotificationState.CHECKING || false
        NotificationState.READY    || false
        NotificationState.CREATED  || true
        NotificationState.SKIPPED  || true
    }

    void "nextReadyNotification returns the oldest READY notification and ignores every other state"() {
        given:
        createNotification(notificationState: NotificationState.CHECKING).save(flush: true)
        createNotification(notificationState: NotificationState.CREATED).save(flush: true)
        createNotification(notificationState: NotificationState.SKIPPED).save(flush: true)

        when: 'no notification is READY'
        Notification noneReady = notificationService.nextReadyNotification()

        then: 'the notifications in other states are not handed out'
        noneReady == null

        when: 'two notifications become READY'
        Notification firstReady = createNotification(notificationState: NotificationState.READY).save(flush: true)
        createNotification(notificationState: NotificationState.READY).save(flush: true)
        Notification result = notificationService.nextReadyNotification()

        then: 'the lower id comes first, so the scheduler drains them in a stable order'
        result.id == firstReady.id
    }

    void "finishedNotificationCount counts only the notifications in a final state"() {
        given:
        NotificationStatus notificationStatus = createNotificationStatus()
                .addToNotifications(createNotification(notificationState: NotificationState.CREATED).save(flush: true))
                .addToNotifications(createNotification(notificationState: NotificationState.SKIPPED).save(flush: true))
                .addToNotifications(createNotification(notificationState: NotificationState.CHECKING).save(flush: true))
                .addToNotifications(createNotification(notificationState: NotificationState.READY).save(flush: true))
                .save(flush: true)

        expect: 'the same states the drift query counts, otherwise the status mail would repeat every tick'
        NotificationService.finishedNotificationCount(notificationStatus) == 2
    }

    void "finishedNotificationCount is zero for a status without any notification"() {
        given:
        NotificationStatus notificationStatus = createNotificationStatus()

        expect:
        NotificationService.finishedNotificationCount(notificationStatus) == 0
    }

    void "notificationStatusesNeedingMail returns only statuses whose finished count drifted"() {
        given:
        Notification createdNotification = createNotification(notificationState: NotificationState.CREATED).save(flush: true)

        NotificationStatus needsMail = createNotificationStatus(finishedWorkflowCount: 0)
                .addToNotifications(createdNotification).save(flush: true)
        NotificationStatus upToDate = createNotificationStatus(finishedWorkflowCount: 1)
                .addToNotifications(createdNotification).save(flush: true)

        // only the two final notifications count, so the stored 3 has drifted; also guards the join fetch against duplicate rows
        NotificationStatus mixedStates = createNotificationStatus(finishedWorkflowCount: 3)
                .addToNotifications(createNotification(notificationState: NotificationState.CREATED).save(flush: true))
                .addToNotifications(createNotification(notificationState: NotificationState.SKIPPED).save(flush: true))
                .addToNotifications(createNotification(notificationState: NotificationState.CHECKING).save(flush: true))
                .save(flush: true)

        NotificationStatus alreadyFinal = createNotificationStatus(finishedWorkflowCount: 99, finalSend: true)
                .addToNotifications(createdNotification).save(flush: true)

        when:
        List<NotificationStatus> result = notificationService.notificationStatusesNeedingMail()
        List<Long> ids = result*.id

        then:
        ids.contains(needsMail.id)
        !ids.contains(upToDate.id)
        ids.count { it == mixedStates.id } == 1
        !ids.contains(alreadyFinal.id)
        // the collection came back with the query, so createNotificationStatus does not trigger one query per status
        result.every { Hibernate.isInitialized(it.notifications) }
    }

    void "deleteOldNotifications removes an old finalSend status but keeps a notification it still references"() {
        given:
        // the status itself is not old enough to be deleted, so the notification it references must survive too
        Notification referencedByStatus = createNotification(notificationState: NotificationState.CREATED).save(flush: true)
        createNotificationStatus(finalSend: true).addToNotifications(referencedByStatus).save(flush: true)
        makeOld(referencedByStatus)

        NotificationStatus oldFinalStatus = createNotificationStatus(finalSend: true).save(flush: true)
        makeOld(oldFinalStatus)

        when:
        notificationService.deleteOldNotifications(30)

        then:
        Notification.exists(referencedByStatus.id)
        !NotificationStatus.exists(oldFinalStatus.id)
    }

    void "deleteOldNotifications removes old, unreferenced notifications but keeps dependency-referenced ones"() {
        given:
        Notification referencedAsDependency = createNotification(notificationState: NotificationState.SKIPPED).save(flush: true)
        createNotification(notificationState: NotificationState.CHECKING)
                .addToDependingNotification(referencedAsDependency).save(flush: true)
        makeOld(referencedAsDependency)

        Notification unreferenced = createNotification(notificationState: NotificationState.CREATED).save(flush: true)
        makeOld(unreferenced)

        when:
        notificationService.deleteOldNotifications(30)

        then:
        Notification.exists(referencedAsDependency.id)
        !Notification.exists(unreferenced.id)
    }

    void "changeProcessToWait resets stale PROCESSING entries back to WAITING"() {
        given:
        CreateNotification processing = createCreateNotification(state: WorkflowCreateState.PROCESSING)
        CreateNotification waiting = createCreateNotification(state: WorkflowCreateState.WAITING)
        CreateNotification success = createCreateNotification(state: WorkflowCreateState.SUCCESS)
        CreateNotification failed = createCreateNotification(state: WorkflowCreateState.FAILED)

        when:
        notificationService.changeProcessToWait()

        then:
        processing.refresh().state == WorkflowCreateState.WAITING
        waiting.refresh().state == WorkflowCreateState.WAITING
        success.refresh().state == WorkflowCreateState.SUCCESS
        failed.refresh().state == WorkflowCreateState.FAILED
    }

    // a dependency edge only exists as an artefact the consuming run takes as input from the producing run
    private void dependOn(WorkflowRun consumingRun, WorkflowRun producingRun) {
        createWorkflowRunInputArtefact(
                workflowRun: consumingRun,
                workflowArtefact: createWorkflowArtefact(producedBy: producingRun),
        )
    }

    private static Notification exactlyOneNotificationFor(Ticket ticket, Project project, Workflow workflow) {
        List<Notification> matches = workflow ?
                Notification.findAllByNotificationScopeAndNotificationScopeIdAndProjectAndWorkflow(
                        NotificationScope.TICKET, ticket.id, project, workflow) :
                Notification.findAllByNotificationScopeAndNotificationScopeIdAndProjectAndWorkflowIsNull(
                        NotificationScope.TICKET, ticket.id, project)
        assert matches.size() == 1
        return matches.first()
    }

    // refresh() afterwards is required: a bulk HQL update doesn't sync already-loaded entities, and a later delete() would fail on the stale version otherwise
    private static void makeOld(Entity domainObject) {
        Class domainClass = domainObject.class
        domainClass.executeUpdate(
                "update ${domainClass.simpleName} set lastUpdated = :lastUpdated, version = version + 1 where id = :id".toString(),
                [lastUpdated: Date.from(ZonedDateTime.now().minusDays(31).toInstant()), id: domainObject.id],
        )
        domainObject.refresh()
    }
}
