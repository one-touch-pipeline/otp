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

import grails.testing.gorm.DataTest
import grails.web.mapping.LinkGenerator
import spock.lang.Specification

import de.dkfz.tbi.otp.Comment
import de.dkfz.tbi.otp.TestMessageSourceService
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.ngsdata.FastqImportInstance
import de.dkfz.tbi.otp.notification.Notification
import de.dkfz.tbi.otp.notification.NotificationScope
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.tracking.Ticket
import de.dkfz.tbi.otp.tracking.TicketService
import de.dkfz.tbi.otp.workflowExecution.Workflow
import de.dkfz.tbi.otp.workflowExecution.WorkflowRun
import de.dkfz.tbi.otp.workflowExecution.WorkflowStepSkipMessage

class NotificationMessageServiceSpec extends Specification implements DataTest {

    private static final NotificationContent EMPTY_CONTENT = new NotificationContent([] as Set, [:], [] as Set, [] as Set)

    private static final String INTRO = "initialized 'Workflow' workflows of project 'Project' are finished"

    private static final String PROCESSED_DATA_HEADER = 'Processed data:'

    private static final String SKIPPED_HEADER = 'The following workflows were skipped:'

    private static final String FAILURE_HEADER = 'The following workflows were failed final:'

    private static final String GUI_URLS_HEADER = 'An overview is shown on:'

    private static final String FILE_PATTERNS_HEADER = 'You can find the files in the "view-by-pid" folder:'

    private static final String CONFIGURATIONS_HEADER = 'The workflow used the following configurations:'

    private static final String CONFIGURATION_SECTION = CONFIGURATIONS_HEADER + '\n- config'

    private static final Map<String, String> FAILURE_SECTIONS = [
            skipped    : SKIPPED_HEADER + '\n- sample:\n  skip reason',
            failedFinal: FAILURE_HEADER + '\n- sample:\n  failure reason',
    ].asImmutable()

    private static final List<String> PROBLEM_HANDLINGS = ['skipped', 'failedFinal'].asImmutable()

    /** template of the message source of the tests, which resolves to blank text */
    private static final String BLANK_TEMPLATE = 'test.template.blank'

    private static final String SNV_ACKNOWLEDGEMENT = 'SNV Calling is done by the SNV plugin for Roddy. For questions regarding the output ' +
            'and its interpretation the code and basic documentation are available at https://github.com/DKFZ-ODCF/SNVCallingWorkflow .'

    private static final String INDEL_ACKNOWLEDGEMENT = 'Indel Calling is done by the Indel plugin for Roddy. For questions regarding the output ' +
            'and its interpretation the code and basic documentation are available at https://github.com/DKFZ-ODCF/IndelCallingWorkflow .'

    @Override
    Class[] getDomainClassesToMock() {
        return [Notification, Ticket]
    }

    void "createWorkflowSubject truncates the ILSe block only above the cap"() {
        given:
        Ticket ticket = Mock(Ticket)
        Project project = Mock(Project) {
            getName() >> 'Project'
        }
        Workflow workflow = Mock(Workflow) {
            getDisplayName() >> 'Workflow'
        }
        Notification notification = Mock(Notification) {
            getProject() >> project
            getWorkflow() >> workflow
        }
        NotificationMessageService service = createService(
                ticketService: Mock(TicketService) {
                    getPrefixedTicketNumber(ticket) >> 'OTP#123'
                },
                workflowNotificationContentService: Mock(WorkflowNotificationContentService) {
                    fetchIlseNumbers(ticket) >> ilseNumbers
                },
        )

        expect:
        service.createWorkflowSubject(ticket, notification, []) ==
                "[OTP#123] TO BE SENT: ${expectedIlse}Project Workflow finished"

        where:
        ilseNumbers             || expectedIlse
        [1, 2]                  || '[S#1,2] '
        [1, 2, 3, 4, 5]         || '[S#1,2,3,4,5] '
        [1, 2, 3, 4, 5, 6]      || '[S#1,2,3,4,5...] '
        []                      || ''
    }

