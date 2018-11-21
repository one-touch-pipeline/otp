package de.dkfz.tbi.otp.qcTrafficLight


import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.notification.*
import de.dkfz.tbi.otp.tracking.*
import de.dkfz.tbi.otp.utils.*

class QcTrafficLightNotificationService {


    TrackingService trackingService

    CreateNotificationTextService createNotificationTextService

    ProcessingOptionService processingOptionService

    UserProjectRoleService userProjectRoleService

    MailHelperService mailHelperService


    private String createResultsAreBlockedSubject(AbstractMergedBamFile bamFile, boolean toBeSent) {
        StringBuilder subject = new StringBuilder()
        if (toBeSent) {
            subject << 'TO BE SENT: '
        }

        subject << createNotificationTextService.createMessage(
                "notification.template.alignment.qcTrafficBlockedSubject",
                [
                        bamFile: bamFile,
                ]
        )

        return subject.toString()
    }


    private String createResultsAreBlockedMessage(AbstractMergedBamFile bamFile) {
        return createNotificationTextService.createMessage(
                "notification.template.alignment.qcTrafficBlockedMessage",
                [
                        bamFile              : bamFile,
                        link                 : createNotificationTextService.createOtpLinks([bamFile.project], 'alignmentQualityOverview', 'index'),
                        emailSenderSalutation: processingOptionService.findOptionAsString(ProcessingOption.OptionName.EMAIL_SENDER_SALUTATION),
                ]
        )
    }

    void informResultsAreBlocked(AbstractMergedBamFile bamFile) {
        List<String> recipients = userProjectRoleService.getEmailsOfToBeNotifiedProjectUsers(bamFile.project)
        String subject = createResultsAreBlockedSubject(bamFile, recipients.empty)
        String content = createResultsAreBlockedMessage(bamFile)
        recipients << processingOptionService.findOptionAsString(ProcessingOption.OptionName.EMAIL_RECIPIENT_NOTIFICATION)
        mailHelperService.sendEmail(subject, content, recipients)
    }
}
