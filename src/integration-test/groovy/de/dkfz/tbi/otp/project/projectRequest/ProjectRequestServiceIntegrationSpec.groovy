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
package de.dkfz.tbi.otp.project.projectRequest

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import grails.web.mapping.LinkGenerator
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.domainFactory.ProjectFieldsDomainFactory
import de.dkfz.tbi.otp.domainFactory.UserDomainFactory
import de.dkfz.tbi.otp.domainFactory.taxonomy.TaxonomyFactory
import de.dkfz.tbi.otp.ngsdata.ProjectRole
import de.dkfz.tbi.otp.ngsdata.SeqCenter
import de.dkfz.tbi.otp.ngsdata.UserProjectRoleService
import de.dkfz.tbi.otp.ngsdata.taxonomy.SpeciesWithStrain
import de.dkfz.tbi.otp.project.*
import de.dkfz.tbi.otp.project.additionalField.*
import de.dkfz.tbi.otp.searchability.Keyword
import de.dkfz.tbi.otp.security.*
import de.dkfz.tbi.otp.security.user.DepartmentService
import de.dkfz.tbi.otp.administration.MailHelperService
import de.dkfz.tbi.otp.security.user.UserService
import de.dkfz.tbi.otp.utils.MessageSourceService

import java.time.LocalDate

@Integration
@Rollback
class ProjectRequestServiceIntegrationSpec extends Specification implements UserDomainFactory, UserAndRoles, ProjectFieldsDomainFactory, TaxonomyFactory {

    ProjectRequestService projectRequestService
    ProjectRequestStateProvider projectRequestStateProvider
    SecurityService securityService
    TestConfigService configService

    final String subject = "subject"
    final String body = "body"
    final String link = "link"
    final String comment = "comment"

    String emailSenderSalutation

    void setup() {
        projectRequestService = new ProjectRequestService([
                messageSourceService                : Mock(MessageSourceService),
                mailHelperService                   : Mock(MailHelperService),
                linkGenerator                       : Mock(LinkGenerator),
                securityService                     : Mock(SecurityService),
                auditLogService                     : Mock(AuditLogService),
                projectRequestUserService           : Mock(ProjectRequestUserService),
                processingOptionService             : Mock(ProcessingOptionService),
                projectRequestStateProvider         : Mock(ProjectRequestStateProvider),
                userProjectRoleService              : Mock(UserProjectRoleService),
                projectRequestPersistentStateService: new ProjectRequestPersistentStateService([
                        projectRequestStateProvider: Mock(ProjectRequestStateProvider)
                ]),
                configService                       : configService,
                departmentService                   : Mock(DepartmentService),
                userService                         : new UserService(),
        ])
    }

    void "piBelongsToDepartment returns true when some users from list belongs to the given department"() {
        given:
        User user1 = createUser(username: 'u1', realName: 'user1')
        createUser(username: 'u2', realName: 'user2')
        createDepartment([ouNumber: 'dept1', departmentHeads: [user1]])
        projectRequestService.departmentService.getListOfHeadsForDepartment('dept1') >> [user1]
        projectRequestService.departmentService.getListOfPIForDepartment('dept1') >> ['u1': 'user1']

        expect:
        projectRequestService.piBelongsToDepartment(['u1', 'u2'], 'dept1')
    }

    void "piBelongsToDepartment returns false when user from list doesn't belong to the given department"() {
        given:
        createUser(username: 'u1', realName: 'user1')
        User user2 = createUser(username: 'u2', realName: 'user2')
        createDepartment([ouNumber: 'dept1', departmentHeads: [user2]])
        projectRequestService.departmentService.getListOfHeadsForDepartment('dept1') >> [user2]
        projectRequestService.departmentService.getListOfPIForDepartment('dept1') >> ['u2': 'user2']

        expect:
        !projectRequestService.piBelongsToDepartment(['u1'], 'dept1')
    }

    void "piBelongsToDepartment returns false when the user from list belongs to another department"() {
        given:
        User user1 = createUser(username: 'u1', realName: 'user1')
        User user2 = createUser(username: 'u2', realName: 'user2')
        createDepartment([ouNumber: 'dept1', departmentHeads: [user1]])
        createDepartment([ouNumber: 'dept2', departmentHeads: [user2]])
        projectRequestService.departmentService.getListOfHeadsForDepartment('dept2') >> [user2]
        projectRequestService.departmentService.getListOfPIForDepartment('dept2') >> ['u2': 'user2']

        expect:
        !projectRequestService.piBelongsToDepartment(['u1'], 'dept2')
    }

    void "piBelongsToDepartment returns true when all users belong to given department"() {
        given:
        User user1 = createUser(username: 'u1', realName: 'user1')
        User user2 = createUser(username: 'u2', realName: 'user2')
        createDepartment([ouNumber: 'dept1', departmentHeads: [user1, user2]])
        projectRequestService.departmentService.getListOfHeadsForDepartment('dept1') >> [user1, user2]
        projectRequestService.departmentService.getListOfPIForDepartment('dept1') >> ['u1': 'user1', 'u2': 'user2']

        expect:
        projectRequestService.piBelongsToDepartment(['u1', 'u2'], 'dept1')
    }

    void "piBelongsToDepartment returns false when none of the users from list belong to given department"() {
        given:
        createUser(username: 'u1')
        createUser(username: 'u2')
        User user3 = createUser(username: 'u3')
        createDepartment([ouNumber: 'dept1', departmentHeads: [user3]])
        projectRequestService.departmentService.getListOfHeadsForDepartment('dept1') >> [user3]
        projectRequestService.departmentService.getListOfPIForDepartment('dept1') >> ['u3': 'user3']

        expect:
        !projectRequestService.piBelongsToDepartment(['u1', 'u2'], 'dept1')
    }