    void "createWorkflowBody wraps the content of the workflow into the mail template and appends the FAQ"() {
        given:
        WorkflowRun delivered = createRun(1, WorkflowRun.State.SUCCESS, notificationText: 'config')
        Notification notification = createNotification([delivered])
        WorkflowNotification provider = createProvider(
                allRunsContent: new NotificationContent(['data'] as Set, [:], [] as Set, [] as Set),
        )
        NotificationMessageService service = createService(
                processingOptionService: Mock(ProcessingOptionService) {
                    findOptionAsString(ProcessingOption.OptionName.NOTIFICATION_TEMPLATE_FAQ_LINK) >> 'https://otp/faq'
                    findOptionAsString(ProcessingOption.OptionName.GUI_CONTACT_DATA_SUPPORT_EMAIL) >> 'support@example.com'
                },
        )
        String expectedContent = createExpectedContent([
                PROCESSED_DATA_HEADER + '\n- data',
                CONFIGURATION_SECTION,
        ])

        expect:
        service.createWorkflowBody(notification, provider) == "\nDear user,\n\n${expectedContent}\n\nBest regards,\nOTP\n" +
                '\nIf you have any further questions please refer to our FAQs https://otp/faq ' +
                'and do not hesitate to contact us if there are still open questions: support@example.com\n\n'
    }

    void "createWorkflowContent, when #name, then render the intro only"() {
        given:
        NotificationMessageService service = createService()
        Notification notification = createNotification(states?.withIndex()?.collect { WorkflowRun.State state, int index ->
            createRun(index + 1, state)
        })
        WorkflowNotification provider = createProvider()

        expect:
        service.createWorkflowContent(notification, provider) == INTRO

        where:
        name                                     | states
        'the notification has no runs at all'    | null
        'the notification has an empty run list' | []
        'none of the runs provides content'      | [WorkflowRun.State.SUCCESS, WorkflowRun.State.FAILED_FINAL, WorkflowRun.State.RUNNING_OTP]
    }

    void "createWorkflowContent, when all sections have content, then render them in a fixed order independent of the order of the runs"() {
        given:
        NotificationMessageService service = createService()
        WorkflowRun delivered = createRun(1, WorkflowRun.State.SUCCESS, notificationText: 'configA')
        WorkflowRun skipped = createRun(2, WorkflowRun.State.SKIPPED_MISSING_PRECONDITION,
                skipMessage: new WorkflowStepSkipMessage(message: 'missing input'), notificationText: 'configC')
        WorkflowRun failed = createRun(3, WorkflowRun.State.FAILED_FINAL,
                comment: new Comment(comment: 'data is invalid'), notificationText: 'configB')
        WorkflowRun unexpected = createRun(4, WorkflowRun.State.RUNNING_OTP, notificationText: 'configOfUnexpectedRun')
        Notification notification = createNotification([failed, unexpected, delivered, skipped])
        WorkflowNotification provider = createProvider(
                allRunsContent: new NotificationContent(['data1', 'data2'] as Set, [:], ['url1', 'url2'] as Set,
                        ['pattern1', 'pattern2'] as Set),
                problemContent: new NotificationContent([] as Set, [2L: 'skipped sample', 3L: 'failed sample'], [] as Set, [] as Set),
                acknowledgementTemplate: 'notification.template.references.snv',
        )

        expect:
        service.createWorkflowContent(notification, provider) == createExpectedContent([
                PROCESSED_DATA_HEADER + '\n- data1\n- data2',
                SKIPPED_HEADER + '\n- skipped sample:\n  missing input',
                FAILURE_HEADER + '\n- failed sample:\n  data is invalid',
                GUI_URLS_HEADER + '\n- url1\n- url2',
                FILE_PATTERNS_HEADER + '\n- pattern1\n- pattern2',
                CONFIGURATIONS_HEADER + '\n- configA\n- configB\n- configC',
                SNV_ACKNOWLEDGEMENT,
        ])
    }

