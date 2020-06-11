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
package de.dkfz.tbi.otp.cron

import grails.util.Pair
import groovy.transform.TupleConstructor
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.OtpException
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.dataprocessing.cellRanger.CellRangerConfigurationService
import de.dkfz.tbi.otp.dataprocessing.cellRanger.CellRangerMergingWorkPackage
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.ngsdata.UserProjectRoleService
import de.dkfz.tbi.otp.notification.CreateNotificationTextService
import de.dkfz.tbi.otp.utils.MailHelperService
import de.dkfz.tbi.otp.utils.MessageSourceService

import java.sql.Timestamp
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.time.LocalDate

@SuppressWarnings("AnnotationsForJobs")
@Scope("singleton")
@Component
@Slf4j
class CellRangerDataCleanupJob extends ScheduledJob {

    @Autowired
    UserProjectRoleService userProjectRoleService

    @Autowired
    MessageSourceService messageSourceService

    @Autowired
    CreateNotificationTextService createNotificationTextService

    @Autowired
    CellRangerConfigurationService cellRangerConfigurationService

    @Autowired
    ProcessingOptionService processingOptionService

    @Autowired
    MailHelperService mailHelperService

    @Override
    void wrappedExecute() {
        checkAndNotifyUncategorisedResults()
        notifyAndDeletion()
    }

    @TupleConstructor
    enum InformationType {
        DELETION("deletionInformation"),
        REMINDER("decisionReminder"),

        final String templateName

        InformationType(String templateName) {
            this.templateName = templateName
        }

        @Override
        String toString() {
            return templateName
        }
    }

    @TupleConstructor
    enum InformationRecipientType {
        AUTHORITIES("authorities"),
        BIOINFOMRATICIAN("bioinformatician"),

        final String recipientType

        InformationRecipientType(String recipientType) {
            this.recipientType = recipientType
        }

        @Override
        String toString() {
            return recipientType
        }
    }

    void notifyAndDeletion() {
        resultsToDelete.groupBy { CellRangerMergingWorkPackage mwp ->
            mwp.project
        }.each { Project project, List<CellRangerMergingWorkPackage> cellRangerMwps ->
            sendNotificationEmail(project, cellRangerMwps, InformationType.DELETION)
            deleteTooOldResults(cellRangerMwps)
        }
    }

    void checkAndNotifyUncategorisedResults() {
        resultsNeedingReminder.groupBy { CellRangerMergingWorkPackage mwp ->
            mwp.project
        }.each { Project project, List<CellRangerMergingWorkPackage> cellRangerMwps ->
            sendNotificationEmail(project, cellRangerMwps, InformationType.REMINDER)
            cellRangerMWPsetDateOfInformed(cellRangerMwps)
        }
    }

    List<CellRangerMergingWorkPackage> getResultsNeedingReminder() {
        return CellRangerMergingWorkPackage.withCriteria {
            eq("status", CellRangerMergingWorkPackage.Status.UNSET)
            lt("dateCreated", reminderDeadline)
            isNull("informed")
        } as List<CellRangerMergingWorkPackage>
    }

    void cellRangerMWPsetDateOfInformed(List<CellRangerMergingWorkPackage> cellRangerMwps) {
        Date informedDate = Timestamp.valueOf(LocalDate.now().atStartOfDay())
        cellRangerMwps.each {
            cellRangerConfigurationService.setInformedFlag(it, informedDate)
        }
    }

    private Pair<List<String>, InformationRecipientType> getRecipientsAndTemplate(Project project) {
        List<String> recipients = UserProjectRoleService.getBioinformaticianUsers(project)*.email
        InformationRecipientType recipientType = InformationRecipientType.BIOINFOMRATICIAN
        if (recipients.size() == 0) {
            recipients = UserProjectRoleService.getProjectAuthorities(project)*.email
            recipientType = InformationRecipientType.AUTHORITIES
        }
        recipients += processingOptionService.findOptionAsList(ProcessingOption.OptionName.EMAIL_RECIPIENT_NOTIFICATION)
        return new Pair<List<String>, InformationRecipientType>(recipients, recipientType)
    }