    void "sendSubmitEmail"() {
        given:
        final User requester = createUser()
        final User pi1 = createUser()
        final User pi2 = createUser()
        final List<User> users = [pi1, pi2]
        final ProjectRequest request = createProjectRequest([requester: requester], [usersThatNeedToApprove: users])

        when:
        projectRequestService.sendSubmitEmail(request)

        then:
        1 * projectRequestService.messageSourceService.createMessage(_, _) >> subject
        1 * projectRequestService.linkGenerator.link(_) >> link
        1 * projectRequestService.messageSourceService.createMessage(_, [
                link         : link,
                requester    : "${requester.realName} (${requester.username})",
                teamSignature: emailSenderSalutation,
        ]) >> body
        1 * projectRequestService.mailHelperService.saveMail(subject, body, [requester.email], users*.email)
        1 * projectRequestService.processingOptionService.findOptionAsString(_) >> emailSenderSalutation
        0 * _
    }

    void "sendOperatorRejectEmail"() {
        given:
        final User requester = createUser()
        final User pi1 = createUser()
        final User pi2 = createUser()
        final List<User> users = [pi1, pi2]
        final ProjectRequest request = createProjectRequest([requester: requester], [usersThatNeedToApprove: users])

        when:
        projectRequestService.sendOperatorRejectEmail(request, comment)

        then:
        1 * projectRequestService.messageSourceService.createMessage(_, _) >> subject
        1 * projectRequestService.linkGenerator.link(_) >> link
        1 * projectRequestService.messageSourceService.createMessage(_, [
                requester    : "${requester.realName} (${requester.username})",
                comment      : comment,
                link         : link,
                teamSignature: emailSenderSalutation,
        ]) >> body
        1 * projectRequestService.mailHelperService.saveMail(subject, body, [requester.email], users*.email)
        1 * projectRequestService.processingOptionService.findOptionAsString(_) >> emailSenderSalutation
        0 * _
    }

    void "sendPassOnEmail"() {
        given:
        final User requester = createUser()
        final User pi1 = createUser()
        final User pi2 = createUser()
        final List<User> authorities = [pi1, pi2]
        final ProjectRequest request = createProjectRequest([requester: requester], [usersThatNeedToApprove: authorities])
        final String expectedAuthorityUsernames = authorities.collect { "${it.realName} (${it.username})" }.join(", ")

        when:
        projectRequestService.sendPassOnEmail(request)

        then:
        1 * projectRequestService.messageSourceService.createMessage(_, _) >> subject
        1 * projectRequestService.linkGenerator.link(_) >> link
        1 * projectRequestService.messageSourceService.createMessage(_, [
                recipients        : expectedAuthorityUsernames,
                projectAuthorities: expectedAuthorityUsernames,
                projectRequestName: request.name,
                link              : link,
                teamSignature     : emailSenderSalutation,
        ]) >> body
        1 * projectRequestService.mailHelperService.saveMail(subject, body, authorities*.email, [requester.email])
        1 * projectRequestService.processingOptionService.findOptionAsString(_) >> emailSenderSalutation
        0 * _
    }

    void "sendPiRejectEmail"() {
        given:
        final User requester = createUser()
        final User pi1 = createUser()
        final User pi2 = createUser()
        final List<User> users = [pi1, pi2]
        final ProjectRequest request = createProjectRequest([requester: requester], [usersThatNeedToApprove: users])

        when:
        projectRequestService.sendPiRejectEmail(request, comment)

        then:
        1 * projectRequestService.messageSourceService.createMessage(_, _) >> subject
        1 * projectRequestService.linkGenerator.link(_) >> link
        1 * projectRequestService.securityService.currentUser >> pi1
        1 * projectRequestService.messageSourceService.createMessage(_, [
                requester         : "${requester.username} (${requester.realName})",
                projectRequestName: request.name,
                projectAuthority  : pi1.username,
                comment           : comment,
                link              : link,
                teamSignature     : emailSenderSalutation,
        ]) >> body
        1 * projectRequestService.mailHelperService.saveMail(subject, body, [requester.email], users*.email)
        1 * projectRequestService.processingOptionService.findOptionAsString(_) >> emailSenderSalutation
        0 * _
    }

    void "sendApprovedEmail"() {
        given:
        final User requester = createUser()
        final User pi1 = createUser()
        final User pi2 = createUser()
        final List<User> users = [pi1, pi2]
        final ProjectRequest request = createProjectRequest([requester: requester], [usersThatNeedToApprove: users])

        when:
        projectRequestService.sendApprovedEmail(request)

        then:
        1 * projectRequestService.messageSourceService.createMessage(_, _) >> subject
        1 * projectRequestService.messageSourceService.createMessage(_, [
                requester         : "${requester.username} (${requester.realName})",
                projectRequestName: request.name,
                teamSignature     : emailSenderSalutation,
        ]) >> body
        1 * projectRequestService.mailHelperService.saveMail(subject, body, [requester.email], users*.email)
        1 * projectRequestService.processingOptionService.findOptionAsString(_) >> emailSenderSalutation
        0 * _
    }

    void "recipientShouldBeUnique"() {
        given:
        final User currentUser = createUser()
        final User requester = createUser()
        final List<User> users = [createUser(), createUser(), requester]
        final ProjectRequest request = createProjectRequest([requester: requester], [
                usersThatNeedToApprove  : users[0..0],
                usersThatAlreadyApproved: users[1..2],
        ])

        when:
        projectRequestService.sendSubmitEmail(request)
        projectRequestService.sendPassOnEmail(request)
        projectRequestService.sendPiRejectEmail(request, comment)
        projectRequestService.sendOperatorRejectEmail(request, comment)
        projectRequestService.sendPartiallyApprovedEmail(request)
        projectRequestService.sendPiEditedEmail(request)
        projectRequestService.sendApprovedEmail(request)
        projectRequestService.sendDeleteEmail(request)
        projectRequestService.sendDraftCreateEmail(request)
        projectRequestService.sendDraftDeleteEmail(request)

        then:
        4 * projectRequestService.securityService.currentUser >> currentUser
        6 * projectRequestService.linkGenerator.link(_) >> link
        7 * projectRequestService.mailHelperService.saveMail(_, _, _, _) >> { arguments ->
            final List<User> recipients = arguments.get(2)
            assert recipients.unique(false).size() == recipients.size()
        }
        3 * projectRequestService.mailHelperService.saveMail(_, _, _) >> { arguments ->
            final List<User> recipients = arguments.get(2)
            assert recipients.unique(false).size() == recipients.size()
        }
    }