    void "createWorkflowContent, when a section contains multiple entries, then sort them alphabetically"() {
        given:
        NotificationMessageService service = createService()
        WorkflowRun skippedB = createRun(1, WorkflowRun.State.SKIPPED_MISSING_PRECONDITION,
                skipMessage: new WorkflowStepSkipMessage(message: 'reason b'))
        WorkflowRun skippedA = createRun(2, WorkflowRun.State.SKIPPED_MISSING_PRECONDITION,
                skipMessage: new WorkflowStepSkipMessage(message: 'reason a'))
        WorkflowRun failedZ = createRun(3, WorkflowRun.State.FAILED_FINAL, comment: new Comment(comment: 'z reason'))
        WorkflowRun failedA = createRun(4, WorkflowRun.State.FAILED_FINAL, comment: new Comment(comment: 'a reason'))
        Notification notification = createNotification([skippedB, skippedA, failedZ, failedA])
        WorkflowNotification provider = createProvider(
                allRunsContent: new NotificationContent(['data10', 'data2', 'data9'] as Set, [:], ['url2', 'url10'] as Set,
                        ['pattern2', 'pattern10'] as Set),
                problemContent: new NotificationContent([] as Set,
                        [1L: 'sample b', 2L: 'sample a', 3L: 'same sample', 4L: 'same sample'], [] as Set, [] as Set),
        )

        expect:
        service.createWorkflowContent(notification, provider) == createExpectedContent([
                PROCESSED_DATA_HEADER + '\n- data10\n- data2\n- data9',
                SKIPPED_HEADER + '\n- sample a:\n  reason a\n- sample b:\n  reason b',
                FAILURE_HEADER + '\n- same sample:\n  a reason\n- same sample:\n  z reason',
                GUI_URLS_HEADER + '\n- url10\n- url2',
                FILE_PATTERNS_HEADER + '\n- pattern10\n- pattern2',
        ])
    }

    void "createWorkflowContent, when a value spans multiple lines, then indent its following lines"() {
        given:
        NotificationMessageService service = createService()
        WorkflowRun delivered = createRun(1, WorkflowRun.State.SUCCESS, notificationText: 'config\nline2')
        Notification notification = createNotification([delivered])
        WorkflowNotification provider = createProvider(
                allRunsContent: new NotificationContent(['data\nline2'] as Set, [:], ['url\nline2'] as Set, ['pattern\nline2'] as Set),
        )

        expect:
        service.createWorkflowContent(notification, provider) == createExpectedContent([
                PROCESSED_DATA_HEADER + '\n- data\n  line2',
                GUI_URLS_HEADER + '\n- url\n  line2',
                FILE_PATTERNS_HEADER + '\n- pattern\n  line2',
                CONFIGURATIONS_HEADER + '\n- config\n  line2',
        ])
    }

    void "createWorkflowContent, when a value is blank, then do not render an entry for it"() {
        given:
        NotificationMessageService service = createService()
        WorkflowRun deliveredWithoutText = createRun(1, WorkflowRun.State.SUCCESS, notificationText: '')
        WorkflowRun delivered = createRun(2, WorkflowRun.State.SUCCESS, notificationText: 'config')
        Notification notification = createNotification([deliveredWithoutText, delivered])
        WorkflowNotification provider = createProvider(
                allRunsContent: new NotificationContent([null, '', 'data'] as Set, [:], [null, '', 'url'] as Set,
                        [null, '', 'pattern'] as Set),
        )

        expect:
        service.createWorkflowContent(notification, provider) == createExpectedContent([
                PROCESSED_DATA_HEADER + '\n- data',
                GUI_URLS_HEADER + '\n- url',
                FILE_PATTERNS_HEADER + '\n- pattern',
                CONFIGURATION_SECTION,
        ])
    }

