/*
 * Copyright 2011-2024 The OTP authors
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
package de.dkfz.tbi.otp.notification

import grails.testing.mixin.integration.Integration
import grails.gorm.transactions.Rollback
import org.grails.spring.context.support.PluginAwareResourceBundleMessageSource
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.domainFactory.pipelines.IsRoddy
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.qcTrafficLight.QcTrafficLightNotificationService
import de.dkfz.tbi.otp.tracking.*
import de.dkfz.tbi.otp.administration.MailHelperService
import de.dkfz.tbi.otp.utils.MessageSourceService

@Rollback
@Integration
class NotificationDigestServiceIntegrationSpec extends Specification implements IsRoddy {

    static final String FAQ = "FAQ"
    static final String SEQ_CENTER_COMMENT = "seqCenterComment"

    @SuppressWarnings("GStringExpressionWithinString")
    static final String NOTIFICATION_DIGEST_TEMPLATE = '''\
        |${content}
        |${emailSenderSalutation}
        |${faq}'''.stripMargin()

    static final String SALUTATION = "salutation"

    void setupData() {
        DomainFactory.createProcessingOptionLazy(name: ProcessingOption.OptionName.TICKET_SYSTEM_NUMBER_PREFIX, value: "prefix")
        DomainFactory.createProcessingOptionLazy(name: ProcessingOption.OptionName.HELP_DESK_TEAM_NAME, value: SALUTATION)
    }

    NotificationCommand createNotificationCommand(Map properties = [:]) {
        return new NotificationCommand([
                ticket         : properties.ticket ?: createTicket(),
                fastqImportInstance: properties.fastqImportInstance ?: createFastqImportInstance(),
                steps              : properties.steps ?: Ticket.ProcessingStep.values() - Ticket.ProcessingStep.FASTQC,
                notifyQcThresholds : properties.notifyQcThresholds ?: true,
        ] + properties)
    }

    Map<String, Object> setupNotificationCommand() {
        Ticket ticket = createTicket()

        SeqTrack seqTrack1 = createSeqTrackWithOneFastqFile()
        SeqTrack seqTrack2 = createSeqTrackWithOneFastqFile()
        List<RawSequenceFile> rawSequenceFiles1 = ([seqTrack1, seqTrack2]*.sequenceFiles).flatten()
        FastqImportInstance fastqImportInstance1 = createFastqImportInstance(ticket: ticket, sequenceFiles: rawSequenceFiles1)
        rawSequenceFiles1.each {
            it.fastqImportInstance = fastqImportInstance1
            it.save(flush: true)
        }

        SeqTrack seqTrack3 = createSeqTrackWithOneFastqFile()
        List<RawSequenceFile> rawSequenceFiles2 = ([seqTrack3]*.sequenceFiles).flatten()
        FastqImportInstance fastqImportInstance2 = createFastqImportInstance(ticket: ticket, sequenceFiles: rawSequenceFiles2)
        rawSequenceFiles2.each {
            it.fastqImportInstance = fastqImportInstance2
            it.save(flush: true)
        }

        List<Ticket.ProcessingStep> processingSteps = [
                Ticket.ProcessingStep.INSTALLATION,
                Ticket.ProcessingStep.ALIGNMENT,
                Ticket.ProcessingStep.SNV,
        ]

        NotificationCommand cmd = createNotificationCommand(
                ticket         : ticket,
                fastqImportInstance: fastqImportInstance1,
                steps              : processingSteps,
                notifyQcThresholds : true,
        )
        List<SeqTrack> seqTracks = [seqTrack1, seqTrack2, seqTrack3]
        return [
                cmd      : cmd,
                seqTracks: seqTracks,
                projects : seqTracks*.project,
        ]
    }

    NotificationDigestService setupMockedServiceForPrepareNotificationsTests(Integer buildCalls, Integer sendCalls, List<UserProjectRole> projectUsers = []) {
        return new NotificationDigestService(
                createNotificationTextService: Mock(CreateNotificationTextService),
                ilseSubmissionService: Mock(IlseSubmissionService),
                notificationCreator: new NotificationCreator(),
                messageSourceService: Mock(MessageSourceService),
                userProjectRoleService: Mock(UserProjectRoleService) {
                    buildCalls * getProjectUsersToBeNotified(_) >> projectUsers
                },
                processingOptionService: new ProcessingOptionService(),
                mailHelperService: Mock(MailHelperService) {
                    sendCalls * saveMail(_, _, _)
                },
                qcTrafficLightNotificationService: Mock(QcTrafficLightNotificationService),
                ticketService: new TicketService(
                        processingOptionService: new ProcessingOptionService(),
                ),
        )
    }

    void "prepareNotifications, creates one notification per project"() {
        given:
        setupData()
        Map<String, Object> map = setupNotificationCommand()

        UserProjectRole projectUser = DomainFactory.createUserProjectRole()

        NotificationDigestService service = setupMockedServiceForPrepareNotificationsTests(3, 0, [projectUser])

        when:
        List<PreparedNotification> preparedNotifications = service.prepareNotifications(map.cmd as NotificationCommand)

        then:
        preparedNotifications.size() == 3
        TestCase.assertContainSame(preparedNotifications*.project, map.projects as List<Project>)
        TestCase.assertContainSame(preparedNotifications*.seqTracks.flatten(), map.seqTracks as List<SeqTrack>)
        preparedNotifications*.toBeNotifiedProjectUsers.flatten().unique() == [projectUser]
    }

    void "prepareAndSendNotifications, preparesNotifications and sends each"() {
        given:
        setupData()
        Map<String, Object> map = setupNotificationCommand()

        UserProjectRole projectUser = DomainFactory.createUserProjectRole()

        NotificationDigestService service = setupMockedServiceForPrepareNotificationsTests(3, 3, [projectUser])

        when:
        List<PreparedNotification> preparedNotifications = service.prepareAndSendNotifications(map.cmd as NotificationCommand)

        then:
        preparedNotifications.size() == 3
        TestCase.assertContainSame(preparedNotifications*.project, map.projects as List<Project>)
        TestCase.assertContainSame(preparedNotifications*.seqTracks.flatten(), map.seqTracks as List<SeqTrack>)
        preparedNotifications*.toBeNotifiedProjectUsers.flatten().unique() == [projectUser]
    }

    NotificationDigestService setupMockedServiceForBuildNotificationDigestTest(List<Ticket.ProcessingStep> finishedSteps = []) {
        Map<Ticket.ProcessingStep, Boolean> stepsToNotify = Ticket.ProcessingStep.values().collectEntries { Ticket.ProcessingStep step ->
            [(step): (step in finishedSteps)]
        }
        return new NotificationDigestService(
                createNotificationTextService    : Mock(CreateNotificationTextService) {
                    _ * buildStepNotifications(_, _) >> { List<Ticket.ProcessingStep> processingSteps, ProcessingStatus status ->
                        return processingSteps.findAll { stepsToNotify[it] }*.name().sort().join(",")
                    }
                    _ * buildSeqCenterComment(_) >> { return SEQ_CENTER_COMMENT }
                    _ * getFaq() >> { return FAQ }
                },
                qcTrafficLightNotificationService: Mock(QcTrafficLightNotificationService),
                messageSourceService             : new MessageSourceService(
                        messageSource: Mock(PluginAwareResourceBundleMessageSource) {
                            _ * getMessageInternal("notification.template.digest", [], _) >> NOTIFICATION_DIGEST_TEMPLATE
                        }
                ),
                processingOptionService          : new ProcessingOptionService(),
        )
    }

    @Unroll
    void "buildNotificationDigest, empty string when nothing to notify, #testCase"() {
        given:
        setupData()

        List<Ticket.ProcessingStep> steps = [
                Ticket.ProcessingStep.INSTALLATION,
                Ticket.ProcessingStep.ALIGNMENT,
                Ticket.ProcessingStep.SNV,
        ]
        NotificationDigestService service = setupMockedServiceForBuildNotificationDigestTest(stepsHaveResult ? steps : [])

        NotificationCommand cmd = createNotificationCommand(
                steps             : notifySteps ? steps : [],
                notifyQcThresholds: notifyQc,
        )

        expect:
        service.buildNotificationDigest(cmd, null) == ""

        where:
        notifySteps | stepsHaveResult | notifyQc | testCase
        false       | true            | false    | "no steps, no QC"
        true        | false           | false    | "steps, but no results, no qc"
        false       | true            | true     | "no steps, QC, but no bams"
        false       | true            | false    | "no steps, no QC, but bams"
    }

    @Unroll
    void "buildNotificationDigest, only adds SeqCenter comment when the provided steps have something to notify, #steps"() {
        given:
        setupData()

        NotificationDigestService service = setupMockedServiceForBuildNotificationDigestTest(steps as List<Ticket.ProcessingStep>)

        FastqImportInstance fastqImportInstance = createFastqImportInstance(sequenceFiles: [createFastqFile()])
        NotificationCommand cmd = createNotificationCommand(
                ticket         : fastqImportInstance.ticket,
                fastqImportInstance: fastqImportInstance,
                notifyQcThresholds : false,
        )

        String expected = steps ? """\
        |${steps*.name().sort().join(',')}
        |
        |${SEQ_CENTER_COMMENT}
        |${SALUTATION}
        |${FAQ}""".stripMargin() : ""

        when:
        String result = service.buildNotificationDigest(cmd, null)

        then:
        expected == result

        where:
        testCase        | steps                                                                                                        | _
        "with steps"    | [Ticket.ProcessingStep.INSTALLATION, Ticket.ProcessingStep.ALIGNMENT, Ticket.ProcessingStep.SNV] | _
        "without steps" | []                                                                                                           | _
    }

    void "buildNotificationDigest, everything given and expected"() {
        given:
        setupData()
        NotificationDigestService service = setupMockedServiceForBuildNotificationDigestTest(Ticket.ProcessingStep.values() as List)

        FastqImportInstance fastqImportInstance = createFastqImportInstance(sequenceFiles: [createFastqFile()])
        NotificationCommand cmd = createNotificationCommand(
                ticket         : fastqImportInstance.ticket,
                fastqImportInstance: fastqImportInstance,
        )

        String expected = """\
        |${cmd.steps*.name().sort().join(',')}
        |
        |${SEQ_CENTER_COMMENT}
        |${SALUTATION}
        |${FAQ}""".stripMargin()

        when:
        String result = service.buildNotificationDigest(cmd, null)

        then:
        expected == result
    }
}