    void "sendPartiallyApprovedEmail"() {
        given:
        final User requester = createUser()
        final User pi1 = createUser()
        final User pi2 = createUser()
        final List<User> users = [pi1, pi2]
        final List<User> usersThatNeedToApprove = users[0..0]
        final List<User> usersThatAlreadyApproved = users[1..1]
        final ProjectRequest request = createProjectRequest([
                requester: requester,
        ], [
                usersThatNeedToApprove  : usersThatNeedToApprove,
                usersThatAlreadyApproved: usersThatAlreadyApproved,
        ])

        when:
        projectRequestService.sendPartiallyApprovedEmail(request)

        then:
        1 * projectRequestService.messageSourceService.createMessage(_, _) >> subject
        1 * projectRequestService.securityService.currentUser >> pi1
        1 * projectRequestService.messageSourceService.createMessage(_, [
                requester         : "${requester.username} (${requester.realName})",
                projectRequestName: request.name,
                projectAuthority  : pi1.username,
                teamSignature     : emailSenderSalutation,
        ]) >> body
        1 * projectRequestService.processingOptionService.findOptionAsString(_) >> emailSenderSalutation
        1 * projectRequestService.mailHelperService.saveMail(subject, body, [requester.email], (usersThatNeedToApprove + usersThatAlreadyApproved)*.email)
        0 * _
    }

    void "sendDeleteEmail"() {
        given:
        final User requester = createUser()
        final User pi1 = createUser()
        final User pi2 = createUser()
        final List<User> users = [pi1, pi2, requester]
        final ProjectRequest request = createProjectRequest([requester: requester], [usersThatNeedToApprove: users])
        final List<User> expectedRecipients = users
        final String expectedAuthorityUsernames = users.collect { "${it.realName} (${it.username})" }.join(", ")

        when:
        projectRequestService.sendDeleteEmail(request)

        then:
        1 * projectRequestService.messageSourceService.createMessage(_, _) >> subject
        1 * projectRequestService.securityService.currentUser >> pi1
        1 * projectRequestService.messageSourceService.createMessage(_, [
                recipients        : "$expectedAuthorityUsernames, ${requester.realName} (${requester.username})",
                projectRequestName: request.name,
                deletingUser      : pi1,
                teamSignature     : emailSenderSalutation,
        ]) >> body
        1 * projectRequestService.processingOptionService.findOptionAsString(_) >> emailSenderSalutation
        1 * projectRequestService.mailHelperService.saveMail(subject, body, expectedRecipients*.email)
        0 * _
    }

    void "sendDraftCreateEmail"() {
        given:
        final String link = "link"
        final User requester = createUser()
        final ProjectRequest request = createProjectRequest([
                requester: requester,
        ])

        when:
        projectRequestService.sendDraftCreateEmail(request)

        then:
        1 * projectRequestService.messageSourceService.createMessage(_, _) >> subject
        1 * projectRequestService.linkGenerator.link(_) >> link
        1 * projectRequestService.messageSourceService.createMessage(_, [
                requester    : "${requester.realName} (${requester.username})",
                link         : link,
                teamSignature: emailSenderSalutation,
        ]) >> body
        1 * projectRequestService.mailHelperService.saveMail(subject, body, [requester.email])
        1 * projectRequestService.processingOptionService.findOptionAsString(_) >> emailSenderSalutation
        0 * _
    }

    void "sendDraftDeleteEmail"() {
        given:
        final User requester = createUser()
        final ProjectRequest request = createProjectRequest([
                requester: requester,
        ])

        when:
        projectRequestService.sendDraftDeleteEmail(request)

        then:
        1 * projectRequestService.messageSourceService.createMessage(_, _) >> subject
        1 * projectRequestService.messageSourceService.createMessage(_, [
                requester         : "${requester.username} (${requester.realName})",
                projectRequestName: request.name,
                teamSignature     : emailSenderSalutation,
        ]) >> body
        1 * projectRequestService.mailHelperService.saveMail(subject, body, [requester.email])
        1 * projectRequestService.processingOptionService.findOptionAsString(_) >> emailSenderSalutation
        0 * _
    }

    void "sendPiEditedEmail"() {
        given:
        final User requester = createUser()
        final User pi1 = createUser()
        final User pi2 = createUser()
        final List<User> users = [pi1, pi2]
        final List<User> usersThatNeedToApprove = users[0..0]
        final List<User> usersThatAlreadyApproved = users[1..1]
        final ProjectRequest request = createProjectRequest([
                requester: requester,
        ], [
                usersThatNeedToApprove  : usersThatNeedToApprove,
                usersThatAlreadyApproved: usersThatAlreadyApproved,
        ])

        when:
        projectRequestService.sendPiEditedEmail(request)

        then:
        1 * projectRequestService.messageSourceService.createMessage(_, _) >> subject
        1 * projectRequestService.securityService.currentUser >> pi1
        1 * projectRequestService.linkGenerator.link(_) >> link
        1 * projectRequestService.messageSourceService.createMessage(_, [
                projectAuthorities: (usersThatNeedToApprove + usersThatAlreadyApproved)*.username.join(", "),
                projectAuthority  : pi1.username,
                projectRequestName: request.name,
                link              : link,
                teamSignature     : emailSenderSalutation,
        ]) >> body
        1 * projectRequestService.processingOptionService.findOptionAsString(_) >> emailSenderSalutation
        1 * projectRequestService.mailHelperService.saveMail(subject, body, users*.email, [requester.email])
        0 * _
    }

