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
package de.dkfz.tbi.otp.project.projectRequest

import grails.gorm.transactions.Rollback
import grails.plugin.springsecurity.SpringSecurityUtils
import grails.testing.mixin.integration.Integration
import grails.web.mapping.LinkGenerator
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.domainFactory.ProjectFieldsDomainFactory
import de.dkfz.tbi.otp.domainFactory.UserDomainFactory
import de.dkfz.tbi.otp.project.*
import de.dkfz.tbi.otp.project.additionalField.*
import de.dkfz.tbi.otp.searchability.Keyword
import de.dkfz.tbi.otp.security.*
import de.dkfz.tbi.otp.utils.MailHelperService
import de.dkfz.tbi.otp.utils.MessageSourceService

import java.time.LocalDate

@Integration
@Rollback
class ProjectRequestServiceIntegrationSpec extends Specification implements UserDomainFactory, UserAndRoles, ProjectFieldsDomainFactory {

    ProjectRequestService projectRequestService
    ProcessingOptionService processingOptionService

    final String subject = "subject"
    final String body = "body"
    final String link = "link"
    final String comment = "comment"
    String emailSenderSalutation

    void setup() {
        emailSenderSalutation = processingOptionService.findOptionAsString(ProcessingOption.OptionName.EMAIL_SENDER_SALUTATION)
    }

    void "sendSubmitEmail"() {
        given:
        final User requester = createUser()
        final User pi1 = createUser()
        final User pi2 = createUser()
        final List<User> users = [pi1, pi2]
        final ProjectRequest request = createProjectRequest([requester: requester], [usersThatNeedToApprove: users])

        projectRequestService.messageSourceService = Mock(MessageSourceService)
        projectRequestService.mailHelperService = Mock(MailHelperService)
        projectRequestService.linkGenerator = Mock(LinkGenerator)

        when:
        projectRequestService.sendSubmitEmail(request)

        then:
        1 * projectRequestService.messageSourceService.createMessage(_) >> subject
        1 * projectRequestService.linkGenerator.link(_) >> link
        1 * projectRequestService.messageSourceService.createMessage(_, [
                link         : link,
                requester    : "${requester.username} (${requester.realName})",
                teamSignature: emailSenderSalutation,
        ]) >> body
        1 * projectRequestService.mailHelperService.sendEmail(subject, body, [requester.email], users*.email)
        0 * _
    }

    void "sendOperatorRejectEmail"() {
        given:
        final User requester = createUser()
        final User pi1 = createUser()
        final User pi2 = createUser()
        final List<User> users = [pi1, pi2]
        final ProjectRequest request = createProjectRequest([requester: requester], [usersThatNeedToApprove: users])

        projectRequestService.messageSourceService = Mock(MessageSourceService)
        projectRequestService.mailHelperService = Mock(MailHelperService)
        projectRequestService.linkGenerator = Mock(LinkGenerator)

        when:
        projectRequestService.sendOperatorRejectEmail(request, comment)

        then:
        1 * projectRequestService.messageSourceService.createMessage(_) >> subject
        1 * projectRequestService.linkGenerator.link(_) >> link
        1 * projectRequestService.messageSourceService.createMessage(_, [
                requester    : "${requester.username} (${requester.realName})",
                comment      : comment,
                link         : link,
                teamSignature: emailSenderSalutation,
        ]) >> body
        1 * projectRequestService.mailHelperService.sendEmail(subject, body, [requester.email], users*.email)
        0 * _
    }

    void "sendPassOnEmail"() {
        given:
        final User requester = createUser()
        final User pi1 = createUser()
        final User pi2 = createUser()
        final List<User> users = [pi1, pi2]
        final ProjectRequest request = createProjectRequest([requester: requester], [usersThatNeedToApprove: users])

        projectRequestService.messageSourceService = Mock(MessageSourceService)
        projectRequestService.mailHelperService = Mock(MailHelperService)
        projectRequestService.linkGenerator = Mock(LinkGenerator)

        when:
        projectRequestService.sendPassOnEmail(request)

        then:
        1 * projectRequestService.messageSourceService.createMessage(_) >> subject
        1 * projectRequestService.linkGenerator.link(_) >> link
        1 * projectRequestService.messageSourceService.createMessage(_, [
                projectAuthorities: users*.username.join(", "),
                requester         : "${requester.username} (${requester.realName})",
                projectName       : request.name,
                link              : link,
                teamSignature     : emailSenderSalutation,
        ]) >> body
        1 * projectRequestService.mailHelperService.sendEmail(subject, body, users*.email, [requester.email])
        0 * _
    }