    void "createWorkflowContent, when a run is in state #state, then handle it as '#handling'"() {
        given:
        NotificationMessageService service = createService()
        Map runProperties = [
                notificationText: 'config',
                comment         : new Comment(comment: 'failure reason'),
                skipMessage     : new WorkflowStepSkipMessage(message: 'skip reason'),
        ]
        WorkflowRun workflowRun = createRun(runProperties, 1, state)
        Notification notification = createNotification([workflowRun])
        List<List<WorkflowRun>> calls = []
        WorkflowNotification provider = createProvider(
                calls: calls,
                problemContent: new NotificationContent([] as Set, [1L: 'sample'], [] as Set, [] as Set),
        )

        when:
        String content = service.createWorkflowContent(notification, provider)

        then:
        calls == [[workflowRun], handling in PROBLEM_HANDLINGS ? [workflowRun] : []]
        content == createExpectedContent([
                FAILURE_SECTIONS[handling],
                handling == 'ignored' ? null : CONFIGURATION_SECTION,
        ])

        where:
        state                                          || handling
        WorkflowRun.State.SUCCESS                      || 'delivered'
        WorkflowRun.State.SKIPPED_MISSING_PRECONDITION || 'skipped'
        WorkflowRun.State.FAILED_FINAL                 || 'failedFinal'
        WorkflowRun.State.PENDING                      || 'ignored'
        WorkflowRun.State.WAITING_FOR_USER             || 'ignored'
        WorkflowRun.State.RUNNING_WES                  || 'ignored'
        WorkflowRun.State.RUNNING_OTP                  || 'ignored'
        WorkflowRun.State.FAILED                       || 'ignored'
        WorkflowRun.State.FAILED_WAITING               || 'ignored'
        WorkflowRun.State.KILLED                       || 'ignored'
        WorkflowRun.State.RESTARTED                    || 'ignored'
        WorkflowRun.State.LEGACY                       || 'ignored'
    }

    void "createWorkflowContent, when collecting the configurations, then #name"() {
        given:
        NotificationMessageService service = createService()
        Notification notification = createNotification(runs.withIndex().collect { List run, int index ->
            createRun(index + 1, run[0] as WorkflowRun.State, notificationText: run[1] as String)
        })
        WorkflowNotification provider = createProvider()

        expect:
        service.createWorkflowContent(notification, provider) == createExpectedContent([expectedSection])

        where:
        name                                 | runs                                                                                                                                                    || expectedSection
        'list a duplicated text only once'   | [[WorkflowRun.State.SUCCESS, 'config'], [WorkflowRun.State.SUCCESS, 'config']]                                                                           || CONFIGURATION_SECTION
        'ignore runs without a text'         | [[WorkflowRun.State.SUCCESS, null], [WorkflowRun.State.SUCCESS, '']]                                                                                     || null
        'ignore runs in an unexpected state' | [[WorkflowRun.State.RUNNING_OTP, 'ignored'], [WorkflowRun.State.SUCCESS, 'config']]                                                                      || CONFIGURATION_SECTION
        'include delivered and problem runs' | [[WorkflowRun.State.SUCCESS, 'configA'], [WorkflowRun.State.FAILED_FINAL, 'configB'], [WorkflowRun.State.SKIPPED_MISSING_PRECONDITION, 'configC']]       || CONFIGURATIONS_HEADER + '\n- configA\n- configB\n- configC'
    }

    void "createWorkflowContent, when rendering the reason of a problem run, then #name"() {
        given:
        NotificationMessageService service = createService()
        Map runProperties = [
                comment    : commentText == null ? null : new Comment(comment: commentText),
                skipMessage: skipMessageText == null ? null : new WorkflowStepSkipMessage(message: skipMessageText),
        ]
        WorkflowRun workflowRun = createRun(runProperties, 1, state)
        Notification notification = createNotification([workflowRun])
        WorkflowNotification provider = createProvider(
                problemContent: new NotificationContent([] as Set, [1L: 'sample'], [] as Set, [] as Set),
        )

        expect:
        service.createWorkflowContent(notification, provider) == createExpectedContent(["${header}\n- sample:\n  ${expectedReason}"])

        where:
        name                                              | state                                          | commentText                        | skipMessageText  || header         | expectedReason
        'use the comment of the run'                      | WorkflowRun.State.FAILED_FINAL                 | 'stopped'                          | null             || FAILURE_HEADER | 'stopped'
        'use the default reason if no comment exists'     | WorkflowRun.State.FAILED_FINAL                 | null                               | null             || FAILURE_HEADER | '(no reason documented)'
        'use the default reason if the comment is empty'  | WorkflowRun.State.FAILED_FINAL                 | ''                                 | null             || FAILURE_HEADER | '(no reason documented)'
        'indent a multi line comment'                     | WorkflowRun.State.FAILED_FINAL                 | 'stopped\nbecause data is invalid' | null             || FAILURE_HEADER | 'stopped\n  because data is invalid'
        'prefer the skip message over the comment'        | WorkflowRun.State.SKIPPED_MISSING_PRECONDITION | 'ignored comment'                  | 'missing input'  || SKIPPED_HEADER | 'missing input'
        'indent a multi line skip message'                | WorkflowRun.State.SKIPPED_MISSING_PRECONDITION | null                               | 'missing\ninput' || SKIPPED_HEADER | 'missing\n  input'
    }

