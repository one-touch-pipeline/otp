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
package de.dkfz.tbi.otp.qcTrafficLight

import grails.gorm.transactions.Transactional
import grails.web.mapping.LinkGenerator
import groovy.transform.CompileDynamic
import org.springframework.beans.factory.annotation.Autowired

import de.dkfz.tbi.otp.ProjectSelectionService
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.notification.CreateNotificationTextService
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.tracking.Ticket
import de.dkfz.tbi.otp.tracking.TicketService
import de.dkfz.tbi.otp.utils.*

@CompileDynamic
@Transactional
class QcTrafficLightNotificationService {

    @Autowired
    LinkGenerator linkGenerator

    TicketService ticketService
    CreateNotificationTextService createNotificationTextService
    ProcessingOptionService processingOptionService
    UserProjectRoleService userProjectRoleService
    MailHelperService mailHelperService
    MessageSourceService messageSourceService

    private String createResultsAreWarnedSubject(AbstractBamFile bamFile, boolean toBeSent) {
        StringBuilder subject = new StringBuilder()
        if (toBeSent) {
            subject << 'TO BE SENT: '
        }

        Set<Ticket> tickets = ticketService.findAllTickets(bamFile.containedSeqTracks)
        String ticketNumber = tickets ? "${ticketService.getPrefixedTicketNumber(tickets.last())} " : ""

        List<IlseSubmission> ilseSubmissions = bamFile.containedSeqTracks*.ilseSubmission.findAll().unique()
        String ilse = ilseSubmissions ? "[S#${ilseSubmissions*.ilseNumber.sort().join(',')}] " : ""

        subject << messageSourceService.createMessage("notification.template.alignment.qcTrafficWarningSubject", [
                ticketNumber: ticketNumber,
                ilse        : ilse,
                bamFile     : bamFile,
        ])

        return subject.toString()
    }

    private String createResultsAreWarnedMessage(AbstractBamFile bamFile) {
        return messageSourceService.createMessage("notification.template.alignment.qcTrafficWarningMessage", [
                bamFile              : bamFile,
                link                 : getAlignmentQualityOverviewLink(bamFile.project, bamFile.seqType),
                emailSenderSalutation: processingOptionService.findOptionAsString(ProcessingOption.OptionName.HELP_DESK_TEAM_NAME),
                thresholdPage        : getThresholdPageLink(bamFile.project),
                faq                  : createNotificationTextService.faq,
        ])
    }

    void informResultsAreWarned(AbstractBamFile bamFile) {
        boolean projectNotification = bamFile.project.processingNotification
        boolean ticketNotification = ticketService.findAllTickets(bamFile.containedSeqTracks).find {
            !it.finalNotificationSent && it.automaticNotification
        } as boolean
        boolean shouldSendEmailToProjectReceiver = projectNotification && ticketNotification
        List<String> recipients = shouldSendEmailToProjectReceiver ? userProjectRoleService.getEmailsOfToBeNotifiedProjectUsers([bamFile.project]) : []
        String subject = createResultsAreWarnedSubject(bamFile, recipients.empty)
        String content = createResultsAreWarnedMessage(bamFile)

        if (recipients) {
            mailHelperService.sendEmail(subject, content, recipients)
        }
    }

    private String getThresholdPageLink(Project project) {
        return linkGenerator.link(
                controller: 'qcThreshold',
                action    : 'projectConfiguration',
                absolute  : true,
                params    : [(ProjectSelectionService.PROJECT_SELECTION_PARAMETER): project],
        )
    }

    private String getAlignmentQualityOverviewLink(Project project, SeqType seqType, Sample sample = null) {
        Map conditionalParams = [:]
        if (sample) {
            conditionalParams["sample"] = sample.id
        }
        return linkGenerator.link(
                controller: 'alignmentQualityOverview',
                action    : 'index',
                absolute  : true,
                params    : [
                        (ProjectSelectionService.PROJECT_SELECTION_PARAMETER): project,
                        seqType                                              : seqType.id,
                ] + conditionalParams
        )
    }
}