    void "sendCreatedEmail, should send a mail only to the ticketsystem when recipient and ccs are empty"() {
        given:
        final String dirAnalysis = "/dirAnalysis"
        final String dirName = "dirName"
        final ProjectRequest projectRequest = ProjectRequest.create()
        final Project project = createProject([
                dirAnalysis: dirAnalysis,
                dirName    : dirName,
        ])
        final String ticketingSystemMail = "ticketingSystemMail"
        final String clusterTicketingSystemMail = "clusterTicketingSystemMail"
        final String additionalText = "additionalText"
        final String clusterName = "clusterName"

        projectRequestService.messageSourceService = Mock(MessageSourceService)
        projectRequestService.mailHelperService = Mock(MailHelperService)
        projectRequestService.processingOptionService = Mock(ProcessingOptionService)
        projectRequestService.linkGenerator = Mock(LinkGenerator)

        when:
        projectRequestService.sendCreatedEmail(projectRequest, project, [], [])

        then:
        1 * projectRequestService.processingOptionService.findOptionAsString(ProcessingOption.OptionName.EMAIL_TICKET_SYSTEM) >> ticketingSystemMail
        1 * projectRequestService.processingOptionService.findOptionAsString(ProcessingOption.OptionName.EMAIL_CLUSTER_ADMINISTRATION) >> clusterTicketingSystemMail
        1 * projectRequestService.processingOptionService.findOptionAsString(ProcessingOption.OptionName.EMAIL_STORAGE_ADMINISTRATION) >> ""
        1 * projectRequestService.processingOptionService.findOptionAsString(ProcessingOption.OptionName.EMAIL_PROJECT_CREATION_FREETEXT) >> additionalText
        1 * projectRequestService.processingOptionService.findOptionAsString(ProcessingOption.OptionName.CLUSTER_NAME) >> clusterName
        1 * projectRequestService.processingOptionService.findOptionAsString(ProcessingOption.OptionName.HELP_DESK_TEAM_NAME) >> emailSenderSalutation
        3 * projectRequestService.linkGenerator.link(_) >> link
        1 * projectRequestService.messageSourceService.createMessage(_, [projectName: project.name, projectRequestId: projectRequest?.id ?: '-', projectRequestName: projectRequest?.name ?: '-']) >> subject
        1 * projectRequestService.messageSourceService.createMessage(_, [
                projectName               : project.name,
                projectLink               : link,
                projectConfigLink         : link,
                projectDirectoryPath      : configService.rootPath.toPath().resolve(dirName),
                analysisDirectoryPath     : dirAnalysis,
                additionalText            : additionalText,
                userManagementLink        : link,
                clusterName               : clusterName,
                clusterTicketingSystemMail: clusterTicketingSystemMail,
                ticketingSystemMail       : ticketingSystemMail,
                teamSignature             : emailSenderSalutation,
        ]) >> body
        1 * projectRequestService.mailHelperService.saveMail(subject, body, [], [])
    }

    void "sendCreatedEmail, should send a mail for project to all recipients and ccs"() {
        given:
        final List<String> recipients = ['requester1', 'requester2']
        final List<String> ccs = ['ccUser1', 'ccUser2']
        final String dirAnalysis = "/dirAnalysis"
        final String dirName = "dirName"
        final ProjectRequest projectRequest = ProjectRequest.create()
        final Project project = createProject([
                dirAnalysis: dirAnalysis,
                dirName    : dirName,
        ])
        final String ticketingSystemMail = "ticketingSystemMail"
        final String clusterTicketingSystemMail = "clusterTicketingSystemMail"
        final String storageAdministrationMail = "storageAdministrationMail"
        final String additionalText = "additionalText"
        final String clusterName = "clusterName"

        when:
        projectRequestService.sendCreatedEmail(projectRequest, project, recipients, ccs)

        then:
        1 * projectRequestService.processingOptionService.findOptionAsString(ProcessingOption.OptionName.EMAIL_TICKET_SYSTEM) >> ticketingSystemMail
        1 * projectRequestService.processingOptionService.findOptionAsString(ProcessingOption.OptionName.EMAIL_CLUSTER_ADMINISTRATION) >> clusterTicketingSystemMail
        1 * projectRequestService.processingOptionService.findOptionAsString(ProcessingOption.OptionName.EMAIL_STORAGE_ADMINISTRATION) >> storageAdministrationMail
        1 * projectRequestService.processingOptionService.findOptionAsString(ProcessingOption.OptionName.EMAIL_PROJECT_CREATION_FREETEXT) >> additionalText
        1 * projectRequestService.processingOptionService.findOptionAsString(ProcessingOption.OptionName.CLUSTER_NAME) >> clusterName
        1 * projectRequestService.processingOptionService.findOptionAsString(ProcessingOption.OptionName.HELP_DESK_TEAM_NAME) >> emailSenderSalutation
        3 * projectRequestService.linkGenerator.link(_) >> link
        1 * projectRequestService.messageSourceService.createMessage(_, [projectName: project.name, projectRequestId: projectRequest?.id ?: '-', projectRequestName: projectRequest?.name ?: '-']) >> subject
        1 * projectRequestService.messageSourceService.createMessage(_, [
                projectName               : project.name,
                projectLink               : link,
                projectConfigLink         : link,
                projectDirectoryPath      : configService.rootPath.toPath().resolve(dirName),
                analysisDirectoryPath     : dirAnalysis,
                additionalText            : additionalText,
                userManagementLink        : link,
                clusterName               : clusterName,
                clusterTicketingSystemMail: clusterTicketingSystemMail,
                ticketingSystemMail       : ticketingSystemMail,
                teamSignature             : emailSenderSalutation,
        ]) >> body
        1 * projectRequestService.mailHelperService.saveMail(subject, body, recipients, ccs + storageAdministrationMail)
        0 * _
    }

    ProjectRequestCreationCommand createProjectRequestCreationCommand(Map properties = [:]) {
        return new ProjectRequestCreationCommand([
                name       : "projectRequest${nextId}",
                projectType: Project.ProjectType.SEQUENCING,
                description: "A description, that is long enough to full fill the requirements of the command object.",
                keywords   : [createKeyword(), createKeyword()],
                users      : [createProjectRequestUser()],
                piUsers    : [createProjectRequestUser([projectRoles: [pi]])],
        ] + properties)
    }