    void "createWorkflowContent, when a skipped run has no skip message, then fail"() {
        given:
        NotificationMessageService service = createService()
        WorkflowRun workflowRun = createRun(1, WorkflowRun.State.SKIPPED_MISSING_PRECONDITION, skipMessage: null)
        Notification notification = createNotification([workflowRun])
        WorkflowNotification provider = createProvider(
                problemContent: new NotificationContent([] as Set, [1L: 'sample'], [] as Set, [] as Set),
        )

        when:
        service.createWorkflowContent(notification, provider)

        then:
        thrown(NullPointerException)
    }

    void "createWorkflowContent, when the content of a problem run has #name, then do not render the run"() {
        given:
        NotificationMessageService service = createService()
        WorkflowRun skipped = createRun(1, WorkflowRun.State.SKIPPED_MISSING_PRECONDITION,
                skipMessage: new WorkflowStepSkipMessage(message: 'skip reason'))
        WorkflowRun delivered = createRun(2, WorkflowRun.State.SUCCESS)
        Notification notification = createNotification([skipped, delivered])
        WorkflowNotification provider = createProvider(
                problemContent: new NotificationContent([] as Set, problemTexts, [] as Set, [] as Set),
        )

        expect:
        service.createWorkflowContent(notification, provider) == createExpectedContent([expectedSection])

        where:
        name                                     | problemTexts             || expectedSection
        'no text'                                | [:]                      || null
        'an empty text'                          | [1L: '']                 || null
        'only a text of a run without a problem' | [2L: 'delivered sample'] || null
        'a text for the run'                     | [1L: 'sample']           || SKIPPED_HEADER + '\n- sample:\n  skip reason'
    }

    void "createWorkflowContent, when the acknowledgement template #name, then #expectation"() {
        given:
        NotificationMessageService service = createService()
        Notification notification = createNotification([])
        WorkflowNotification provider = createProvider(acknowledgementTemplate: acknowledgementTemplate)

        expect:
        service.createWorkflowContent(notification, provider) == createExpectedContent([expectedSection])

        where:
        name                       | acknowledgementTemplate                  || expectation                 | expectedSection
        'is missing'               | null                                     || 'render no acknowledgement' | null
        'resolves to blank text'   | BLANK_TEMPLATE                           || 'render no acknowledgement' | null
        'points to the snv text'   | 'notification.template.references.snv'   || 'render the snv text'       | SNV_ACKNOWLEDGEMENT
        'points to the indel text' | 'notification.template.references.indel' || 'render the indel text'     | INDEL_ACKNOWLEDGEMENT
    }

    void "createWorkflowContent, when building the content, then pass all runs and the problem runs to the provider"() {
        given:
        NotificationMessageService service = createService()
        WorkflowRun delivered = createRun(1, WorkflowRun.State.SUCCESS)
        WorkflowRun skipped = createRun(2, WorkflowRun.State.SKIPPED_MISSING_PRECONDITION)
        WorkflowRun failed = createRun(3, WorkflowRun.State.FAILED_FINAL)
        WorkflowRun running = createRun(4, WorkflowRun.State.RUNNING_OTP)
        WorkflowRun restarted = createRun(5, WorkflowRun.State.RESTARTED)
        Notification notification = createNotification([delivered, skipped, failed, running, restarted])
        List<List<WorkflowRun>> calls = []
        WorkflowNotification provider = createProvider(calls: calls)

        when:
        service.createWorkflowContent(notification, provider)

        then:
        calls == [
                [delivered, skipped, failed, running, restarted],
                [skipped, failed],
        ]
    }

