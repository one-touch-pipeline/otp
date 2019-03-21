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
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.Comment
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.rnaAlignment.RnaRoddyBamFile
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.notification.CreateNotificationTextService
import de.dkfz.tbi.otp.tracking.OtrsTicket
import de.dkfz.tbi.otp.tracking.TrackingService
import de.dkfz.tbi.otp.utils.MailHelperService

class QcTrafficLightNotificationServiceSpec extends Specification implements DataTest {

    Class[] getDomainClassesToMock() {[
            AbstractMergedBamFile,
            DataFile,
            Comment,
            ExomeSeqTrack,
            FileType,
            Individual,
            LibraryPreparationKit,
            MergingCriteria,
            MergingWorkPackage,
            Pipeline,
            Project,
            ProcessingOption,
            OtrsTicket,
            Realm,
            ReferenceGenome,
            ReferenceGenomeProjectSeqType,
            RnaRoddyBamFile,
            RoddyBamFile,
            RoddyWorkflowConfig,
            Run,
            RunSegment,
            Sample,
            SampleType,
            SeqType,
            SeqPlatform,
            SeqPlatformGroup,
            SeqPlatformModelLabel,
            SeqCenter,
            SeqTrack,
            SoftwareTool,
    ]}

    @Unroll
    void "test informResultsAreBlocked (#name)"() {
        given:
        final String HEADER = 'HEADER'
        final String BODY = 'BODY'
        final String LINK = 'LINK'

        AbstractMergedBamFile bamFile = DomainFactory.createRoddyBamFile([:])
        String emailSenderSalutation = DomainFactory.createProcessingOptionForEmailSenderSalutation().value
        DomainFactory.createProcessingOptionForNotificationRecipient()
        Set<OtrsTicket> otrsTickets = [
                DomainFactory.createOtrsTicket([
                        finalNotificationSent: finalNotificationSent,
                        automaticNotification: automaticNotification,
                ]),
        ] as Set

        QcTrafficLightNotificationService service = new QcTrafficLightNotificationService([
                processingOptionService: new ProcessingOptionService(),
                trackingService        : Mock(TrackingService) {
                    1 * findAllOtrsTickets(_) >> otrsTickets
                },
        ])

        service.createNotificationTextService = Mock(CreateNotificationTextService) {
            1 * createMessage('notification.template.alignment.qcTrafficBlockedSubject', _) >> { String templateName, Map properties ->
                assert properties.size() == 1
                assert properties['bamFile'] == bamFile
                return HEADER

            }
            1 * createMessage('notification.template.alignment.qcTrafficBlockedMessage', _) >> { String templateName, Map properties ->
                assert properties.size() == 3
                assert properties['bamFile'] == bamFile
                assert properties['emailSenderSalutation'] == emailSenderSalutation
                assert properties['link'] == LINK
                return BODY
            }
            1 * createOtpLinks([bamFile.project], 'alignmentQualityOverview', 'index') >> LINK
            0 * _
        }

        service.userProjectRoleService = Mock(UserProjectRoleService) {
            (0..1) * getEmailsOfToBeNotifiedProjectUsers(_) >> emails
        }

        service.mailHelperService = Mock(MailHelperService) {
            1 * sendEmail(_, _, _) >> { String emailSubject, String content, List<String> recipients ->
                assert emailSubject == subjectHeader + HEADER
                assert content == BODY
                assert recipients
                assert recipients.size() == recipientsCount
                assert !recipients.contains(null)
            }
        }

        expect:
        service.informResultsAreBlocked(bamFile)

        where:
        name                                         | finalNotificationSent | automaticNotification | emails    | subjectHeader  || recipientsCount
        'without userProjectRole'                    | false                 | true                  | []        | 'TO BE SENT: ' || 1
        'with userProjectRole'                       | false                 | true                  | ['email'] | ''             || 2
        'ticket has already send final notification' | true                  | true                  | ['email'] | 'TO BE SENT: ' || 1
        'ticket has disabled automaticNotification'  | false                 | false                 | ['email'] | 'TO BE SENT: ' || 1
    }
}