    void "sendPiRejectEmail"() {
        given:
        final User requester = createUser()
        final User pi1 = createUser()
        final User pi2 = createUser()
        final List<User> users = [pi1, pi2]
        final ProjectRequest request = createProjectRequest([requester: requester], [usersThatNeedToApprove: users])

        projectRequestService.messageSourceService = Mock(MessageSourceService)
        projectRequestService.mailHelperService = Mock(MailHelperService)
        projectRequestService.linkGenerator = Mock(LinkGenerator)
        projectRequestService.securityService = Mock(SecurityService)

        when:
        projectRequestService.sendPiRejectEmail(request, comment)

        then:
        1 * projectRequestService.messageSourceService.createMessage(_) >> subject
        1 * projectRequestService.linkGenerator.link(_) >> link
        1 * projectRequestService.securityService.currentUserAsUser >> pi1
        1 * projectRequestService.messageSourceService.createMessage(_, [
                requester       : "${requester.username} (${requester.realName})",
                projectName     : request.name,
                projectAuthority: pi1.username,
                comment         : comment,
                link            : link,
                teamSignature   : emailSenderSalutation,
        ]) >> body
        1 * projectRequestService.mailHelperService.sendEmail(subject, body, [requester.email], users*.email)
        0 * _
    }

    void "sendApprovedEmail"() {
        given:
        final User requester = createUser()
        final User pi1 = createUser()
        final User pi2 = createUser()
        final List<User> users = [pi1, pi2]
        final ProjectRequest request = createProjectRequest([requester: requester], [usersThatNeedToApprove: users])

        projectRequestService.messageSourceService = Mock(MessageSourceService)
        projectRequestService.mailHelperService = Mock(MailHelperService)
        projectRequestService.linkGenerator = Mock(LinkGenerator)

        when:
        projectRequestService.sendApprovedEmail(request)

        then:
        1 * projectRequestService.messageSourceService.createMessage(_) >> subject
        1 * projectRequestService.messageSourceService.createMessage(_, [
                requester    : "${requester.username} (${requester.realName})",
                projectName  : request.name,
                teamSignature: emailSenderSalutation,
        ]) >> body
        1 * projectRequestService.mailHelperService.sendEmail(subject, body, (users*.email + [requester.email]))
        0 * _
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

        projectRequestService.messageSourceService = Mock(MessageSourceService)
        projectRequestService.mailHelperService = Mock(MailHelperService)
        projectRequestService.linkGenerator = Mock(LinkGenerator)
        projectRequestService.securityService = Mock(SecurityService)

        when:
        projectRequestService.sendPartiallyApprovedEmail(request)

        then:
        1 * projectRequestService.messageSourceService.createMessage(_) >> subject
        1 * projectRequestService.securityService.currentUserAsUser >> pi1
        1 * projectRequestService.messageSourceService.createMessage(_, [
                requester       : "${requester.username} (${requester.realName})",
                projectName     : request.name,
                projectAuthority: pi1.username,
                teamSignature   : emailSenderSalutation,
        ]) >> body
        1 * projectRequestService.mailHelperService.sendEmail(subject, body, [requester.email], (usersThatNeedToApprove + usersThatAlreadyApproved)*.email)
    }

    void "sendDeleteEmail"() {
        given:
        final User requester = createUser()
        final User pi1 = createUser()
        final User pi2 = createUser()
        final List<User> users = [pi1, pi2]
        final ProjectRequest request = createProjectRequest([requester: requester], [usersThatNeedToApprove: users])

        projectRequestService.messageSourceService = Mock(MessageSourceService)
        projectRequestService.mailHelperService = Mock(MailHelperService)
        projectRequestService.linkGenerator = Mock(LinkGenerator)
        projectRequestService.securityService = Mock(SecurityService)

        when:
        projectRequestService.sendDeleteEmail(request)

        then:
        1 * projectRequestService.messageSourceService.createMessage(_) >> subject
        1 * projectRequestService.securityService.currentUserAsUser >> pi1
        1 * projectRequestService.messageSourceService.createMessage(_, [
                requester         : "${requester.username} (${requester.realName})",
                projectAuthorities: users*.username.join(", "),
                projectName       : request.name,
                deletingUser      : pi1,
                teamSignature     : emailSenderSalutation,
        ]) >> body
        1 * projectRequestService.mailHelperService.sendEmail(subject, body, users*.email + [requester.email])
    }