    void sendNotificationEmail(Project project, List<CellRangerMergingWorkPackage> cellRangerMwps, InformationType informationType) {
        Pair<List<String>, InformationRecipientType> recipientsAndTemplate = getRecipientsAndTemplate(project)
        InformationRecipientType recipientType = recipientsAndTemplate.getbValue()

        String subject = messageSourceService.createMessage("cellRanger.notification.${recipientType}.${informationType}.subject", [project: project])
        String content
        switch (informationType) {
            case InformationType.DELETION:
                content = buildDeletionMessageBody(project, cellRangerMwps, recipientType)
                break
            case InformationType.REMINDER:
                content = buildReminderMessageBody(project, cellRangerMwps, recipientType)
                break
            default:
                throw new OtpException("Invalid state ${informationType} for informationType")
        }

        mailHelperService.sendEmail(subject, content, (recipientsAndTemplate.getaValue() + cellRangerMwps*.requester*.email).unique())
    }

    String buildDeletionMessageBody(Project project, List<CellRangerMergingWorkPackage> cellRangerMwps, InformationRecipientType recipientType) {
        return messageSourceService.createMessage("cellRanger.notification.${recipientType}.deletionInformation.body", [
                "project"               : project.name,
                "formattedMwpList"      : getFormattedMwpList(cellRangerMwps),
                "otpLinkUserManagement" : getOtpLinksUserManagement(project),
        ])
    }

    String buildReminderMessageBody(Project project, List<CellRangerMergingWorkPackage> cellRangerMwps, InformationRecipientType recipientType) {
        return messageSourceService.createMessage("cellRanger.notification.${recipientType}.decisionReminder.body", [
                "project"               : project.name,
                "plannedDeletionDate"   : formattedPlannedDeletionDate,
                "formattedMwpList"      : getFormattedMwpList(cellRangerMwps),
                "otpLinkCellRanger"     : getOtpLinksCellRanger(project),
                "otpLinkUserManagement" : getOtpLinksUserManagement(project),
        ])
    }

    static String getFormattedMwpList(List<CellRangerMergingWorkPackage> mwps) {
        return mwps.collect { "  - ${it.toStringWithoutIdAndPipeline()}" }.sort().join("\n")
    }

    String getFormattedPlannedDeletionDate() {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd" , Locale.ENGLISH)
        return dateFormat.format(plannedDeletionDate)
    }

    Date getPlannedDeletionDate() {
        int weeksToDeletion = processingOptionService.findOptionAsInteger(ProcessingOption.OptionName.CELLRANGER_CLEANUP_WEEKS_TILL_DELETION_AFTER_REMINDER)
        return Timestamp.valueOf(LocalDate.now().plusWeeks(weeksToDeletion).atStartOfDay())
    }

    String getOtpLinksUserManagement(Project project) {
        return createNotificationTextService.createOtpLinks([project], 'projectUser', 'index')
    }

    String getOtpLinksCellRanger(Project project) {
        return createNotificationTextService.createOtpLinks([project], 'cellRanger', 'finalRunSelection')
    }

    void deleteTooOldResults(List<CellRangerMergingWorkPackage> cellRangerMwps) {
        if (cellRangerMwps) {
            cellRangerConfigurationService.deleteMwps(cellRangerMwps)
        }
    }

    List<CellRangerMergingWorkPackage> getResultsToDelete() {
        return CellRangerMergingWorkPackage.createCriteria().list {
            eq("status", CellRangerMergingWorkPackage.Status.UNSET)
            lt("informed", deletionDeadline)
            isNotNull("informed")
        } as List<CellRangerMergingWorkPackage>
    }

    /**
     * Gets the cutoff date when users should be informed about this dataset.
     *
     * Calculates the date (in the past), based on the configured period for reminder.
     * The date is calculated relative to today.
     *
     * @see ProcessingOption.OptionName#CELLRANGER_CLEANUP_WEEKS_TILL_REMINDER
     */
    Date getReminderDeadline() {
        int weeksToNotification = processingOptionService.findOptionAsInteger(ProcessingOption.OptionName.CELLRANGER_CLEANUP_WEEKS_TILL_REMINDER)
        return Timestamp.valueOf(LocalDate.now().minusWeeks(weeksToNotification).atStartOfDay())
    }

    /**
     * Gets the cutoff date for when data is old enough to be deleted.
     *
     * Calculates the date (in the past), where a CellRangerMergingWorkPakage can be deleted.
     * The date is calculated relative to today.
     *
     * @see ProcessingOption.OptionName#CELLRANGER_CLEANUP_WEEKS_TILL_DELETION_AFTER_REMINDER
     */
    Date getDeletionDeadline() {
        int weeksToDeletion = processingOptionService.findOptionAsInteger(ProcessingOption.OptionName.CELLRANGER_CLEANUP_WEEKS_TILL_DELETION_AFTER_REMINDER)
        return Timestamp.valueOf(LocalDate.now().minusDays(weeksToDeletion * 7).atStartOfDay())
    }
}
