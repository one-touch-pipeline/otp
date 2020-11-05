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

import grails.testing.gorm.DataTest
import spock.lang.Specification

import de.dkfz.tbi.otp.Comment
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.domainFactory.pipelines.AlignmentPipelineFactory
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.tracking.OtrsTicket
import de.dkfz.tbi.otp.tracking.PreparedNotification
import de.dkfz.tbi.otp.utils.MailHelperService
import de.dkfz.tbi.otp.workflowExecution.ProcessingPriority

class NotificationDigestServiceSpec extends Specification implements DomainFactoryCore, AlignmentPipelineFactory, DataTest {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                Comment,
                IlseSubmission,
                Individual,
                OtrsTicket,
                ProcessingOption,
                ProcessingPriority,
                Project,
                ProjectRole,
                Realm,
                Sample,
                SampleType,
                UserProjectRole,
        ]
    }

    void setupData() {
        DomainFactory.createProcessingOptionLazy(name: ProcessingOption.OptionName.TICKET_SYSTEM_NUMBER_PREFIX, value: "prefix")
        DomainFactory.createProcessingOptionLazy(name: ProcessingOption.OptionName.EMAIL_RECIPIENT_NOTIFICATION, value: "service@otp.de")
        DomainFactory.createProcessingOptionLazy(name: ProcessingOption.OptionName.EMAIL_SENDER_SALUTATION, value: "salutation")
    }

    void "buildNotificationSubject, properly build notification subject"() {
        given:
        setupData()
        NotificationDigestService service = new NotificationDigestService(
                ilseSubmissionService: new IlseSubmissionService(),
        )

        OtrsTicket ticket = createOtrsTicket()
        Project project = createProject()
        List<SeqTrack> seqTracks = [
            createSeqTrack(ilseSubmission: createIlseSubmission(ilseNumber: 1)),
            createSeqTrack(ilseSubmission: createIlseSubmission(ilseNumber: 2)),
        ]

        String expected = "[${ticket.prefixedTicketNumber}][S#1,2] - ${project} - OTP processing completed"

        when:
        String result = service.buildNotificationSubject(ticket, seqTracks, project)

        then:
        expected == result
    }

    NotificationDigestService setupMockedServiceForMailTests(Integer mailCalls) {
        return new NotificationDigestService(
                userProjectRoleService : new UserProjectRoleService(),
                processingOptionService: new ProcessingOptionService(),
                mailHelperService      : Mock(MailHelperService) {
                    mailCalls * sendEmail(_, _, _)
                },
        )
    }

    PreparedNotification createBasicPreparedNotification() {
        return new PreparedNotification(
                project                 : createProject(),
                seqTracks               : [],
                bams                    : [],
                toBeNotifiedProjectUsers: [
                        DomainFactory.createUserProjectRole(),
                        DomainFactory.createUserProjectRole(),
                ],
                processingStatus        : null,
                subject                 : "subject",
                notification            : "notification",
        )
    }

    void "sendPreparedNotification, signature with list, sends one mail for each"() {
        given:
        setupData()
        NotificationDigestService service = setupMockedServiceForMailTests(2)

        expect:
        service.sendPreparedNotification([
                createBasicPreparedNotification(),
                createBasicPreparedNotification(),
        ])
    }

    void "sendPreparedNotification, signature with field, does one call"() {
        given:
        setupData()
        NotificationDigestService service = setupMockedServiceForMailTests(1)

        expect:
        service.sendPreparedNotification(createBasicPreparedNotification())
    }

    void "sendPreparedNotification, properly handles notification without users to notify"() {
        given:
        setupData()
        NotificationDigestService service = setupMockedServiceForMailTests(1)

        PreparedNotification preparedNotification = createBasicPreparedNotification()
        preparedNotification.toBeNotifiedProjectUsers = []

        expect:
        service.sendPreparedNotification(preparedNotification)
    }
}