    void "sendDraftCreateEmail"() {
        given:
        final String link = "link"
        final User requester = createUser()
        final ProjectRequest request = createProjectRequest([
                requester: requester,
        ])

        projectRequestService.messageSourceService = Mock(MessageSourceService)
        projectRequestService.mailHelperService = Mock(MailHelperService)
        projectRequestService.linkGenerator = Mock(LinkGenerator)

        when:
        projectRequestService.sendDraftCreateEmail(request)

        then:
        1 * projectRequestService.messageSourceService.createMessage(_) >> subject
        1 * projectRequestService.linkGenerator.link(_) >> link
        1 * projectRequestService.messageSourceService.createMessage(_, [
                requester    : "${requester.username} (${requester.realName})",
                link         : link,
                teamSignature: emailSenderSalutation,
        ]) >> body
        1 * projectRequestService.mailHelperService.sendEmail(subject, body, [requester.email])
        0 * _
    }

    void "sendDraftDeleteEmail"() {
        given:
        final User requester = createUser()
        final ProjectRequest request = createProjectRequest([
                requester: requester,
        ])

        projectRequestService.messageSourceService = Mock(MessageSourceService)
        projectRequestService.mailHelperService = Mock(MailHelperService)

        when:
        projectRequestService.sendDraftDeleteEmail(request)

        then:
        1 * projectRequestService.messageSourceService.createMessage(_) >> subject
        1 * projectRequestService.messageSourceService.createMessage(_, [
                requester    : "${requester.username} (${requester.realName})",
                projectName  : request.name,
                teamSignature: emailSenderSalutation,
        ]) >> body
        1 * projectRequestService.mailHelperService.sendEmail(subject, body, [requester.email])
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

        projectRequestService.messageSourceService = Mock(MessageSourceService)
        projectRequestService.mailHelperService = Mock(MailHelperService)
        projectRequestService.linkGenerator = Mock(LinkGenerator)
        projectRequestService.securityService = Mock(SecurityService)

        when:
        projectRequestService.sendPiEditedEmail(request)

        then:
        1 * projectRequestService.messageSourceService.createMessage(_) >> subject
        1 * projectRequestService.securityService.currentUserAsUser >> pi1
        1 * projectRequestService.linkGenerator.link(_) >> link
        1 * projectRequestService.messageSourceService.createMessage(_, [
                projectAuthorities: (usersThatNeedToApprove + usersThatAlreadyApproved)*.username.join(", "),
                projectAuthority  : pi1.username,
                projectName       : request.name,
                link              : link,
                teamSignature     : emailSenderSalutation,
        ]) >> body
        1 * projectRequestService.mailHelperService.sendEmail(subject, body, users*.email, [requester.email])
    }

    void "sendCreatedEmail"() {
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

        final String dirAnalysis = "/dirAnalysis"
        final String dirName = "dirName"
        final Project project = createProject([
                dirAnalysis: dirAnalysis,
                dirName    : dirName,
        ])
        final String ticketingSystemMail = "ticketingSystemMail"
        final String clusterTicketingSystemMail = "clusterTicketingSystemMail"
        final String additionalText = "additionalText"

        projectRequestService.messageSourceService = Mock(MessageSourceService)
        projectRequestService.mailHelperService = Mock(MailHelperService)
        projectRequestService.processingOptionService = Mock(ProcessingOptionService)
        projectRequestService.linkGenerator = Mock(LinkGenerator)

        when:
        projectRequestService.sendCreatedEmail(project, request)

        then:
        1 * projectRequestService.processingOptionService.findOptionAsString(ProcessingOption.OptionName.EMAIL_TICKET_SYSTEM) >> ticketingSystemMail
        1 * projectRequestService.processingOptionService.findOptionAsString(ProcessingOption.OptionName.EMAIL_CLUSTER_ADMINISTRATION) >> clusterTicketingSystemMail
        1 * projectRequestService.processingOptionService.findOptionAsString(ProcessingOption.OptionName.EMAIL_PROJECT_CREATION_FREETEXT) >> additionalText
        1 * projectRequestService.processingOptionService.findOptionAsString(ProcessingOption.OptionName.EMAIL_SENDER_SALUTATION) >> emailSenderSalutation
        2 * projectRequestService.linkGenerator.link(_) >> link
        1 * projectRequestService.messageSourceService.createMessage(_, [projectName: project.name]) >> subject
        1 * projectRequestService.messageSourceService.createMessage(_, [
                projectName               : project.name,
                projectLink               : link,
                projectDirectoryPath      : dirName,
                analysisDirectoryPath     : dirAnalysis,
                additionalText            : additionalText,
                userManagementLink        : link,
                clusterTicketingSystemMail: clusterTicketingSystemMail,
                ticketingSystemMail       : ticketingSystemMail,
                teamSignature             : emailSenderSalutation,
        ]) >> body
        1 * projectRequestService.mailHelperService.sendEmail(subject, body, (usersThatNeedToApprove + usersThatAlreadyApproved)*.email, [requester.email])
    }