    void "createStatusBody renders ordered workflow states and import links"() {
        given:
        Ticket ticket = Mock(Ticket) {
            getTicketNumber() >> '123'
        }
        Workflow shortWorkflow = Mock(Workflow) {
            getName() >> 'short'
            getDisplayName() >> 'Alpha'
        }
        Workflow restartedWorkflow = Mock(Workflow) {
            getName() >> 'restarted'
            getDisplayName() >> 'Beta'
        }
        Workflow longWorkflow = Mock(Workflow) {
            getName() >> 'long'
            getDisplayName() >> 'Long workflow'
        }
        List<WorkflowRun> workflowRuns = [
                Mock(WorkflowRun) {
                    getWorkflow() >> shortWorkflow
                    getState() >> WorkflowRun.State.FAILED
                },
                Mock(WorkflowRun) {
                    getWorkflow() >> shortWorkflow
                    getState() >> WorkflowRun.State.SUCCESS
                },
                Mock(WorkflowRun) {
                    getWorkflow() >> shortWorkflow
                    getState() >> WorkflowRun.State.LEGACY
                },
                Mock(WorkflowRun) {
                    getWorkflow() >> restartedWorkflow
                    getState() >> WorkflowRun.State.RESTARTED
                },
                Mock(WorkflowRun) {
                    getWorkflow() >> longWorkflow
                    getState() >> WorkflowRun.State.SKIPPED_MISSING_PRECONDITION
                },
                Mock(WorkflowRun) {
                    getWorkflow() >> longWorkflow
                    getState() >> WorkflowRun.State.SUCCESS
                },
                Mock(WorkflowRun) {
                    getWorkflow() >> longWorkflow
                    getState() >> WorkflowRun.State.FAILED_FINAL
                },
        ]
        FastqImportInstance secondImport = Mock(FastqImportInstance) {
            getId() >> 2L
        }
        FastqImportInstance firstImport = Mock(FastqImportInstance) {
            getId() >> 1L
        }
        NotificationMessageService service = createService(
                linkGenerator: Mock(LinkGenerator) {
                    link { Map parameters -> parameters.id == 1L } >> 'https://otp/import/1'
                    link { Map parameters -> parameters.id == 2L } >> 'https://otp/import/2'
                },
                ticketService: Mock(TicketService) {
                    getPrefixedTicketNumber(ticket) >> 'OTP#123'
                    getAllFastqImportInstances(ticket) >> [secondImport, firstImport]
                },
        )
        String expectedBody = '\nDear data manager,\n\n' +
                'The workflows of ticket 123:\n' +
                '- Alpha:         IN_PROGRESS: 1 FAILED, 1 SUCCESS, 1 LEGACY\n' +
                '- Beta:          IN_PROGRESS: 1 RESTARTED\n' +
                '- Long workflow: ALL_DONE: 1 SKIPPED_MISSING_PRECONDITION, 1 SUCCESS, 1 FAILED_FINAL\n\n\n' +
                'Details about metadata import can be found\n' +
                '- https://otp/import/1\n- https://otp/import/2\n\n\n' +
                'Best regards,\nOTP'

        expect:
        service.createStatusSubject(ticket, false) == '[OTP#123] Processing Status Update'
        service.createStatusBody(ticket, workflowRuns) == expectedBody
    }