    @Unroll
    void "saveProjectRequestFromCommand, should save projectRequest and translate all parameters"() {
        given:
        createAllBasicProjectRoles()

        User currentUser = createUser()
        SeqCenter seqCenter = createSeqCenter()
        SpeciesWithStrain speciesWithStrain = createSpeciesWithStrain()
        List<Keyword> keywords = ['keyword1', 'keyword2', 'test keyword']
        ProjectRequestUser user = createProjectRequestUser()
        ProjectRequestUser piUser = createProjectRequestUser([projectRoles: [pi]])
        List<ProjectRequestUserCommand> users = [new ProjectRequestUserCommand([projectRequestUser: user, username: user.username])]
        List<ProjectRequestUserCommand> piUsers = [new ProjectRequestUserCommand(projectRequestUser: piUser, username: piUser.username)]
        ProjectRequestPersistentState projectRequestPersistentState = createProjectRequestPersistentState()
        ProjectRequest projectRequest = projectRequestExists ? createProjectRequest([state: projectRequestPersistentState]) : null
        ProjectRequestCreationCommand cmd = new ProjectRequestCreationCommand([
                name                    : "projectRequest_${nextId}",
                description             : "A description, that is long enough to fulfill the required length of the command object.",
                seqTypes                : [createSeqType(), createSeqType()],
                customSeqTypes          : ["TestSeqType", "TestSeqType2"],
                approxNoOfSamples       : 12,
                storagePeriod           : storagePeriod,
                storageUntil            : storageUntil,
                speciesWithStrains      : [speciesWithStrain],
                customSpeciesWithStrains: ["customSpeciesWithStrains1", "customSpeciesWithStrains2"],
                sequencingCenters       : [seqCenter],
                customSequencingCenters : ["TestCenter", "TestCenter2"],
                projectRequest          : projectRequest,
                keywords                : keywords,
                requesterComment        : "Comments for the request.",
                projectType             : Project.ProjectType.SEQUENCING,
        ])
        cmd.users = users
        cmd.piUsers = piUsers
        findOrCreateProcessingOption(name: ProcessingOption.OptionName.ENABLE_PROJECT_REQUEST_PI, value: false)
        projectRequestService.projectRequestStateProvider = projectRequestStateProvider
        projectRequestService.projectRequestPersistentStateService.projectRequestStateProvider = projectRequestStateProvider

        when:
        ProjectRequest result = projectRequestService.saveProjectRequestFromCommand(cmd)

        then:
        1 * projectRequestService.processingOptionService.findOptionAsString(ProcessingOption.OptionName.ENABLE_PROJECT_REQUEST_PI) >> false
        1 * projectRequestService.securityService.ensureNotSwitchedUser()
        1 * projectRequestService.projectRequestUserService.saveProjectRequestUsersFromCommands(cmd.users) >> ([user] as Set<ProjectRequestUser>)
        1 * projectRequestService.projectRequestUserService.saveProjectRequestUsersFromCommands(cmd.piUsers) >> ([piUser] as Set<ProjectRequestUser>)
        (projectRequestExists ? 0 : 1) * projectRequestService.securityService.currentUser >> currentUser
        1 * projectRequestService.auditLogService.logAction(_, _) >> _
        0 * _

        then:
        ProjectRequest.count == 1
        result.name == cmd.name
        result.requester == (projectRequestExists ? projectRequest.requester : currentUser)
        result.users == [user] as Set<ProjectRequestUser>
        result.piUsers == [piUser] as Set<ProjectRequestUser>
        result.project == null
        projectRequestExists ? result.state == projectRequestPersistentState : result.state.beanName == "initial"
        TestCase.assertContainSame(result.customSpeciesWithStrains, cmd.customSpeciesWithStrains)
        TestCase.assertContainSame(result.keywords*.name, cmd.keywords)
        TestCase.assertContainSame(result.sequencingCenters, cmd.sequencingCenters)
        result.approxNoOfSamples == cmd.approxNoOfSamples
        TestCase.assertContainSame(result.seqTypes, cmd.seqTypes)
        result.requesterComment == cmd.requesterComment
        result.storageUntil == resultStorageUntil
        result.projectType == cmd.projectType

        where:
        storagePeriod              | storageUntil              | resultStorageUntil            | projectRequestExists
        StoragePeriod.TEN_YEARS    | null                      | LocalDate.now().plusYears(10) | true
        StoragePeriod.USER_DEFINED | LocalDate.of(2000, 12, 1) | storageUntil                  | true
        StoragePeriod.INFINITELY   | null                      | null                          | false
    }

    @Unroll
    void "getRequestsUserIsInvolved, with #resolved parameter and #testDescription requests the user is involved in"() {
        given:
        createUserAndRoles()
        ProjectRequest prRequestedByUser = createProjectRequest([requester: getUser(USER)])
        ProjectRequest prCreatedByOtherUser = createProjectRequest([:], [beanName: "created"])
        ProjectRequest prCreatedByUser = createProjectRequest([requester: getUser(USER)], [beanName: "created"])
        ProjectRequest prDraftByOtherUser = createProjectRequest([:], [beanName: "draft"])
        ProjectRequest prDraftByUser = createProjectRequest([requester: getUser(USER)], [beanName: "draft"])

        String role = isOperator ? OPERATOR : USER

        projectRequestService.securityService = securityService

        when:
        List<ProjectRequest> result = doWithAuth(role) {
            projectRequestService.getRequestsUserIsInvolved(resolved)
        }

        then:
        (isOperator && resolved) ? TestCase.assertContainSame(result, [prCreatedByOtherUser, prCreatedByUser]) : true
        (isOperator && !resolved) ? TestCase.assertContainSame(result, [prRequestedByUser, prDraftByOtherUser, prDraftByUser]) : true
        (!isOperator && resolved) ? TestCase.assertContainSame(result, [prCreatedByUser]) : true
        (!isOperator && !resolved) ? TestCase.assertContainSame(result, [prDraftByUser, prRequestedByUser]) : true

        where:
        testDescription                             | isOperator | resolved
        "user is operator return all resolved"      | true       | true
        "user is operator return all unresolved"    | true       | false
        "user is no operator return all resolved"   | false      | true
        "user is no operator return all unresolved" | false      | false
    }