    ProjectRequestCreationCommand createProjectRequestCreationCommand(Map properties = [:]) {
        return new ProjectRequestCreationCommand([
                name       : "projectRequest${nextId}",
                projectType: Project.ProjectType.SEQUENCING,
                description: "A description, that is long enough to full fill the requirements of the command object.",
                keywords   : [createKeyword(), createKeyword()],
                users      : [createProjectRequestUser()],
        ] + properties)
    }

    @Unroll
    void "saveProjectRequestFromCommand, should save projectRequest and translate all parameters"() {
        given:
        createAllBasicProjectRoles()
        User currentUser = createUser()
        List<Keyword> keywords = [createKeyword(), createKeyword()]
        Set<ProjectRequestUser> users = [createProjectRequestUser([projectRoles: [pi]]), createProjectRequestUser()]
        ProjectRequestPersistentState projectRequestPersistentState = createProjectRequestPersistentState()
        ProjectRequest projectRequest = projectRequestExists ? createProjectRequest([state: projectRequestPersistentState]) : null
        ProjectRequestCreationCommand cmd = new ProjectRequestCreationCommand([
                name                    : "projectRequest_${nextId}",
                description             : "A description, that is long enough to fulfill the required length of the command object.",
                seqTypes                : [createSeqType(), createSeqType()],
                approxNoOfSamples       : 12,
                storagePeriod           : storagePeriod,
                storageUntil            : storageUntil,
                customSpeciesWithStrains: ["customSpeciesWithStrains1", "customSpeciesWithStrains2"],
                sequencingCenters       : ["TestCenter", "TestCenter2"],
                projectRequest          : projectRequest,
                keywords                : keywords,
                comments                : "Comments for the request.",
                projectType             : Project.ProjectType.SEQUENCING,

        ])
        projectRequestService.securityService = Mock(SecurityService)
        projectRequestService.projectRequestUserService = Mock(ProjectRequestUserService)
        projectRequestService.securityService = Mock(SecurityService)
        projectRequestService.auditLogService.securityService = Mock(SecurityService)

        when:
        ProjectRequest result = projectRequestService.saveProjectRequestFromCommand(cmd)

        then:
        1 * projectRequestService.securityService.ensureNotSwitchedUser()
        1 * projectRequestService.projectRequestUserService.saveProjectRequestUsersFromCommands(_) >> users
        (projectRequestExists ? 0 : 1) * projectRequestService.securityService.currentUserAsUser >> currentUser
        1 * projectRequestService.auditLogService.securityService.trueCurrentUserAsUser >> currentUser
        0 * _
        ProjectRequest.count == 1
        result.name == cmd.name
        result.requester == (projectRequestExists ? projectRequest.requester : currentUser)
        result.users == users
        result.project == null
        projectRequestExists ? result.state == projectRequestPersistentState : result.state.beanName == "initial"
        result.customSpeciesWithStrains == cmd.customSpeciesWithStrains as Set
        result.keywords == cmd.keywords*.name as Set
        result.sequencingCenters == cmd.sequencingCenters as Set
        result.approxNoOfSamples == cmd.approxNoOfSamples
        result.seqTypes == cmd.seqTypes as Set
        result.comments == cmd.comments
        result.storageUntil == resultStorageUntil
        result.projectType == cmd.projectType

        where:
        storagePeriod              | storageUntil               | resultStorageUntil            | projectRequestExists
        StoragePeriod.TEN_YEARS    | null                       | LocalDate.now().plusYears(10) | true
        StoragePeriod.USER_DEFINED | new LocalDate(2000, 12, 1) | storageUntil                  | true
        StoragePeriod.INFINITELY   | null                       | null                          | false
    }

