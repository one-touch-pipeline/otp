/*
 * Copyright 2011-2020 The OTP authors
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
import grails.transaction.Rollback
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
import de.dkfz.tbi.otp.utils.MailHelperService
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
        DomainFactory.createProcessingOptionLazy(name: ProcessingOption.OptionName.EMAIL_SENDER_SALUTATION, value: SALUTATION)
    }

    NotificationCommand createNotificationCommand(Map properties = [:]) {
        return new NotificationCommand([
                otrsTicket         : properties.otrsTicket ?: createOtrsTicket(),
                fastqImportInstance: properties.fastqImportInstance ?: createFastqImportInstance(),
                steps              : properties.steps ?: OtrsTicket.ProcessingStep.values() - OtrsTicket.ProcessingStep.FASTQC,
                notifyQcThresholds : properties.notifyQcThresholds ?: true,
        ] + properties)
    }

    Map<String, Object> setupNotificationCommand() {
        OtrsTicket otrsTicket = createOtrsTicket()

        SeqTrack seqTrack1 = createSeqTrackWithOneDataFile()
        SeqTrack seqTrack2 = createSeqTrackWithOneDataFile()
        List<DataFile> dataFiles1 = ([seqTrack1, seqTrack2]*.dataFiles).flatten()
        FastqImportInstance fastqImportInstance1 = createFastqImportInstance(otrsTicket: otrsTicket, dataFiles: dataFiles1)
        dataFiles1.each {
            it.fastqImportInstance = fastqImportInstance1
            it.save(flush: true)
        }

        SeqTrack seqTrack3 = createSeqTrackWithOneDataFile()
        List<DataFile> dataFiles2 = ([seqTrack3]*.dataFiles).flatten()
        FastqImportInstance fastqImportInstance2 = createFastqImportInstance(otrsTicket: otrsTicket, dataFiles: dataFiles2)
        dataFiles2.each {
            it.fastqImportInstance = fastqImportInstance2
            it.save(flush: true)
        }

        List<OtrsTicket.ProcessingStep> processingSteps = [
                OtrsTicket.ProcessingStep.INSTALLATION,
                OtrsTicket.ProcessingStep.ALIGNMENT,
                OtrsTicket.ProcessingStep.SNV,
        ]

        NotificationCommand cmd = createNotificationCommand(
                otrsTicket         : otrsTicket,
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

    NotificationDigestService setupMockedServiceForPrepareNotificationsTests(Integer buildCalls, Integer sendCalls, List<AbstractMergedBamFile> bams = [], List<UserProjectRole> projectUsers = []) {
        return new NotificationDigestService(
                abstractMergedBamFileService: Mock(AbstractMergedBamFileService) {
                    buildCalls * getActiveBlockedBamsContainingSeqTracks(_) >> bams
                },
                createNotificationTextService: Mock(CreateNotificationTextService),
                ilseSubmissionService: Mock(IlseSubmissionService),
                notificationCreator: new NotificationCreator(),
                messageSourceService: Mock(MessageSourceService),
                userProjectRoleService: Mock(UserProjectRoleService) {
                    buildCalls * getProjectUsersToBeNotified(_) >> projectUsers
                    sendCalls * getMails(_) >> ["mail1", "mail2"]
                },
                processingOptionService: new ProcessingOptionService(),
                mailHelperService: Mock(MailHelperService) {
                    sendCalls * sendEmail(_, _, _)
                },
                qcTrafficLightNotificationService: Mock(QcTrafficLightNotificationService),
        )
    }

    void "prepareNotifications, creates one notification per project"() {
        given:
        setupData()
        Map<String, Object> map = setupNotificationCommand()

        RoddyBamFile bam = DomainFactory.createRoddyBamFile()
        UserProjectRole projectUser = DomainFactory.createUserProjectRole()

        NotificationDigestService service = setupMockedServiceForPrepareNotificationsTests(3, 0, [bam], [projectUser])

        when:
        List<PreparedNotification> preparedNotifications = service.prepareNotifications(map.cmd as NotificationCommand)

        then:
        preparedNotifications.size() == 3
        TestCase.assertContainSame(preparedNotifications*.project, map.projects as List<Project>)
        TestCase.assertContainSame(preparedNotifications*.seqTracks.flatten(), map.seqTracks as List<SeqTrack>)
        preparedNotifications*.bams.flatten().unique() == [bam]
        preparedNotifications*.toBeNotifiedProjectUsers.flatten().unique() == [projectUser]
    }

    void "prepareAndSendNotifications, preparesNotifications and sends each"() {
        given:
        setupData()
        Map<String, Object> map = setupNotificationCommand()

        RoddyBamFile bam = DomainFactory.createRoddyBamFile()
        UserProjectRole projectUser = DomainFactory.createUserProjectRole()

        NotificationDigestService service = setupMockedServiceForPrepareNotificationsTests(3, 3, [bam], [projectUser])

        when:
        List<PreparedNotification> preparedNotifications = service.prepareAndSendNotifications(map.cmd as NotificationCommand)

        then:
        preparedNotifications.size() == 3
        TestCase.assertContainSame(preparedNotifications*.project, map.projects as List<Project>)
        TestCase.assertContainSame(preparedNotifications*.seqTracks.flatten(), map.seqTracks as List<SeqTrack>)
        preparedNotifications*.bams.flatten().unique() == [bam]
        preparedNotifications*.toBeNotifiedProjectUsers.flatten().unique() == [projectUser]
    }

    NotificationDigestService setupMockedServiceForBuildNotificationDigestTest(List<OtrsTicket.ProcessingStep> finishedSteps = []) {
        Map<OtrsTicket.ProcessingStep, Boolean> stepsToNotify = OtrsTicket.ProcessingStep.values().collectEntries { OtrsTicket.ProcessingStep step ->
            [(step): (step in finishedSteps)]
        }
        return new NotificationDigestService(
                createNotificationTextService    : Mock(CreateNotificationTextService) {
                    _ * buildStepNotifications(_, _) >> { List<OtrsTicket.ProcessingStep> processingSteps, ProcessingStatus status ->
                        return processingSteps.findAll { stepsToNotify[it] }*.name().sort().join(",")
                    }
                    _ * buildSeqCenterComment(_) >> { return SEQ_CENTER_COMMENT }
                    _ * getFaq() >> { return FAQ }
                },
                qcTrafficLightNotificationService: Mock(QcTrafficLightNotificationService) {
                    _ * buildContentForMultipleBamsWarningMessage(_) >> { List<AbstractMergedBamFile> bams ->
                        // some bug (?) causes bams to be a List<List<AbstractMergedBamFile>>, so I flatten it here
                        return bams.flatten()*.id.join(",")
                    }
                },
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

        List<OtrsTicket.ProcessingStep> steps = [
                OtrsTicket.ProcessingStep.INSTALLATION,
                OtrsTicket.ProcessingStep.ALIGNMENT,
                OtrsTicket.ProcessingStep.SNV,
        ]
        NotificationDigestService service = setupMockedServiceForBuildNotificationDigestTest(stepsHaveResult ? steps : [])

        NotificationCommand cmd = createNotificationCommand(
                steps             : notifySteps ? steps : [],
                notifyQcThresholds: notifyQc,
        )
        List<RoddyBamFile> bams = [
                createRoddyBamFile(RoddyBamFile),
                createRoddyBamFile(RoddyBamFile),
        ]

        expect:
        service.buildNotificationDigest(cmd, null, provideBams ? bams : []) == ""

        where:
        notifySteps | stepsHaveResult | notifyQc | provideBams | testCase
        false       | true            | false    | false       | "no steps, no QC"
        true        | false           | false    | false       | "steps, but no results, no qc"
        false       | true            | true     | false       | "no steps, QC, but no bams"
        false       | true            | false    | true        | "no steps, no QC, but bams"
    }

    @Unroll
    void "buildNotificationDigest, only adds SeqCenter comment when the provided steps have something to notify, #steps"() {
        given:
        setupData()

        NotificationDigestService service = setupMockedServiceForBuildNotificationDigestTest(steps as List<OtrsTicket.ProcessingStep>)

        FastqImportInstance fastqImportInstance = createFastqImportInstance(dataFiles: [createDataFile()])
        NotificationCommand cmd = createNotificationCommand(
                otrsTicket         : fastqImportInstance.otrsTicket,
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
        String result = service.buildNotificationDigest(cmd, null, [])

        then:
        expected == result

        where:
        testCase        | steps                                                                                                        | _
        "with steps"    | [OtrsTicket.ProcessingStep.INSTALLATION, OtrsTicket.ProcessingStep.ALIGNMENT, OtrsTicket.ProcessingStep.SNV] | _
        "without steps" | []                                                                                                           | _
    }

    void "buildNotificationDigest, everything given and expected"() {
        given:
        setupData()
        NotificationDigestService service = setupMockedServiceForBuildNotificationDigestTest(OtrsTicket.ProcessingStep.values() as List)

        FastqImportInstance fastqImportInstance = createFastqImportInstance(dataFiles: [createDataFile()])
        NotificationCommand cmd = createNotificationCommand(
                otrsTicket         : fastqImportInstance.otrsTicket,
                fastqImportInstance: fastqImportInstance,
        )

        List<AbstractMergedBamFile> bams = [
                createRoddyBamFile(RoddyBamFile),
                createRoddyBamFile(RoddyBamFile),
        ]

        String expected = """\
        |${cmd.steps*.name().sort().join(',')}
        |
        |${SEQ_CENTER_COMMENT}
        |
        |${(bams*.id).join(",")}
        |${SALUTATION}
        |${FAQ}""".stripMargin()

        when:
        String result = service.buildNotificationDigest(cmd, null, bams)

        then:
        expected == result
    }
}
