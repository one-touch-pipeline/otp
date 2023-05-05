/*
 * Copyright 2011-2021 The OTP authors
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

import grails.testing.mixin.integration.Integration
import grails.gorm.transactions.Rollback
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.cellRanger.CellRangerConfigurationService
import de.dkfz.tbi.otp.dataprocessing.cellRanger.CellRangerMergingWorkPackage
import de.dkfz.tbi.otp.dataprocessing.singleCell.SingleCellBamFile
import de.dkfz.tbi.otp.domainFactory.UserDomainFactory
import de.dkfz.tbi.otp.domainFactory.pipelines.cellRanger.CellRangerFactory
import de.dkfz.tbi.otp.ngsdata.Sample
import de.dkfz.tbi.otp.ngsdata.UserProjectRoleService
import de.dkfz.tbi.otp.notification.CreateNotificationTextService
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.security.User
import de.dkfz.tbi.otp.security.UserAndRoles
import de.dkfz.tbi.otp.utils.*

import java.time.LocalDate
import java.time.ZoneId

import static de.dkfz.tbi.otp.dataprocessing.cellRanger.CellRangerMergingWorkPackage.Status.*

@Rollback
@Integration
class CellRangerDataCleanupJobIntegrationSpec extends Specification implements CellRangerFactory, UserAndRoles, UserDomainFactory {

    static final int REMINDER_WEEKS = 12
    static final int REMINDER_DAYS = REMINDER_WEEKS * 7
    static final int DELETION_WEEKS = 4
    static final int DELETION_DAYS = DELETION_WEEKS * 7

    void setupData() {
        createAllBasicProjectRoles()

        findOrCreateProcessingOption(
                name: ProcessingOption.OptionName.CELLRANGER_CLEANUP_WEEKS_TILL_REMINDER,
                value: REMINDER_WEEKS,
        )
        findOrCreateProcessingOption(
                name: ProcessingOption.OptionName.CELLRANGER_CLEANUP_WEEKS_TILL_DELETION_AFTER_REMINDER,
                value: DELETION_WEEKS,
        )
    }

    CellRangerMergingWorkPackage createCellRangerMwpHelper(CellRangerMergingWorkPackage.Status status, LocalDate baseDate, Integer informedOffset,
                                                           Integer dateUpdatedOffset, boolean withFinishedBamFile, Map properties = [:]) {
        Closure<Date> getBaseDateMinusDays = { Integer offset ->
            return Date.from(baseDate.minusDays(offset).atStartOfDay(ZoneId.systemDefault()).toInstant())
        }

        CellRangerMergingWorkPackage mwp = createMergingWorkPackage([status: status] + properties)
        if (informedOffset) {
            mwp.informed = getBaseDateMinusDays(informedOffset)
        }

        SingleCellBamFile bamFile = createBamFile([
                workPackage        : mwp,
                fileOperationStatus: withFinishedBamFile ? AbstractBamFile.FileOperationStatus.PROCESSED : AbstractBamFile.FileOperationStatus.NEEDS_PROCESSING,
        ])
        mwp.save(flush: true)

        if (dateUpdatedOffset) {
            SingleCellBamFile.executeUpdate("update SingleCellBamFile set lastUpdated = :lastUpdated, version = version + 1 where id = :id", [
                    lastUpdated: getBaseDateMinusDays(dateUpdatedOffset),
                    id         : bamFile.id,
            ])
            bamFile.refresh()
        }

        return mwp
    }

    @Unroll
    void "sendNotificationEmail, Type #type, the email should only be sent to requester, the ticket system and users with receivesNotifications='true'"() {
        given:
        setupData()

        User user = createUser()
        User user2 = createUser()
        User requester = createUser()
        User requester2 = createUser()

        CellRangerDataCleanupJob cellRangerDataCleanupJob = new CellRangerDataCleanupJob([
                processingOptionService      : new ProcessingOptionService(),
                messageSourceService         : Stub(MessageSourceService) {
                    _ * createMessage("cellRanger.notification.${type.templateName}.subject", _ as Map) >> "${type.templateName} subject"
                    _ * createMessage("cellRanger.notification.${type.templateName}.body", _ as Map) >> "${type.templateName} body"
                },
                mailHelperService            : Mock(MailHelperService),
                createNotificationTextService: Mock(CreateNotificationTextService),
                userProjectRoleService       : Stub(UserProjectRoleService) {
                    getEmailsOfToBeNotifiedProjectUsers(_) >> [user.email, requester.email]
                },
        ])

        Closure<CellRangerMergingWorkPackage> createMwpForProjectAsUser = { Project project, User req ->
            Sample sample = createSample(
                    individual: createIndividual(
                            project: project ?: createProject()
                    )
            )
            return createMergingWorkPackage(sample: sample, requester: req)
        }

        Project project1 = createProject()
        CellRangerMergingWorkPackage crmwp1 = createMwpForProjectAsUser(project1, requester)
        CellRangerMergingWorkPackage crmwp2 = createMwpForProjectAsUser(project1, requester2)

        createUserProjectRole(
                project: project1,
                user: user,
                receivesNotifications: true,
        )
        createUserProjectRole(
                project: project1,
                user: user2,
                receivesNotifications: false,
        )
        createUserProjectRole(
                project: project1,
                user: requester,
                receivesNotifications: true,
        )
        createUserProjectRole(
                project: project1,
                user: requester2,
                receivesNotifications: false,
        )

        when:
        cellRangerDataCleanupJob.sendNotificationEmail(project1, [crmwp1, crmwp2], type as CellRangerDataCleanupJob.InformationType)

        then:
        1 * cellRangerDataCleanupJob.mailHelperService.sendEmail(
                "${type.templateName} subject",
                "${type.templateName} body",
                [user.email, requester.email, requester2.email]
        )

        where:
        type << CellRangerDataCleanupJob.InformationType.findAll()
    }

    void "getResultsToDelete, only deletes too old mwps"() {
        given:
        setupData()

        CellRangerDataCleanupJob cellRangerDataCleanupJob = new CellRangerDataCleanupJob(
                processingOptionService: new ProcessingOptionService(),
        )
        LocalDate baseDate = LocalDate.now()

        List<CellRangerMergingWorkPackage> mwps = [
                [UNSET, 1, 1, true], // expected, informed and deletion date passed
                [UNSET, -1, 1, true], // informed but before reminderDeadline
                [UNSET, null, 1, true], // to be deleted but not informed yet
                [UNSET, null, -1, true], // not to be deleted and not informed yet
                [FINAL, 1, 1, true], // should not be found since status is FINAL
                [DELETED, 1, 1, true], // should not be found since status is DELETED
                [FINAL, -1, 1, true],
                [FINAL, null, 1, true],
                [FINAL, null, -1, true],
                [DELETED, null, 1, true],
                [DELETED, null, -1, true],
                [UNSET, 1, 1, false], // not expected, since workflow has not run through
        ].collect { List it ->
            createCellRangerMwpHelper(
                    it[0] as CellRangerMergingWorkPackage.Status,
                    baseDate,
                    it[1] ? DELETION_DAYS + (it[1] as Integer) : null,
                    it[2] ? DELETION_DAYS + REMINDER_DAYS + (it[2] as Integer) : null,
                    it[3],
            )
        }

        expect: "Finds only old unfinal informed items"
        CollectionUtils.containSame(cellRangerDataCleanupJob.resultsToDelete, [mwps[0]])
    }

    void "getResultsNeedingReminder, only finds undecided mwps older than the deadline"() {
        given:
        setupData()

        CellRangerDataCleanupJob cellRangerDataCleanupJob = new CellRangerDataCleanupJob(
                processingOptionService: new ProcessingOptionService(),
        )
        LocalDate baseDate = LocalDate.now()

        List<CellRangerMergingWorkPackage> mwps = [
                [UNSET, null, 1, true], // expected, to be reminded
                [UNSET, null, -1, true], // reminder time not yet met
                [FINAL, null, 1, true], // to be reminded but already FINAL
                [DELETED, null, 1, true], // to be reminded but already DELETED
                [UNSET, 1, 1, true], // already reminded
                [FINAL, null, -1, true],
                [DELETED, null, -1, true],
                [UNSET, null, 1, false],
                [UNSET, null, -1, false],
        ].collect { List it ->
            createCellRangerMwpHelper(
                    it[0] as CellRangerMergingWorkPackage.Status,
                    baseDate,
                    it[1] as Integer,
                    it[2] ? REMINDER_DAYS + (it[2] as Integer) : null,
                    it[3],
            )
        }

        expect: "Finds only old unfinal items"
        CollectionUtils.containSame(cellRangerDataCleanupJob.resultsNeedingReminder, [mwps[0]])
    }

    void "buildReminderMessageBody, checking if the service call is correct"() {
        given:
        setupData()

        CellRangerDataCleanupJob cellRangerDataCleanupJob = new CellRangerDataCleanupJob([
                processingOptionService      : new ProcessingOptionService(),
                createNotificationTextService: Mock(CreateNotificationTextService),
                messageSourceService         : Mock(MessageSourceService),
        ])
        LocalDate baseDate = LocalDate.now()

        Closure<CellRangerMergingWorkPackage> createMwpToBeNotifiedForProject = { Project project ->
            Sample sample = createSample(
                    individual: createIndividual(
                            project: project ?: createProject()
                    )
            )
            return createCellRangerMwpHelper(UNSET, baseDate, null, REMINDER_DAYS + 1, true, [sample: sample])
        }

        Project project = createProject()
        List<CellRangerMergingWorkPackage> cellRangerMergingWorkPackages = [
                createMwpToBeNotifiedForProject(project),
                createMwpToBeNotifiedForProject(project),
        ]

        when: "the email body is build"
        cellRangerDataCleanupJob.buildReminderMessageBody(project, cellRangerMergingWorkPackages)

        then: "message creation is called with the correct parameters"
        1 * cellRangerDataCleanupJob.messageSourceService.createMessage("cellRanger.notification.decisionReminder.body", [
                project              : project.name,
                plannedDeletionDate  : cellRangerDataCleanupJob.formattedPlannedDeletionDate,
                formattedMwpList     : CellRangerDataCleanupJob.getFormattedMwpList(cellRangerMergingWorkPackages),
                otpLinkCellRanger    : '[link to CellRanger]',
                otpLinkUserManagement: '[link to UserManagement]',
        ])

        and: "includes the links to the action pages"
        1 * cellRangerDataCleanupJob.createNotificationTextService.createOtpLinks([project], 'cellRanger', 'finalRunSelection') >> '[link to CellRanger]'
        1 * cellRangerDataCleanupJob.createNotificationTextService.createOtpLinks([project], 'projectUser', 'index') >> '[link to UserManagement]'
    }

    void "deleteOfOldFailedWorkflows, call deleteMwps for old not finished mwp and send mail"() {
        given:
        setupData()
        String header = "header ${nextId}"
        String body = "body ${nextId}"

        CellRangerDataCleanupJob cellRangerDataCleanupJob = new CellRangerDataCleanupJob([
                processingOptionService       : new ProcessingOptionService(),
                cellRangerConfigurationService: Mock(CellRangerConfigurationService),
                messageSourceService          : Mock(MessageSourceService),
                createNotificationTextService : Mock(CreateNotificationTextService),
                mailHelperService             : Mock(MailHelperService),
                userProjectRoleService        : new UserProjectRoleService(),
        ])
        LocalDate baseDate = LocalDate.now()

        List<CellRangerMergingWorkPackage> expectedMwps = [
                [UNSET, 1, 1, false],
                [UNSET, null, 1, false],
                [UNSET, null, -1, false],
                [UNSET, 1, 1, true],
                [UNSET, 1, 1, true],
                [UNSET, -1, 1, true],
                [UNSET, 1, -1, true],
                [UNSET, -1, -1, true],
        ].collect { List it ->
            createCellRangerMwpHelper(
                    it[0] as CellRangerMergingWorkPackage.Status,
                    baseDate,
                    it[1] ? REMINDER_DAYS + (it[1] as Integer) : null,
                    it[2] ? CellRangerDataCleanupJob.DELETE_AFTER_WEEKS * 7 + (it[2] as Integer) : null,
                    it[3],
            )
        }[0..1]

        when:
        cellRangerDataCleanupJob.deleteOfOldFailedWorkflows()

        then:
        1 * cellRangerDataCleanupJob.cellRangerConfigurationService.deleteMwps(_) >> { List<CellRangerMergingWorkPackage> mwp ->
            TestCase.assertContainSame(mwp[0], expectedMwps)
        }
        0 * cellRangerDataCleanupJob.cellRangerConfigurationService._
        1 * cellRangerDataCleanupJob.messageSourceService.createMessage("cellRanger.notification.failedDeletionInformation.subject", [:]) >> header
        1 * cellRangerDataCleanupJob.messageSourceService.createMessage("cellRanger.notification.failedDeletionInformation.body", _) >> body
        1 * cellRangerDataCleanupJob.mailHelperService.sendEmailToTicketSystem(header, body)
    }

    void "deleteOfOldFailedWorkflows, if no old not finished mwp exist, do not call deleteMwp and do not send mail"() {
        given:
        setupData()

        CellRangerDataCleanupJob cellRangerDataCleanupJob = new CellRangerDataCleanupJob([
                processingOptionService       : new ProcessingOptionService(),
                cellRangerConfigurationService: Mock(CellRangerConfigurationService),
                messageSourceService          : Mock(MessageSourceService),
                createNotificationTextService : Mock(CreateNotificationTextService),
                mailHelperService             : Mock(MailHelperService),
                userProjectRoleService        : new UserProjectRoleService(),
        ])
        LocalDate baseDate = LocalDate.now()

        [
                [UNSET, 1, 1, true],
                [UNSET, 1, 1, true],
                [UNSET, -1, 1, true],
                [UNSET, 1, -1, true],
                [UNSET, -1, -1, true],
        ].collect { List it ->
            createCellRangerMwpHelper(
                    it[0] as CellRangerMergingWorkPackage.Status,
                    baseDate,
                    it[1] ? REMINDER_DAYS + (it[1] as Integer) : null,
                    it[2] ? DELETION_DAYS + REMINDER_DAYS + (it[2] as Integer) : null,
                    it[3],
            )
        }

        when:
        cellRangerDataCleanupJob.deleteOfOldFailedWorkflows()

        then:
        0 * cellRangerDataCleanupJob.cellRangerConfigurationService._
        0 * cellRangerDataCleanupJob.messageSourceService.createMessage("cellRanger.notification.failedDeletionInformation.subject", _)
        0 * cellRangerDataCleanupJob.messageSourceService.createMessage("cellRanger.notification.failedDeletionInformation.body", _)
        0 * cellRangerDataCleanupJob.mailHelperService.sendEmailToTicketSystem(_, _)
    }

    void "notifyAndDeletion, uses results of getResultsToDelete"() {
        given:
        setupData()

        CellRangerDataCleanupJob cellRangerDataCleanupJob = new CellRangerDataCleanupJob([
                processingOptionService       : new ProcessingOptionService(),
                cellRangerConfigurationService: Mock(CellRangerConfigurationService),
                messageSourceService          : Mock(MessageSourceService),
                createNotificationTextService : Mock(CreateNotificationTextService),
                mailHelperService             : Mock(MailHelperService),
                userProjectRoleService        : new UserProjectRoleService(),
        ])
        LocalDate baseDate = LocalDate.now()

        List<CellRangerMergingWorkPackage> expectedMwps = [
                [UNSET, 1, 1, true],
                [UNSET, 1, 1, true],
        ].collect { List it ->
            createCellRangerMwpHelper(
                    it[0] as CellRangerMergingWorkPackage.Status,
                    baseDate,
                    it[1] ? REMINDER_DAYS + (it[1] as Integer) : null,
                    it[2] ? DELETION_DAYS + REMINDER_DAYS + (it[2] as Integer) : null,
                    it[3],
            )
        }

        when:
        cellRangerDataCleanupJob.notifyAndDeletion()

        then:
        1 * cellRangerDataCleanupJob.cellRangerConfigurationService.deleteMwps([expectedMwps[0]])
        1 * cellRangerDataCleanupJob.cellRangerConfigurationService.deleteMwps([expectedMwps[1]])
    }

    void "notifyAndDeletion, if no results are found, skips delete call"() {
        given:
        setupData()

        CellRangerDataCleanupJob cellRangerDataCleanupJob = new CellRangerDataCleanupJob([
                processingOptionService       : new ProcessingOptionService(),
                cellRangerConfigurationService: Mock(CellRangerConfigurationService),
                messageSourceService          : Mock(MessageSourceService),
                createNotificationTextService : Mock(CreateNotificationTextService),
        ])

        when:
        cellRangerDataCleanupJob.notifyAndDeletion()

        then:
        0 * cellRangerDataCleanupJob.cellRangerConfigurationService.deleteMwps(_)
    }

    void "setDateOfInformed, setInformedFlag for each mwp"() {
        given:
        setupData()

        CellRangerDataCleanupJob cellRangerDataCleanupJob = new CellRangerDataCleanupJob([
                processingOptionService       : new ProcessingOptionService(),
                cellRangerConfigurationService: Mock(CellRangerConfigurationService),
        ])
        List<CellRangerMergingWorkPackage> mwps = (1..3).collect { createMergingWorkPackage() }

        when:
        cellRangerDataCleanupJob.cellRangerMWPsetDateOfInformed(mwps)

        then:
        mwps.size() * cellRangerDataCleanupJob.cellRangerConfigurationService.setInformedFlag(_, _)
    }

    void "checkAndNotifyUncategorisedResults, groups mwps by project and calls function per group"() {
        given:
        setupData()

        CellRangerDataCleanupJob cellRangerDataCleanupJob = new CellRangerDataCleanupJob([
                processingOptionService       : new ProcessingOptionService(),
                messageSourceService          : Mock(MessageSourceService),
                createNotificationTextService : Mock(CreateNotificationTextService),
                cellRangerConfigurationService: Mock(CellRangerConfigurationService),
                mailHelperService             : Mock(MailHelperService),
                userProjectRoleService        : new UserProjectRoleService(),
        ])
        LocalDate baseDate = LocalDate.now()

        Closure<CellRangerMergingWorkPackage> createMwpToBeNotifiedForProject = { Project project ->
            Sample sample = createSample(
                    individual: createIndividual(
                            project: project ?: createProject()
                    )
            )
            return createCellRangerMwpHelper(UNSET, baseDate, null, REMINDER_DAYS + 1, true, [sample: sample])
        }
        Project project1 = createProject()
        CellRangerMergingWorkPackage crmwp1 = createMwpToBeNotifiedForProject(project1)
        CellRangerMergingWorkPackage crmwp2 = createMwpToBeNotifiedForProject(project1)

        Project project2 = createProject()
        CellRangerMergingWorkPackage crmwp3 = createMwpToBeNotifiedForProject(project2)

        when:
        cellRangerDataCleanupJob.checkAndNotifyUncategorisedResults()

        then:
        1 * cellRangerDataCleanupJob.messageSourceService.createMessage("cellRanger.notification.${CellRangerDataCleanupJob.InformationType.REMINDER}." +
                "subject", [project: project1])
        1 * cellRangerDataCleanupJob.cellRangerConfigurationService.setInformedFlag(crmwp1, _)
        1 * cellRangerDataCleanupJob.cellRangerConfigurationService.setInformedFlag(crmwp2, _)

        and:
        1 * cellRangerDataCleanupJob.messageSourceService.createMessage("cellRanger.notification.${CellRangerDataCleanupJob.InformationType.REMINDER}." +
                "subject", [project: project2])
        1 * cellRangerDataCleanupJob.cellRangerConfigurationService.setInformedFlag(crmwp3, _)
    }
}