    @Unroll
    void "getRequestsUserIsInvolved, with #resolved parameter and #testDescription requests the user is involved in"() {
        given:
        createUserAndRoles()
        User currentUser = createUser()
        ProjectRequest prRequestedByUser = createProjectRequest([requester: currentUser])
        ProjectRequest prCreatedByOtherUser = createProjectRequest([:], [beanName: "created"])
        ProjectRequest prCreatedByUser = createProjectRequest([requester: currentUser], [beanName: "created"])
        ProjectRequest prDraftByOtherUser = createProjectRequest([:], [beanName: "draft"])
        ProjectRequest prDraftByUser = createProjectRequest([requester: currentUser], [beanName: "draft"])

        String role = isOperator ? OPERATOR : USER
        List<ProjectRequest> result = []

        projectRequestService.securityService = Mock(SecurityService)

        when:
        SpringSecurityUtils.doWithAuth(role) {
            result = projectRequestService.getRequestsUserIsInvolved(resolved)
        }

        then:
        projectRequestService.securityService.currentUserAsUser >> currentUser
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
        User currentUser = createUser()
        ProjectRequest prCheckOwnedByUser = createProjectRequest([:], [beanName: "check", currentOwner: currentUser])
        ProjectRequest prCreated = createProjectRequest([:], [beanName: "created"])
        ProjectRequest prApprovalUserNeedsToApprove = createProjectRequest([:], [beanName: "approval", usersThatNeedToApprove: [currentUser]])
        ProjectRequest prApproval = createProjectRequest([:], [beanName: "approval"])
        ProjectRequest prApproved = createProjectRequest([:], [beanName: "approved"])
        ProjectRequest prApprovedOwnedByUser = createProjectRequest([:], [beanName: "approved", currentOwner: currentUser])
        ProjectRequest prDraft = createProjectRequest([:], [beanName: "draft"])
        ProjectRequest prDraftRequestedByUser = createProjectRequest([requester: currentUser], [beanName: "draft", currentOwner: currentUser])
        ProjectRequest prPiEdit = createProjectRequest([:], [beanName: "piEdit"])
        ProjectRequest prPiEditOwnedByUser = createProjectRequest([:], [beanName: "piEdit", currentOwner: currentUser])
        ProjectRequest prRequesterEdit = createProjectRequest([:], [beanName: "requesterEdit"])
        ProjectRequest prRequesterEditRequestedByUser = createProjectRequest([requester: currentUser], [beanName: "requesterEdit"])
        List<ProjectRequest> projectRequests = [prCheckOwnedByUser, prCreated, prApprovalUserNeedsToApprove, prApproval, prApproved, prApprovedOwnedByUser,
                                                prDraft, prDraftRequestedByUser, prPiEdit, prPiEditOwnedByUser, prRequesterEdit, prRequesterEditRequestedByUser]
        String role = isOperator ? OPERATOR : USER
        List<ProjectRequest> result = []

        projectRequestService.projectRequestStateProvider.projectRequestStates.each {
            it.securityService = Mock(SecurityService) {
                getCurrentUserAsUser() >> currentUser
            }
        }

        when:
        SpringSecurityUtils.doWithAuth(role) {
            result = projectRequestService.sortRequestToBeHandledByUser(projectRequests)
        }

        then:
        if (isOperator) {
            TestCase.assertContainSame(result, [prCheckOwnedByUser, prApprovalUserNeedsToApprove, prApproved, prApprovedOwnedByUser,
                                                prDraftRequestedByUser, prPiEditOwnedByUser, prRequesterEditRequestedByUser])
        } else {
            TestCase.assertContainSame(result, [prApprovalUserNeedsToApprove, prDraftRequestedByUser, prPiEditOwnedByUser,
                                                prRequesterEditRequestedByUser])
        }

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

    void "addProjectRequestUsersToProject, should add project request Users to the project"() {
        given:
        createUserAndRoles()
        createAllBasicProjectRoles()
        Project project = createProject()
        ProjectRequestUser prUser1 = createProjectRequestUser([projectRoles: [pi]])
        ProjectRequestUser prUser2 = createProjectRequestUser([projectRoles: [coordinator]])
        ProjectRequestUser prUser3 = createProjectRequestUser([projectRoles: [bioinformatician, other]])
        ProjectRequest projectRequest = createProjectRequest([
                state  : createProjectRequestPersistentState([beanName: "created"]),
                project: project,
                users  : [prUser1, prUser2, prUser3],
        ])
        projectRequestService.projectRequestUserService = Mock(ProjectRequestUserService)

        when:
        SpringSecurityUtils.doWithAuth(OPERATOR) {
            projectRequestService.addProjectRequestUsersToProject(projectRequest)
        }

        then:
        1 * projectRequestService.projectRequestUserService.toUserProjectRole(project, prUser1)
        1 * projectRequestService.projectRequestUserService.toUserProjectRole(project, prUser2)
        1 * projectRequestService.projectRequestUserService.toUserProjectRole(project, prUser3)
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

        String role = isOperator ? OPERATOR : USER
        List<AbstractFieldDefinition> result = []

        when:
        SpringSecurityUtils.doWithAuth(role) {
            result = projectRequestService.listAndFetchAbstractFields(projectType, projectPageType)
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
}
