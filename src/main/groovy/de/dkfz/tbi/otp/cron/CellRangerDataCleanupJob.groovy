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
package de.dkfz.tbi.otp.cron

import groovy.transform.CompileDynamic
import groovy.transform.TupleConstructor
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.cellRanger.CellRangerConfigurationService
import de.dkfz.tbi.otp.dataprocessing.cellRanger.CellRangerMergingWorkPackage
import de.dkfz.tbi.otp.dataprocessing.singleCell.SingleCellBamFile
import de.dkfz.tbi.otp.ngsdata.UserProjectRoleService
import de.dkfz.tbi.otp.notification.CreateNotificationTextService
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.administration.MailHelperService
import de.dkfz.tbi.otp.utils.MessageSourceService
import de.dkfz.tbi.otp.utils.exceptions.InformationTypeException
import de.dkfz.tbi.util.TimeFormats

import java.sql.Timestamp
import java.time.LocalDate

@CompileDynamic
@Component
@Slf4j
class CellRangerDataCleanupJob extends AbstractScheduledJob {

    static final int DELETE_AFTER_WEEKS = 12

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
        deleteOfOldFailedWorkflows()
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

    /**
     * delete automatically cell ranger data not finished after reaching the reminder deadline
     */
    void deleteOfOldFailedWorkflows() {
        Date date = Timestamp.valueOf(LocalDate.now().minusWeeks(DELETE_AFTER_WEEKS).atStartOfDay())

        List<CellRangerMergingWorkPackage> mwps = (SingleCellBamFile.withCriteria {
            workPackage {
                eq("status", CellRangerMergingWorkPackage.Status.UNSET)
            }
            ne("fileOperationStatus", AbstractBamFile.FileOperationStatus.PROCESSED)
            lt("lastUpdated", date)
        } as List<SingleCellBamFile>)*.workPackage

        if (mwps) {
            cellRangerConfigurationService.deleteMwps(mwps)
            String subject = messageSourceService.createMessage("cellRanger.notification.failedDeletionInformation.subject", [:])
            String content = buildDeletionForFailedMergingWorkPackageMessageBody(mwps)
            mailHelperService.saveMail(subject, content)
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
        return (SingleCellBamFile.withCriteria {
            workPackage {
                eq("status", CellRangerMergingWorkPackage.Status.UNSET)
                isNull("informed")
                isNotNull('bamFileInProjectFolder')
            }
            eq("fileOperationStatus", AbstractBamFile.FileOperationStatus.PROCESSED)
            lt("lastUpdated", reminderDeadline)
        } as List<SingleCellBamFile>)*.workPackage
    }

    void cellRangerMWPsetDateOfInformed(List<CellRangerMergingWorkPackage> cellRangerMwps) {
        Date informedDate = Timestamp.valueOf(LocalDate.now().atStartOfDay())
        cellRangerMwps.each {
            cellRangerConfigurationService.setInformedFlag(it, informedDate)
        }
    }

    void sendNotificationEmail(Project project, List<CellRangerMergingWorkPackage> cellRangerMwps, InformationType informationType) {
        String subject = messageSourceService.createMessage("cellRanger.notification.${informationType}.subject", [project: project])
        String content
        switch (informationType) {
            case InformationType.DELETION:
                content = buildDeletionMessageBody(project, cellRangerMwps)
                break
            case InformationType.REMINDER:
                content = buildReminderMessageBody(project, cellRangerMwps)
                break
            default:
                throw new InformationTypeException("Invalid state ${informationType} for informationType")
        }
        mailHelperService.saveMail(subject, content, (userProjectRoleService.getEmailsOfToBeNotifiedProjectUsers([project]) +
                cellRangerMwps*.requester*.email).unique())
    }

    String buildDeletionForFailedMergingWorkPackageMessageBody(List<CellRangerMergingWorkPackage> cellRangerMwps) {
        return messageSourceService.createMessage("cellRanger.notification.failedDeletionInformation.body", [
                weeks           : DELETE_AFTER_WEEKS,
                formattedMwpList: getFormattedMwpList(cellRangerMwps),
        ])
    }

    String buildDeletionMessageBody(Project project, List<CellRangerMergingWorkPackage> cellRangerMwps) {
        return messageSourceService.createMessage("cellRanger.notification.deletionInformation.body", [
                project              : project.name,
                formattedMwpList     : getFormattedMwpList(cellRangerMwps),
                otpLinkUserManagement: getOtpLinksUserManagement(project),
        ])
    }

    String buildReminderMessageBody(Project project, List<CellRangerMergingWorkPackage> cellRangerMwps) {
        return messageSourceService.createMessage("cellRanger.notification.decisionReminder.body", [
                project              : project.name,
                plannedDeletionDate  : formattedPlannedDeletionDate,
                formattedMwpList     : getFormattedMwpList(cellRangerMwps),
                otpLinkCellRanger    : getOtpLinksCellRanger(project),
                otpLinkUserManagement: getOtpLinksUserManagement(project),
        ])
    }

    static String getFormattedMwpList(List<CellRangerMergingWorkPackage> mwps) {
        return mwps.collect { "  - ${it.toStringWithoutIdAndPipeline()}" }.sort().join("\n")
    }

    String getFormattedPlannedDeletionDate() {
        return TimeFormats.DATE.getFormattedDate(plannedDeletionDate)
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
            isNotNull('bamFileInProjectFolder')
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
