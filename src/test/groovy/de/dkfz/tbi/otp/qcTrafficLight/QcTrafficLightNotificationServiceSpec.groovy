/*
 * Copyright 2011-2019 The OTP authors
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
package de.dkfz.tbi.otp.qcTrafficLight

import grails.testing.gorm.DataTest
import grails.web.mapping.LinkGenerator
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.Comment
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.rnaAlignment.RnaRoddyBamFile
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.domainFactory.pipelines.IsRoddy
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.notification.CreateNotificationTextService
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.tracking.OtrsTicket
import de.dkfz.tbi.otp.tracking.OtrsTicketService
import de.dkfz.tbi.otp.utils.MailHelperService
import de.dkfz.tbi.otp.utils.MessageSourceService

class QcTrafficLightNotificationServiceSpec extends Specification implements DataTest, IsRoddy {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                AbstractBamFile,
                Comment,
                DataFile,
                FastqImportInstance,
                FileType,
                Individual,
                LibraryPreparationKit,
                MergingCriteria,
                MergingWorkPackage,
                OtrsTicket,
                Pipeline,
                ProcessingOption,
                Project,
                Realm,
                ReferenceGenome,
                ReferenceGenomeProjectSeqType,
                RnaRoddyBamFile,
                RoddyBamFile,
                RoddyWorkflowConfig,
                Run,
                Sample,
                SampleType,
                SeqCenter,
                SeqPlatform,
                SeqPlatformGroup,
                SeqPlatformModelLabel,
                SeqTrack,
                SeqType,
                SoftwareTool,
        ]
    }

    @Unroll
    void "test informResultsAreWarned (#name)"() {
        given:
        final String HEADER = 'HEADER'
        final String BODY = 'BODY'
        final String LINK = 'LINK'
        final String FAQ = 'FAQ'
        final String TICKET_PREFIX = "prefix"

        AbstractBamFile bamFile = createBamFile()
        bamFile.project.qcTrafficLightNotification = qcTrafficLightNotification
        bamFile.project.save(flush: true)

        String emailSenderSalutation = DomainFactory.createProcessingOptionForEmailSenderSalutation().value
        DomainFactory.createProcessingOptionForOtrsTicketPrefix(TICKET_PREFIX)
        Set<OtrsTicket> otrsTickets = [
                createOtrsTicket([
                        finalNotificationSent: finalNotificationSent,
                        automaticNotification: automaticNotification,
                ]),
        ] as Set

        String ilseNumbers = ""
        if (createIlse) {
            IlseSubmission ilseSubmission = createIlseSubmission()
            bamFile.containedSeqTracks.first().ilseSubmission = ilseSubmission
            bamFile.save(flush: true)
            ilseNumbers = "[S#${ilseSubmission.ilseNumber}] "
        }

        String prefixedTicketNumber = "prefixedTicketNumber"
        QcTrafficLightNotificationService service = new QcTrafficLightNotificationService([
                processingOptionService: new ProcessingOptionService(),
                otrsTicketService      : Mock(OtrsTicketService) {
                    2 * findAllOtrsTickets(_) >> otrsTickets
                    1 * getPrefixedTicketNumber(_) >> prefixedTicketNumber
                },
        ])

        service.messageSourceService = Mock(MessageSourceService) {
            1 * createMessage('notification.template.alignment.qcTrafficWarningSubject', _) >> { String templateName, Map properties ->
                assert properties.size() == 3
                assert properties['bamFile'] == bamFile
                assert properties['ticketNumber'] == "${prefixedTicketNumber} "
                assert properties['ilse'] == ilseNumbers
                return HEADER
            }
            1 * createMessage('notification.template.alignment.qcTrafficWarningMessage', _) >> { String templateName, Map properties ->
                assert properties.size() == 5
                assert properties['bamFile'] == bamFile
                assert properties['emailSenderSalutation'] == emailSenderSalutation
                assert properties['link'] == LINK
                assert properties['thresholdPage'] == LINK
                assert properties['faq'] == FAQ
                return BODY
            }
            0 * _
        }

        service.createNotificationTextService = Mock(CreateNotificationTextService) {
            1 * getFaq() >> FAQ
        }

        service.userProjectRoleService = Mock(UserProjectRoleService) {
            (0..1) * getEmailsOfToBeNotifiedProjectUsers(_) >> emails
        }

        service.mailHelperService = Mock(MailHelperService) {
            if (emails && !finalNotificationSent && automaticNotification && qcTrafficLightNotification && !ilseNumbers) {
                1 * sendEmail(_, _, _) >> { String emailSubject, String content, List<String> receivers ->
                    assert emailSubject == subjectHeader + HEADER
                    assert content == BODY
                    assert receivers == emails
                }
            } else {
                1 * sendEmailToTicketSystem(_, _) >> { String emailSubject, String content ->
                    assert emailSubject == subjectHeader + HEADER
                    assert content == BODY
                }
            }
        }

        service.linkGenerator = Mock(LinkGenerator) {
            1 * link({ it.controller == 'qcThreshold' }) >> LINK
            1 * link({ it.controller == 'alignmentQualityOverview' }) >> LINK
        }

        expect:
        service.informResultsAreWarned(bamFile)

        where:
        name                                          | finalNotificationSent | automaticNotification | qcTrafficLightNotification | emails    | subjectHeader  | createIlse   || recipientsCount
        'without userProjectRole'                     | false                 | true                  | true                       | []        | 'TO BE SENT: ' | false        || 1
        'with userProjectRole'                        | false                 | true                  | true                       | ['email'] | ''             | false        || 2
        'ticket has already send final notification'  | true                  | true                  | true                       | ['email'] | 'TO BE SENT: ' | false        || 1
        'ticket has disabled automaticNotification'   | false                 | false                 | true                       | ['email'] | 'TO BE SENT: ' | false        || 1
        'project has disabled automatic notification' | false                 | true                  | false                      | ['email'] | 'TO BE SENT: ' | false        || 1
        'project with ilse numbers'                   | false                 | true                  | false                      | ['email'] | 'TO BE SENT: ' | true         || 1
    }
}