    void "creates unsupported, all-workflows, and empty-status messages"() {
        given:
        Ticket ticket = Mock(Ticket) {
            getTicketNumber() >> '123'
        }
        Project project = Mock(Project) {
            getName() >> 'Project'
        }
        Notification notification = Mock(Notification) {
            getId() >> 12L
            getNotificationScope() >> NotificationScope.PROJECT
            getProject() >> project
        }
        NotificationMessageService service = createService(
                linkGenerator: Mock(LinkGenerator),
                processingOptionService: Mock(ProcessingOptionService),
                ticketService: Mock(TicketService) {
                    getPrefixedTicketNumber(ticket) >> 'OTP#123'
                    getAllFastqImportInstances(ticket) >> []
                },
                workflowNotificationContentService: Mock(WorkflowNotificationContentService) {
                    fetchIlseNumbers(ticket) >> []
                },
        )

        expect:
        service.createUnsupportedSubject() == 'Unsupported notification scope'
        service.createUnsupportedBody(notification) == "Notification 12 with scope 'PROJECT' cannot be handled."
        service.createAllWorkflowsSubject(ticket, notification) ==
                '[OTP#123] TO BE SENT: Project all workflows finished'
        service.createAllWorkflowsBody(['Alpha', 'Beta']) ==
                '\nDear user,\n\nAlpha\n\n--------------------\n\nBeta\n\nBest regards,\nOTP\n'
        service.createStatusSubject(ticket, true) == '[OTP#123] Final Processing Status Update'
        service.createStatusBody(ticket, []) == '\nDear data manager,\n\n' +
                'This ticket no longer contains any workflows to report.\n\n\nBest regards,\nOTP'
    }

    /**
     * Creates the service with a message source using the real messages, extended by {@link #BLANK_TEMPLATE}.
     *
     * @param properties the collaborators needed by the tested method
     */
    private NotificationMessageService createService(Map properties = [:]) {
        TestMessageSourceService messageSourceService = new TestMessageSourceService()
        messageSourceService.staticMessageSource.addMessage(BLANK_TEMPLATE, TestMessageSourceService.defaultLocale, ' \n ')
        return new NotificationMessageService([messageSourceService: messageSourceService] + properties)
    }

    /**
     * Creates a mocked {@link WorkflowRun}.
     *
     * @param properties may contain a {@code notificationText}, a {@code comment} and a {@code skipMessage}; the skip message
     * defaults to a message derived from the id, since a skipped run always has one
     */
    private WorkflowRun createRun(Map properties = [:], long id, WorkflowRun.State state) {
        WorkflowStepSkipMessage skipMessage = properties.containsKey('skipMessage') ?
                properties.skipMessage as WorkflowStepSkipMessage :
                new WorkflowStepSkipMessage(message: "skip message of run ${id}")
        return Mock(WorkflowRun) {
            getId() >> id
            getState() >> state
            getNotificationText() >> properties.notificationText
            getComment() >> properties.comment
            getSkipMessage() >> skipMessage
        }
    }

    private Notification createNotification(Collection<WorkflowRun> workflowRuns) {
        Project project = Mock(Project) {
            getName() >> 'Project'
        }
        Workflow workflow = Mock(Workflow) {
            getDisplayName() >> 'Workflow'
        }
        return Mock(Notification) {
            getId() >> 1L
            getProject() >> project
            getWorkflow() >> workflow
            getWorkflowRuns() >> (workflowRuns == null ? null : workflowRuns as Set)
        }
    }

    /**
     * Creates a stubbed {@link WorkflowNotification}, which answers the first call of
     * {@link WorkflowNotification#buildContent} with the {@code allRunsContent} and the second one with the
     * {@code problemContent}.
     *
     * @param properties may contain an {@code allRunsContent}, a {@code problemContent}, an {@code acknowledgementTemplate}
     * and a list {@code calls} collecting the runs of each call of {@link WorkflowNotification#buildContent}
     */
    private WorkflowNotification createProvider(Map properties = [:]) {
        List<List<WorkflowRun>> calls = (properties.containsKey('calls') ? properties.calls : []) as List
        NotificationContent allRunsContent = (properties.allRunsContent ?: EMPTY_CONTENT) as NotificationContent
        NotificationContent problemContent = (properties.problemContent ?: EMPTY_CONTENT) as NotificationContent
        return Stub(WorkflowNotification) {
            pipelineAcknowledgementTemplate() >> properties.acknowledgementTemplate
            buildContent(_) >> { List<Collection<WorkflowRun>> arguments ->
                calls << arguments.first().toList()
                return calls.size() == 1 ? allRunsContent : problemContent
            }
        }
    }

    private static String createExpectedContent(List<String> sections) {
        return ([INTRO] + sections.findAll()).join('\n\n\n')
    }
}
