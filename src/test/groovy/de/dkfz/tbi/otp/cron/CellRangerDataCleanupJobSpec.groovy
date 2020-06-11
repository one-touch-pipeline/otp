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

import grails.testing.gorm.DataTest
import spock.lang.Specification

import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.dataprocessing.cellRanger.CellRangerConfigurationService
import de.dkfz.tbi.otp.dataprocessing.cellRanger.CellRangerMergingWorkPackage
import de.dkfz.tbi.otp.domainFactory.pipelines.cellRanger.CellRangerFactory
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.notification.CreateNotificationTextService
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.security.User
import de.dkfz.tbi.otp.security.UserAndRoles
import de.dkfz.tbi.otp.utils.*

import java.time.LocalDate
import java.time.ZoneId

import static de.dkfz.tbi.otp.dataprocessing.cellRanger.CellRangerMergingWorkPackage.Status.*

class CellRangerDataCleanupJobSpec extends Specification implements CellRangerFactory, DataTest, UserAndRoles {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                Project,
                CellRangerMergingWorkPackage,
                Individual,
                User,
                ProjectRole,
                UserProjectRole,
                ProcessingOption,
        ]
    }

    static final int REMINDER_WEEKS = 12
    static final int REMINDER_DAYS = REMINDER_WEEKS * 7
    static final int DELETION_WEEKS = 4
    static final int DELETION_DAYS = DELETION_WEEKS * 7
    static final String EMAIL_RECIPIENT = "email-recipient@notification.com"

    void setupData() {
        createAllBasicProjectRoles()

        DomainFactory.createProcessingOptionLazy(
                name : ProcessingOption.OptionName.CELLRANGER_CLEANUP_WEEKS_TILL_REMINDER,
                value: REMINDER_WEEKS,
        )
        DomainFactory.createProcessingOptionLazy(
                name : ProcessingOption.OptionName.CELLRANGER_CLEANUP_WEEKS_TILL_DELETION_AFTER_REMINDER,
                value: DELETION_WEEKS,
        )
        DomainFactory.createProcessingOptionLazy(
                name : ProcessingOption.OptionName.EMAIL_RECIPIENT_NOTIFICATION,
                value: EMAIL_RECIPIENT,
        )
    }

    CellRangerMergingWorkPackage createCellRangerMwpHelper(CellRangerMergingWorkPackage.Status status, LocalDate baseDate, Integer informedOffset, Integer dateCreatedOffset, Map properties = [:]) {
        Closure<Date> getBaseDateMinusDays = { Integer offset ->
            return Date.from(baseDate.minusDays(offset).atStartOfDay(ZoneId.systemDefault()).toInstant())
        }

        CellRangerMergingWorkPackage mwp = createMergingWorkPackage([status: status] + properties)
        if (informedOffset) {
            mwp.informed = getBaseDateMinusDays(informedOffset)
        }
        if (dateCreatedOffset) {
            // can not give "dateCreated" as argument to the DomainFactory, since it is a automatically created attribute
            mwp.dateCreated = getBaseDateMinusDays(dateCreatedOffset)
        }
        return mwp.save(flush: true)
    }

    void "sendNotificationEmail, Type Reminder, the email should only be sent to the correct people and the ticket system"() {
        given:
        setupData()

        CellRangerDataCleanupJob cellRangerDataCleanupJob = new CellRangerDataCleanupJob(
                processingOptionService      : new ProcessingOptionService(),
                messageSourceService         : Stub(MessageSourceService) {
                    createMessage("cellRanger.notification.${CellRangerDataCleanupJob.InformationRecipientType.BIOINFOMRATICIAN}.decisionReminder.subject", _ as Map) >> '[message subject bioinformatician]'
                    createMessage("cellRanger.notification.${CellRangerDataCleanupJob.InformationRecipientType.BIOINFOMRATICIAN}.decisionReminder.body", _ as Map) >> '[message body bioinformatician]'
                    createMessage("cellRanger.notification.${CellRangerDataCleanupJob.InformationRecipientType.AUTHORITIES}.decisionReminder.subject", _ as Map) >> '[message subject authorities]'
                    createMessage("cellRanger.notification.${CellRangerDataCleanupJob.InformationRecipientType.AUTHORITIES}.decisionReminder.body", _ as Map) >> '[message body authorities]'
                },
                mailHelperService            : Mock(MailHelperService),
                createNotificationTextService: Mock(CreateNotificationTextService),
        )

        User user = DomainFactory.createUser()
        User user2 = DomainFactory.createUser()
        User requester = DomainFactory.createUser()

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
        CellRangerMergingWorkPackage crmwp2 = createMwpForProjectAsUser(project1, requester)

        Project project2 = createProject()
        CellRangerMergingWorkPackage crmwp3 = createMwpForProjectAsUser(project2, user2)

        ProjectRole projectRoleToNotify = LEAD_BIOINFORMATICIAN
        ProjectRole projectRoleOnlyToNotifyIfNoBioinfs = PI
        ProjectRole projectRoleRequester = DomainFactory.createProjectRole(name: ":)")

        // UserProjectRoles for project1
        DomainFactory.createUserProjectRole(
                project    : project1,
                user       : user,
                projectRole: projectRoleToNotify,
        )
        DomainFactory.createUserProjectRole(
                project    : project1,
                projectRole: projectRoleOnlyToNotifyIfNoBioinfs,
        )
        DomainFactory.createUserProjectRole(
                project    : project1,
                user       : requester,
                projectRole: projectRoleRequester,
        )

        // UserProjectRoles for project2
        DomainFactory.createUserProjectRole(
                project    : project2,
                user       : user2,
                projectRole: projectRoleOnlyToNotifyIfNoBioinfs,
        )

        when: "all MWPs of Project 1"
        cellRangerDataCleanupJob.sendNotificationEmail(project1, [crmwp1, crmwp2], CellRangerDataCleanupJob.InformationType.REMINDER)

        then: "there should only be one recipient"
        1 * cellRangerDataCleanupJob.mailHelperService.sendEmail(
                '[message subject bioinformatician]',
                '[message body bioinformatician]',
                [user.email, EMAIL_RECIPIENT, requester.email]
        )
        1 * cellRangerDataCleanupJob.createNotificationTextService.createOtpLinks([project1], 'cellRanger', 'finalRunSelection')

        when: "all MWPs of Project 2"
        cellRangerDataCleanupJob.sendNotificationEmail(project2, [crmwp3], CellRangerDataCleanupJob.InformationType.REMINDER)

        then: "Beside the ticket system, PI Users should be notified only"
        0 * cellRangerDataCleanupJob.mailHelperService.sendEmail(
                '[message subject bioinformatician]',
                '[message body bioinformatician]',
                [user.email, requester.email]
        )
        1 * cellRangerDataCleanupJob.mailHelperService.sendEmail(
                '[message subject authorities]',
                '[message body authorities]',
                [user2.email, EMAIL_RECIPIENT])
        1 * cellRangerDataCleanupJob.createNotificationTextService.createOtpLinks([project2], 'cellRanger', 'finalRunSelection')
        1 * cellRangerDataCleanupJob.createNotificationTextService.createOtpLinks([project2], 'projectUser', 'index')
    }

    void "getResultsToDelete, only deletes too old mwps"() {
        given:
        setupData()

        CellRangerDataCleanupJob cellRangerDataCleanupJob = new CellRangerDataCleanupJob(
                processingOptionService: new ProcessingOptionService(),
        )
        LocalDate baseDate = LocalDate.now()

        List<CellRangerMergingWorkPackage> mwps = [
                [UNSET,      1,  1], // expected, informed and deletion date passed
                [UNSET,     -1,  1], // informed but before reminderDeadline
                [UNSET,   null,  1], // to be deleted but not informed yet
                [UNSET,   null, -1], // not to be deleted and not informed yet
                [FINAL,      1,  1], // should not be found since status is FINAL
                [DELETED,    1,  1], // should not be found since status is DELETED
                [FINAL,     -1,  1],
                [FINAL,   null,  1],
                [FINAL,   null, -1],
                [DELETED, null,  1],
                [DELETED, null, -1],
        ].collect { List it ->
            createCellRangerMwpHelper(
                    it[0] as CellRangerMergingWorkPackage.Status,
                    baseDate,
                    it[1] ? DELETION_DAYS + (it[1] as Integer) : null,
                    it[2] ? DELETION_DAYS + REMINDER_DAYS + (it[2] as Integer) : null
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
                [UNSET,   null,  1], // expected, to be reminded
                [UNSET,   null, -1], // reminder time not yet met
                [FINAL,   null,  1], // to be reminded but already FINAL
                [DELETED, null,  1], // to be reminded but already DELETED
                [UNSET,      1,  1], // already reminded
                [FINAL,   null, -1],
                [DELETED, null, -1],
        ].collect { List it ->
            createCellRangerMwpHelper(
                    it[0] as CellRangerMergingWorkPackage.Status,
                    baseDate,
                    it[1] as Integer,
                    it[2] ? REMINDER_DAYS + (it[2] as Integer) : null
            )
        }

        expect: "Finds only old unfinal items"
        CollectionUtils.containSame(cellRangerDataCleanupJob.resultsNeedingReminder, [mwps[0]])
    }

    void "buildReminderMessageBody, checking if the service call is correct"() {
        given:
        setupData()

        CellRangerDataCleanupJob cellRangerDataCleanupJob = new CellRangerDataCleanupJob(
                processingOptionService      : new ProcessingOptionService(),
                createNotificationTextService: Mock(CreateNotificationTextService),
                messageSourceService         : Mock(MessageSourceService),
        )
        LocalDate baseDate = LocalDate.now()

        Closure<CellRangerMergingWorkPackage> createMwpToBeNotifiedForProject = { Project project ->
            Sample sample = createSample(
                    individual: createIndividual(
                            project: project ?: createProject()
                    )
            )
            return createCellRangerMwpHelper(UNSET, baseDate, null, REMINDER_DAYS + 1, [sample: sample])
        }

        Project project = createProject()
        List<CellRangerMergingWorkPackage> cellRangerMergingWorkPackages = [
                createMwpToBeNotifiedForProject(project),
                createMwpToBeNotifiedForProject(project),
        ]

        when: "the email body is build"
        cellRangerDataCleanupJob.buildReminderMessageBody(project, cellRangerMergingWorkPackages, CellRangerDataCleanupJob.InformationRecipientType.BIOINFOMRATICIAN)

        then: "message creation is called with the correct parameters"
        1 * cellRangerDataCleanupJob.messageSourceService.createMessage("cellRanger.notification.${CellRangerDataCleanupJob.InformationRecipientType.BIOINFOMRATICIAN}.decisionReminder.body", [
                project                 : project.name,
                plannedDeletionDate     : cellRangerDataCleanupJob.formattedPlannedDeletionDate,
                formattedMwpList        : CellRangerDataCleanupJob.getFormattedMwpList(cellRangerMergingWorkPackages),
                otpLinkCellRanger       : '[link to CellRanger]',
                otpLinkUserManagement   : '[link to UserManagement]',
        ])

        and: "includes the links to the action pages"
        1 * cellRangerDataCleanupJob.createNotificationTextService.createOtpLinks([project], 'cellRanger', 'finalRunSelection') >> '[link to CellRanger]'
        1 * cellRangerDataCleanupJob.createNotificationTextService.createOtpLinks([project], 'projectUser', 'index') >> '[link to UserManagement]'
    }

    void "notifyAndDeletion, uses results of getResultsToDelete"() {
        given:
        setupData()

        CellRangerDataCleanupJob cellRangerDataCleanupJob = new CellRangerDataCleanupJob(
                processingOptionService       : new ProcessingOptionService(),
                cellRangerConfigurationService: Mock(CellRangerConfigurationService),
                messageSourceService          : Mock(MessageSourceService),
                createNotificationTextService : Mock(CreateNotificationTextService),
                mailHelperService             : Mock(MailHelperService),
        )
        LocalDate baseDate = LocalDate.now()

        List<CellRangerMergingWorkPackage> expectedMwps = [
                [UNSET, 1, 1],
                [UNSET, 1, 1],
        ].collect { List it ->
            createCellRangerMwpHelper(
                    it[0] as CellRangerMergingWorkPackage.Status,
                    baseDate,
                    it[1] ? REMINDER_DAYS + (it[1] as Integer) : null,
                    it[2] ? DELETION_DAYS + REMINDER_DAYS + (it[2] as Integer) : null
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

        CellRangerDataCleanupJob cellRangerDataCleanupJob = new CellRangerDataCleanupJob(
                processingOptionService       : new ProcessingOptionService(),
                cellRangerConfigurationService: Mock(CellRangerConfigurationService),
                messageSourceService          : Mock(MessageSourceService),
                createNotificationTextService : Mock(CreateNotificationTextService),
        )

        when:
        cellRangerDataCleanupJob.notifyAndDeletion()

        then:
        0 * cellRangerDataCleanupJob.cellRangerConfigurationService.deleteMwps(_)
    }

    void "setDateOfInformed, setInformedFlag for each mwp"() {
        given:
        setupData()

        CellRangerDataCleanupJob cellRangerDataCleanupJob = new CellRangerDataCleanupJob(
                processingOptionService       : new ProcessingOptionService(),
                cellRangerConfigurationService: Mock(CellRangerConfigurationService),
        )
        List<CellRangerMergingWorkPackage> mwps = (1..3).collect { createMergingWorkPackage() }

        when:
        cellRangerDataCleanupJob.cellRangerMWPsetDateOfInformed(mwps)

        then:
        mwps.size() * cellRangerDataCleanupJob.cellRangerConfigurationService.setInformedFlag(_, _)
    }

    void "checkAndNotifyUncategorisedResults, groups mwps by project and calls function per group"() {
        given:
        setupData()

        CellRangerDataCleanupJob cellRangerDataCleanupJob = new CellRangerDataCleanupJob(
                processingOptionService       : new ProcessingOptionService(),
                messageSourceService          : Mock(MessageSourceService),
                createNotificationTextService : Mock(CreateNotificationTextService),
                cellRangerConfigurationService: Mock(CellRangerConfigurationService),
                mailHelperService             : Mock(MailHelperService),
        )
        LocalDate baseDate = LocalDate.now()

        Closure<CellRangerMergingWorkPackage> createMwpToBeNotifiedForProject = { Project project ->
            Sample sample = createSample(
                    individual: createIndividual(
                            project: project ?: createProject()
                    )
            )
            return createCellRangerMwpHelper(UNSET, baseDate, null, REMINDER_DAYS + 1, [sample: sample])
        }
        Project project1 = createProject()
        CellRangerMergingWorkPackage crmwp1 = createMwpToBeNotifiedForProject(project1)
        CellRangerMergingWorkPackage crmwp2 = createMwpToBeNotifiedForProject(project1)

        Project project2 = createProject()
        CellRangerMergingWorkPackage crmwp3 = createMwpToBeNotifiedForProject(project2)

        when:
        cellRangerDataCleanupJob.checkAndNotifyUncategorisedResults()

        then:
        1 * cellRangerDataCleanupJob.messageSourceService.createMessage("cellRanger.notification." +
                "${CellRangerDataCleanupJob.InformationRecipientType.AUTHORITIES}.${CellRangerDataCleanupJob.InformationType.REMINDER}." +
                "subject", [project: project1])
        1 * cellRangerDataCleanupJob.cellRangerConfigurationService.setInformedFlag(crmwp1, _)
        1 * cellRangerDataCleanupJob.cellRangerConfigurationService.setInformedFlag(crmwp2, _)

        and:
        1 * cellRangerDataCleanupJob.messageSourceService.createMessage("cellRanger.notification." +
                "${CellRangerDataCleanupJob.InformationRecipientType.AUTHORITIES}.${CellRangerDataCleanupJob.InformationType.REMINDER}." +
                "subject", [project: project2])
        1 * cellRangerDataCleanupJob.cellRangerConfigurationService.setInformedFlag(crmwp3, _)
    }
}