    @Unroll
    void "sortRequestToBeHandledByUser, should return the requests user has to do something when user is operator is #isOperator"() {
        given:
        createUserAndRoles()
        String userName = isOperator ? OPERATOR : USER
        User currentUser = getUser(userName)

        ProjectRequest prCheckOwnedByUser = createProjectRequest([:], [beanName: "check", currentOwner: currentUser])
        ProjectRequest prCreated = createProjectRequest([:], [beanName: "created"])
        ProjectRequest prApprovalUserNeedsToApprove = createProjectRequest([:], [beanName: "approval", usersThatNeedToApprove: [currentUser]])
        ProjectRequest prApproval = createProjectRequest([:], [beanName: "approval"])
        ProjectRequest prApproved = createProjectRequest([:], [beanName: "approved"])
        ProjectRequest prApprovedOwnedByUser = createProjectRequest([:], [beanName: "approved", currentOwner: currentUser])
        ProjectRequest prDraft = createProjectRequest([:], [beanName: "draft"])
        ProjectRequest prDraftRequestedByUser = createProjectRequest([requester: currentUser], [beanName: "draft"])
        ProjectRequest prPiEdit = createProjectRequest([:], [beanName: "piEdit"])
        ProjectRequest prPiEditOwnedByUser = createProjectRequest([:], [beanName: "piEdit", currentOwner: currentUser])
        ProjectRequest prRequesterEdit = createProjectRequest([:], [beanName: "requesterEdit"])
        ProjectRequest prRequesterEditRequestedByUser = createProjectRequest([requester: currentUser], [beanName: "requesterEdit"])
        List<ProjectRequest> projectRequests = [prCheckOwnedByUser, prCreated, prApprovalUserNeedsToApprove, prApproval, prApproved, prApprovedOwnedByUser,
                                                prDraft, prDraftRequestedByUser, prPiEdit, prPiEditOwnedByUser, prRequesterEdit, prRequesterEditRequestedByUser]

        projectRequestService.projectRequestStateProvider = projectRequestStateProvider
        projectRequestService.projectRequestPersistentStateService.projectRequestStateProvider = projectRequestStateProvider

        when:
        List<ProjectRequest> requestsToBeHandledByUser = doWithAuth(userName) {
            projectRequestService.sortRequestToBeHandledByUser(projectRequests)
        }

        then:
        List<ProjectRequest> expectedRequestToHandle = isOperator
                ? [prApprovalUserNeedsToApprove, prDraftRequestedByUser, prPiEditOwnedByUser, prRequesterEditRequestedByUser,
                   prCheckOwnedByUser, prApproved, prApprovedOwnedByUser]
                : [prApprovalUserNeedsToApprove, prDraftRequestedByUser, prPiEditOwnedByUser, prRequesterEditRequestedByUser]
        TestCase.assertContainSame(requestsToBeHandledByUser, expectedRequestToHandle)

        where:
        isOperator << [true, false]
    }

    void "saveProjectRequest, should save project request to database"() {
        given:
        ProjectRequest projectRequest = createProjectRequest([:], [:], false)

        and:
        assert ProjectRequest.count == 0

        when:
        projectRequestService.saveProjectRequest(projectRequest)

        then:
        ProjectRequest.count == 1
    }

    void "addProjectRequestUsersToProject, should add project request Users to the project with required rights"() {
        given:
        createUserAndRoles()
        createAllBasicProjectRoles()

        Project project = createProject()
        List<ProjectRequestUser> prPiUserList = [createProjectRequestUser(
                projectRoles: [pi],
                accessToOtp: accessToOtp,
                accessToFiles: accessToFiles,
                manageUsers: manageUsers,
                manageUsersAndDelegate: manageUsersAndDelegate,
        ),]
        List<ProjectRequestUser> prUserList = [createProjectRequestUser(
                projectRoles: [bioinformatician],
                accessToOtp: accessToOtp,
                accessToFiles: accessToFiles,
                manageUsers: manageUsers,
                manageUsersAndDelegate: manageUsersAndDelegate,
        ),]
        ProjectRequest projectRequest = createProjectRequest([
                state  : createProjectRequestPersistentState([beanName: "created"]),
                project: project,
                piUsers: prPiUserList,
                users  : prUserList,
        ])

        when:
        doWithAuth(OPERATOR) {
            projectRequestService.addProjectRequestUsersToProject(projectRequest)
        }

        then:
        1 * projectRequestService.userProjectRoleService.createUserProjectRole(prPiUserList[0].user, projectRequest.project, prPiUserList[0].projectRoles, [
                accessToOtp           : prPiUserList[0].accessToOtp,
                accessToFiles         : prPiUserList[0].accessToFiles,
                manageUsers           : prPiUserList[0].manageUsers,
                manageUsersAndDelegate: prPiUserList[0].manageUsersAndDelegate,
                receivesNotifications : true,
        ])
        1 * projectRequestService.userProjectRoleService.createUserProjectRole(prUserList[0].user, projectRequest.project, prUserList[0].projectRoles, [
                accessToOtp           : prUserList[0].accessToOtp,
                accessToFiles         : prUserList[0].accessToFiles,
                manageUsers           : prUserList[0].manageUsers,
                manageUsersAndDelegate: prUserList[0].manageUsersAndDelegate,
                receivesNotifications : true,
        ])
        0 * _

        where:
        accessToOtp | accessToFiles | manageUsers | manageUsersAndDelegate
        true        | true          | true        | true
        false       | false         | false       | false
        true        | false         | true        | false
    }

    void "approveProjectRequest, should add user to list of usersThatAlreadyApproved and remove from list of usersThatNeedToApprove"() {
        given:
        User approvingUser = createUser()
        ProjectRequest projectRequest = createProjectRequest([:], [
                usersThatNeedToApprove  : [approvingUser, createUser()],
                usersThatAlreadyApproved: [createUser()],
        ])

        when:
        projectRequestService.approveProjectRequest(projectRequest, approvingUser)

        then:
        !projectRequest.state.usersThatNeedToApprove.contains(approvingUser)
        projectRequest.state.usersThatAlreadyApproved.contains(approvingUser)
    }

    @Unroll
    void "approveProjectRequest, should throw assertionError if user is not able to approve"() {
        given:
        User approvingUser = createUser()
        ProjectRequest projectRequest = createProjectRequest([:], [
                usersThatNeedToApprove  : [createUser()],
                usersThatAlreadyApproved: [createUser(), approvingUser],
        ])

        when:
        projectRequestService.approveProjectRequest(projectRequest, userIsNull ? null : approvingUser)

        then:
        thrown AssertionError

        where:
        userIsNull << [true, false]
    }

    void "deleteProjectRequest"() {
        given:
        ProjectRequest projectRequest = createProjectRequest()

        and:
        assert ProjectRequest.count == 1

        when:
        projectRequestService.deleteProjectRequest(projectRequest)

        then:
        ProjectRequest.count == 0
    }

    void "listAdditionalFieldValues"() {
        given:
        TextFieldValue textFieldValue = createTextFieldValue()
        DateFieldValue dateFieldValue = createDateFieldValue()
        DomainReferenceFieldValue domainReferenceFieldValue = createDomainReferenceFieldValue()
        SetValueField setValueField = createSetValueField()
        ProjectRequest projectRequest = createProjectRequest([
                projectFields: [textFieldValue, dateFieldValue, domainReferenceFieldValue, setValueField]
        ])

        when:
        Map<String, String> result = projectRequestService.listAdditionalFieldValues(projectRequest)

        then:
        result[textFieldValue.definition.id as String] == textFieldValue.displayValue
        result[dateFieldValue.definition.id as String] == dateFieldValue.displayValue
        result[domainReferenceFieldValue.definition.id as String] == domainReferenceFieldValue.displayValue
        result[setValueField.definition.id as String] == setValueField.displayValue
    }

    void "saveAdditionalFieldValuesForProjectRequest, should save values to project request"() {
        given:
        TextFieldDefinition textFieldDefinition = createTextFieldDefinition()
        IntegerFieldDefinition integerFieldDefinition = createIntegerFieldDefinition()
        ProjectRequest projectRequest = createProjectRequest()

        when:
        projectRequestService.saveAdditionalFieldValuesForProjectRequest("New text", textFieldDefinition.id as String, projectRequest)
        projectRequestService.saveAdditionalFieldValuesForProjectRequest("12", integerFieldDefinition.id as String, projectRequest)

        then:
        TextFieldValue.count == 1
        IntegerFieldValue.count == 1
        projectRequest.projectFields[0].definition == textFieldDefinition
        projectRequest.projectFields[1].definition == integerFieldDefinition
        projectRequest.projectFields[0].displayValue == "New text"
        projectRequest.projectFields[1].displayValue == "12"
    }

    @Unroll
    void "listAndFetchAbstractFields, should return right fields for operator #isOperator, projectType #projectType and page #projectPageType"() {
        given:
        createUserAndRoles()
        String role = isOperator ? OPERATOR : USER

        TextFieldDefinition textFdSourceIsOperator = createTextFieldDefinition([
                sourceOfData: ProjectSourceOfData.OPERATOR,
        ])
        FlagFieldDefinition flagFd = createFlagFieldDefinition()
        DateFieldDefinition dateFdIsLegacy = createDateFieldDefinition([
                legacy: true,
        ])
        DomainReferenceFieldDefinition domainReferenceFdForSequencing = createDomainReferenceFieldDefinition([
                fieldUseForSequencingProjects    : FieldExistenceType.REQUIRED,
                fieldUseForDataManagementProjects: FieldExistenceType.NOT_AVAILABLE,
        ])
        FlagFieldDefinition FlagFdOnlyForUserManagement = createFlagFieldDefinition([
                fieldUseForDataManagementProjects: FieldExistenceType.OPTIONAL,
                fieldUseForSequencingProjects    : FieldExistenceType.NOT_AVAILABLE,
        ])
        DateFieldDefinition dateFdHideOnConfig = createDateFieldDefinition([
                projectDisplayOnConfigPage: ProjectDisplayOnConfigPage.HIDE,
        ])

        projectRequestService.securityService = securityService

        when:
        List<AbstractFieldDefinition> result = doWithAuth(role) {
            projectRequestService.listAndFetchAbstractFields(projectType, projectPageType)
        }

        then:
        switch (caseNumber) {
            case 1:
                TestCase.assertContainSame(result, [flagFd, domainReferenceFdForSequencing, dateFdHideOnConfig])
                break
            case 2:
                TestCase.assertContainSame(result, [flagFd, FlagFdOnlyForUserManagement, dateFdHideOnConfig])
                break
            case 3:
                TestCase.assertContainSame(result, [textFdSourceIsOperator, flagFd, domainReferenceFdForSequencing, dateFdHideOnConfig])
                break
            case 4:
                TestCase.assertContainSame(result, [textFdSourceIsOperator, flagFd, FlagFdOnlyForUserManagement, dateFdHideOnConfig])
                break
            case 5:
                TestCase.assertContainSame(result, [textFdSourceIsOperator, flagFd, dateFdIsLegacy, domainReferenceFdForSequencing])
                break
            case 6:
                TestCase.assertContainSame(result, [textFdSourceIsOperator, flagFd, dateFdIsLegacy, FlagFdOnlyForUserManagement])
                break
            case 7:
                TestCase.assertContainSame(result, [flagFd, domainReferenceFdForSequencing, dateFdHideOnConfig])
                break
            case 8:
                TestCase.assertContainSame(result, [flagFd, FlagFdOnlyForUserManagement, dateFdHideOnConfig])
                break
            case 9:
                TestCase.assertContainSame(result, [textFdSourceIsOperator, flagFd, domainReferenceFdForSequencing, dateFdHideOnConfig])
                break
            case 10:
                TestCase.assertContainSame(result, [textFdSourceIsOperator, flagFd, FlagFdOnlyForUserManagement, dateFdHideOnConfig])
                break
            case 11:
                TestCase.assertContainSame(result, [textFdSourceIsOperator, flagFd, domainReferenceFdForSequencing])
                break
            case 12:
                TestCase.assertContainSame(result, [textFdSourceIsOperator, flagFd, FlagFdOnlyForUserManagement])
                break
        }

        where:
        caseNumber | isOperator | projectType                         | projectPageType
        1          | true       | Project.ProjectType.SEQUENCING      | ProjectPageType.PROJECT_REQUEST
        2          | true       | Project.ProjectType.USER_MANAGEMENT | ProjectPageType.PROJECT_REQUEST
        3          | true       | Project.ProjectType.SEQUENCING      | ProjectPageType.PROJECT_CREATION
        4          | true       | Project.ProjectType.USER_MANAGEMENT | ProjectPageType.PROJECT_CREATION
        5          | true       | Project.ProjectType.SEQUENCING      | ProjectPageType.PROJECT_CONFIG
        6          | true       | Project.ProjectType.USER_MANAGEMENT | ProjectPageType.PROJECT_CONFIG
        7          | false      | Project.ProjectType.SEQUENCING      | ProjectPageType.PROJECT_REQUEST
        8          | false      | Project.ProjectType.USER_MANAGEMENT | ProjectPageType.PROJECT_REQUEST
        9          | false      | Project.ProjectType.SEQUENCING      | ProjectPageType.PROJECT_CREATION
        10         | false      | Project.ProjectType.USER_MANAGEMENT | ProjectPageType.PROJECT_CREATION
        11         | false      | Project.ProjectType.SEQUENCING      | ProjectPageType.PROJECT_CONFIG
        12         | false      | Project.ProjectType.USER_MANAGEMENT | ProjectPageType.PROJECT_CONFIG
    }

    void "getCurrentOwnerDisplayName, should return real name if user is not an admin user"() {
        given:
        User nonAdminUser = createUser()
        ProjectRequest projectRequest = createProjectRequest([state: createProjectRequestPersistentState([currentOwner: nonAdminUser])])

        when:
        String currentUsername = projectRequestService.getCurrentOwnerDisplayName(projectRequest)

        then:
        1 * projectRequestService.securityService.hasCurrentUserAdministrativeRoles() >> false
        0 * _

        then:
        currentUsername == nonAdminUser.username
    }

    void "getCurrentOwnerDisplayName, should return masked of admins for non admin users"() {
        given:
        createUserAndRoles()
        User adminUser = getUser(ADMIN)
        ProjectRequest projectRequest = createProjectRequest([state: createProjectRequestPersistentState([currentOwner: adminUser])])

        when:
        String username = projectRequestService.getCurrentOwnerDisplayName(projectRequest)

        then:
        1 * projectRequestService.securityService.hasCurrentUserAdministrativeRoles() >> false
        1 * projectRequestService.processingOptionService.findOptionAsString(ProcessingOption.OptionName.HELP_DESK_TEAM_NAME) >> emailSenderSalutation
        0 * _

        then:
        username != adminUser.username
        username != adminUser.realName
    }

    void "getCurrentOwnerDisplayName, should return not masked username of admins for for admin users"() {
        given:
        createUserAndRoles()
        User adminUser = getUser(ADMIN)
        ProjectRequest projectRequest = createProjectRequest([state: createProjectRequestPersistentState([currentOwner: adminUser])])

        when:
        String username = projectRequestService.getCurrentOwnerDisplayName(projectRequest)

        then:
        1 * projectRequestService.securityService.hasCurrentUserAdministrativeRoles() >> true
        0 * _

        then:
        username == adminUser.username

        where:
        authority << Role.ADMINISTRATIVE_ROLES
    }

    void "getAllApproved, should return all approved project requests"() {
        given:
        ProjectRequest projectRequest1 = createProjectRequest([:], [beanName: "approved"])
        ProjectRequest projectRequest2 = createProjectRequest([:], [beanName: "approved"])
        createProjectRequest([:], [beanName: "created"])
        createProjectRequest()

        expect:
        TestCase.assertContainSame(projectRequestService.allApproved, [projectRequest1, projectRequest2])
    }

    @Unroll
    void "getDepartmentIfExists, should return #resultText, when processing option is #optionEnabled and additional field is #additionalFieldText created"() {
        given:
        projectRequestService.processingOptionService = Mock(ProcessingOptionService) {
            1 * findOptionAsString(ProcessingOption.OptionName.ENABLE_PROJECT_REQUEST_PI) >> optionEnabled
        }
        Department department = createDepartment(result ? [ouNumber: result] : [:])
        AbstractFieldDefinition additionalField = additionalFieldExists ?
                createTextFieldDefinition([name: 'Organizational Unit']) :
                createTextFieldDefinition([name: 'Other Additional Field'])

        Map<String, String> additionFieldValue = [(additionalField.id.toString()): department.ouNumber]

        expect:
        projectRequestService.getDepartmentIfExists(additionFieldValue) == result

        where:
        resultText   | result    | optionEnabled | additionalFieldText | additionalFieldExists
        "department" | "49283kd" | true          | ""                  | true
        "department" | null      | true          | "not"               | false
        "null"       | null      | false         | ""                  | true
        "null"       | null      | false         | "not"               | false
    }

    @Unroll
    void "addDepartmentHeadsToProject, should #text add existing department heads, when processing option is #departmentsEnabled"() {
        given:
        createAllBasicProjectRoles()
        projectRequestService.processingOptionService = Mock(ProcessingOptionService)
        projectRequestService.departmentService = Mock(DepartmentService)
        ProjectRequest projectRequest = createProjectRequest()
        User head1 = createUser()
        User head2 = createUser()
        Department department = createDepartment(['departmentHeads': [head1, head2]])
        AbstractFieldDefinition additionalField = createTextFieldDefinition([name: 'Organizational Unit'])

        Map<String, String> additionFieldValue = [(additionalField.id.toString()): department.ouNumber]
        Set<ProjectRole> pi = ProjectRole.findAllByName(ProjectRole.Basic.PI.name()) as Set<ProjectRole>

        when:
        projectRequestService.addDepartmentHeadsToProject(additionFieldValue, projectRequest)

        then:
        (departmentsEnabled ? 1 : 0) * projectRequestService.userProjectRoleService.createUserProjectRole(head1, projectRequest.project, pi, [
                accessToOtp           : true,
                accessToFiles         : false,
                manageUsers           : true,
                manageUsersAndDelegate: true,
                receivesNotifications : true,
        ])
        (departmentsEnabled ? 1 : 0) * projectRequestService.userProjectRoleService.createUserProjectRole(head2, projectRequest.project, pi, [
                accessToOtp           : true,
                accessToFiles         : false,
                manageUsers           : true,
                manageUsersAndDelegate: true,
                receivesNotifications : true,
        ])
        (departmentsEnabled ? 1 : 0) * projectRequestService.departmentService.getListOfHeadsForDepartment(department.ouNumber) >> [head1, head2]
        1 * projectRequestService.processingOptionService.findOptionAsString(ProcessingOption.OptionName.ENABLE_PROJECT_REQUEST_PI) >> departmentsEnabled
        0 * _

        where:
        text  | departmentsEnabled
        ""    | true
        "not" | false
    }
}
