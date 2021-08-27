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
package de.dkfz.tbi.otp.project

import grails.gorm.transactions.Transactional
import grails.plugin.springsecurity.SpringSecurityUtils
import grails.validation.ValidationException
import grails.web.mapping.LinkGenerator
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.Errors

import de.dkfz.tbi.otp.OtpRuntimeException
import de.dkfz.tbi.otp.administration.LdapService
import de.dkfz.tbi.otp.administration.UserService
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.ngsdata.UserProjectRoleService
import de.dkfz.tbi.otp.project.ProjectRequestUser.ApprovalState
import de.dkfz.tbi.otp.project.additionalField.*
import de.dkfz.tbi.otp.security.*
import de.dkfz.tbi.otp.utils.*

import java.time.LocalDate

@Transactional
class ProjectRequestService {

    AuditLogService auditLogService
    @Autowired
    LinkGenerator linkGenerator
    LdapService ldapService
    MailHelperService mailHelperService
    MessageSourceService messageSourceService
    @Autowired
    ProcessingOptionService processingOptionService
    SecurityService securityService
    UserProjectRoleService userProjectRoleService
    UserService userService
    RolesService rolesService
    ProjectRequestUserService projectRequestUserService

    List<ProjectRequest> getAllProjectRequests() {
        return ProjectRequest.list()
    }

    // Returns all requests for checking regardless which users they belong to
    List<ProjectRequest> getSubmittedRequests() {
        return ProjectRequest.withCriteria {
            eq("status", ProjectRequest.Status.SUBMITTED)
        } as List<ProjectRequest>
    }

    List<ProjectRequest> getResolvedOfCurrentUser() {
        boolean isOperator = SpringSecurityUtils.ifAllGranted(Role.ROLE_OPERATOR)
        return isOperator ? ProjectRequest.withCriteria {
                'in'("status", ProjectRequest.Status.values().findAll { it.resolvedStatus })
            } as List<ProjectRequest> : getRequestsHelper(true)
    }

    List<ProjectRequest> getUnresolvedRequestsOfUser() {
        return getRequestsHelper(false).findAll { ProjectRequest projectRequest ->
                projectRequest.status == ProjectRequest.Status.WAITING_FOR_APPROVER
            }
    }

    List<ProjectRequest> getRequestsHelper(boolean requestsAreResolved) {
        return (ProjectRequest.withCriteria {
            or {
                users {
                    'in'("user", securityService.currentUserAsUser)
                }
                eq("requester", securityService.currentUserAsUser)
            }
            'in'("status", ProjectRequest.Status.values().findAll { it.resolvedStatus == requestsAreResolved })
        } as List<ProjectRequest>).unique()
    }

    List<AbstractFieldDefinition> listAndFetchAbstractFields(Project.ProjectType projectType, ProjectPageType page) {
        boolean isOperator = SpringSecurityUtils.ifAllGranted(Role.ROLE_OPERATOR)
        List<AbstractFieldDefinition> fieldDefinitions = []
        AbstractFieldDefinition.list().each {
            if ((projectType == Project.ProjectType.SEQUENCING && it.fieldUseForSequencingProjects != FieldExistenceType.NOT_AVAILABLE) ||
                    (projectType == Project.ProjectType.USER_MANAGEMENT && it.fieldUseForDataManagementProjects != FieldExistenceType.NOT_AVAILABLE)) {
                if (page == ProjectPageType.PROJECT_REQUEST) {
                    if (!it.legacy && it.sourceOfData == ProjectSourceOfData.REQUESTER) {
                        fieldDefinitions << it
                    }
                } else if (page == ProjectPageType.PROJECT_CONFIG && it.projectDisplayOnConfigPage != ProjectDisplayOnConfigPage.HIDE) {
                    if ((it.projectDisplayOnConfigPage == ProjectDisplayOnConfigPage.SHOW && (isOperator || (!isOperator && !it.legacy)))
                            || (it.projectDisplayOnConfigPage == ProjectDisplayOnConfigPage.ONLY_FOR_OPERATOR && isOperator)) {
                        fieldDefinitions << it
                    }
                } else if (page == ProjectPageType.PROJECT_CREATION && !it.legacy) {
                    fieldDefinitions << it
                }
            }
        }
        return fieldDefinitions.sort { a, b ->
            a.sortNumber <=> b.sortNumber ?: a.name.compareToIgnoreCase(b.name)
        }
    }

    Map<String, String> listAdditionalFieldValues(ProjectPropertiesGivenWithRequest projectRequest) {
        return projectRequest.projectFields.collectEntries { AbstractFieldValue afv ->
            [(afv.definition.id.toString()) : afv.displayValue]
        }
    }

    ProjectRequest create(ProjectRequestCreationCommand cmd) throws SwitchedUserDeniedException {
        securityService.ensureNotSwitchedUser()

        ProjectRequest req = new ProjectRequest(
                name: cmd.name,
                description: cmd.description,
                keywords: cmd.keywords as Set,
                organizationalUnit: cmd.organizationalUnit,
                costCenter: cmd.costCenter,
                grantId: cmd.grantId,
                fundingBody: cmd.fundingBody,
                endDate: cmd.endDate,
                storageUntil: resolveStoragePeriodToLocalDate(cmd.storagePeriod, cmd.storageUntil),
                relatedProjects: cmd.relatedProjects,
                tumorEntity: cmd.tumorEntity,
                speciesWithStrain: cmd.speciesWithStrain,
                customSpeciesWithStrain: cmd.customSpeciesWithStrain,
                projectType: cmd.projectType,
                sequencingCenter: cmd.sequencingCenter,
                approxNoOfSamples: cmd.approxNoOfSamples,
                seqTypes: cmd.seqTypes,
                comments: cmd.comments,

                requester: securityService.currentUserAsUser,
                users: projectRequestUserService.createProjectRequestUsersFromCommands(cmd.users),
        )
        req.save(flush: true)

        if (cmd.additionalFieldValue) {
            cmd.additionalFieldValue.each { entry ->
                saveAdditionalFieldValuesForProjectRequest(entry.value, entry.key, req)
            }
        }

        sendEmailOnMaintenanceApproval(req)
        logAction(req, "request created")
        return req
    }

    void edit(EditProjectRequestCommand cmd) throws SwitchedUserDeniedException {
        securityService.ensureNotSwitchedUser()
        ProjectRequest projectRequest = cmd.request

        ensureEligibleToEdit(projectRequest)

        projectRequest.with {
            name = cmd.name
            description = cmd.description
            keywords = cmd.keywords as Set
            organizationalUnit = cmd.organizationalUnit
            costCenter = cmd.costCenter
            grantId = cmd.grantId
            fundingBody = cmd.fundingBody
            endDate = cmd.endDate
            storageUntil = resolveStoragePeriodToLocalDate(cmd.storagePeriod, cmd.storageUntil)
            relatedProjects = cmd.relatedProjects
            tumorEntity = cmd.tumorEntity
            speciesWithStrain = cmd.speciesWithStrain
            customSpeciesWithStrain = cmd.customSpeciesWithStrain
            projectType = cmd.projectType
            sequencingCenter = cmd.sequencingCenter
            approxNoOfSamples = cmd.approxNoOfSamples
            seqTypes = cmd.seqTypes
            comments = cmd.comments

            users = projectRequestUserService.createProjectRequestUsersFromCommands(cmd.users)
        }
        projectRequest.save(flush: true)

        if (cmd.additionalFieldValue) {
            cmd.additionalFieldValue.each { entry ->
                boolean fieldDoesNotExist = true
                projectRequest.projectFields.each { AbstractFieldValue abstractFieldValue ->
                    if (Long.toString(abstractFieldValue.definition.id) == entry.key) {
                        fieldDoesNotExist = false
                        updateAdditionalFieldValuesForProjectRequest(entry.value, entry.key, projectRequest)
                    }
                }
                if (fieldDoesNotExist) {
                    saveAdditionalFieldValuesForProjectRequest(entry.value, entry.key, projectRequest)
                }
            }
        }

        projectRequest.status = ProjectRequest.Status.SUBMITTED
        //send email to operators first
        sendEmailOnMaintenanceApproval(projectRequest)
        logAction(projectRequest, "request edited")
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    void updateAdditionalFieldValuesForProjectRequest(String fieldValue, String fieldId, ProjectRequest projectRequest) {
        projectRequest.projectFields.each { AbstractFieldValue afv ->
            if (afv.definition.id.toString() == fieldId) {
                if (afv.definition.projectFieldType == ProjectFieldType.TEXT) {
                    TextFieldValue tfv = afv
                    tfv.textValue = fieldValue
                    tfv.save(flush: true)
                } else if (afv.definition.projectFieldType == ProjectFieldType.INTEGER) {
                    IntegerFieldValue ifv = afv
                    ifv.integerValue = fieldValue.toInteger()
                    ifv.save(flush: true)
                }
                projectRequest.save(flush: true)
            }
        }
    }

    private static LocalDate resolveStoragePeriodToLocalDate(StoragePeriod storagePeriod, LocalDate given) {
        switch (storagePeriod) {
            case StoragePeriod.USER_DEFINED:
                return given
            case StoragePeriod.TEN_YEARS:
                return LocalDate.now().plusYears(10)
            case StoragePeriod.INFINITELY:
                return null
            default:
                return null
        }
    }

    ProjectRequest get(Long l) {
        ProjectRequest req = ProjectRequest.get(l)
        User currentUser = securityService.currentUserAsUser
        if (req && (rolesService.isAdministrativeUser(currentUser) || isUserPartOfRequest(currentUser, req))) {
            return req
        }
        return null
    }

    List<ProjectRequestUser> getApproversOfProjectRequest(ProjectRequest request) {
        return request.users.toList().findAll { it.approver }
    }

    boolean isUserPartOfRequest(User user, ProjectRequest request) {
        return request.requester == user || request.users.find { it.user == user }
    }

    boolean isUserEligibleToClose(User user, ProjectRequest request) {
        return request.requester == user
    }

    boolean isCurrentUserEligibleToClose(ProjectRequest request) {
        return isUserEligibleToClose(securityService.currentUserAsUser, request)
    }

    void ensureEligibleToClose(ProjectRequest request) {
        if (!(isCurrentUserEligibleToClose(request) && request.status.editableStatus)) {
            throw new AccessDeniedException("User '${securityService.currentUserAsUser}' is not eligible to close this request")
        }
    }

    boolean isUserEligibleToEdit(User user, ProjectRequest request) {
        return request.requester == user
    }

    boolean isCurrentUserEligibleToEdit(ProjectRequest request) {
        return isUserEligibleToEdit(securityService.currentUserAsUser, request)
    }

    void ensureEligibleToEdit(ProjectRequest request) {
        if (!(isCurrentUserEligibleToEdit(request) && request.status.editableStatus)) {
            throw new AccessDeniedException("User '${securityService.currentUserAsUser}' is not eligible to edit this request")
        }
    }

    boolean isUserEligibleApproverForRequest(User user, ProjectRequest request) {
        return request.users.find { it.user == user && it.approver } ||
            user.authorities.any { it.authority in Role.ADMINISTRATIVE_ROLES }
    }

    boolean isCurrentUserEligibleApproverForRequest(ProjectRequest request) {
        return isUserEligibleApproverForRequest(securityService.currentUserAsUser, request)
    }

    boolean confirmationRequired(ProjectRequest request) {
        return isUserEligibleApproverForRequest(securityService.currentUserAsUser, request) &&
                request.status != ProjectRequest.Status.SUBMITTED
    }

    void ensureApprovalEligible(ProjectRequest request) {
        if (!(isCurrentUserEligibleApproverForRequest(request) && request.status.editableStatus)) {
            throw new AccessDeniedException("User '${securityService.currentUserAsUser}' is not eligible to approve this request")
        }
    }

    void ensureTermsAndConditions(boolean confirmConsent, boolean confirmRecordOfProcessingActivities) {
        if ([confirmConsent, confirmRecordOfProcessingActivities].any { !it }) {
            throw new OtpRuntimeException("Invalid state, conditions were not accepted")
        }
    }

    boolean allProjectRequestUsersInState(final ProjectRequest projectRequest, final ApprovalState state) {
        return projectRequest.users.findAll { it.approver }.every { it.approvalState == state }
    }

    Errors approveRequest(ProjectRequest request, String comments, boolean confirmConsent = false, boolean confirmRecordOfProcessingActivities = false)
        throws SwitchedUserDeniedException {
        try {
            securityService.ensureNotSwitchedUser()
            ensureApprovalEligible(request)

            if (comments) {
                setComments(request, comments)
            }

            if (request.status == ProjectRequest.Status.SUBMITTED) {
                setStatus(request, ProjectRequest.Status.WAITING_FOR_APPROVER)
                sendEmailOnEdit(request)
                logAction(request, "request approved by OTP Maintenance")
            } else {
                ensureTermsAndConditions(confirmConsent, confirmRecordOfProcessingActivities)
                projectRequestUserService.setApprovalStateAsCurrentUser(request, ApprovalState.APPROVED)
                logAction(request, "user approves request")

                if (allProjectRequestUsersInState(request, ApprovalState.APPROVED)) {
                    setStatus(request, ProjectRequest.Status.WAITING_FOR_OPERATOR)
                    sendEmailOnCompleteApproval(request)
                    logAction(request, "request approved")
                }
            }
        } catch (ValidationException e) {
            return e.errors
        }
        return null
    }

    Errors denyRequest(ProjectRequest request, String comments) throws SwitchedUserDeniedException {
        try {
            securityService.ensureNotSwitchedUser()
            ensureApprovalEligible(request)

            if (comments) {
                setComments(request, comments)
            }

            if (request.status == ProjectRequest.Status.SUBMITTED) {
                setStatus(request, ProjectRequest.Status.DENIED_BY_APPROVER)
                logAction(request, "request denied by OTP Maintenance")
            } else {
                projectRequestUserService.setApprovalStateAsCurrentUser(request, ApprovalState.DENIED)
                logAction(request, "user denies request")

                if (allProjectRequestUsersInState(request, ApprovalState.DENIED)) {
                    setStatus(request, ProjectRequest.Status.DENIED_BY_APPROVER)
                    logAction(request, "request denied")
                }
            }
        } catch (ValidationException e) {
            return e.errors
        }
        return null
    }

    Errors closeRequest(ProjectRequest request) throws SwitchedUserDeniedException {
        try {
            securityService.ensureNotSwitchedUser()
            ensureEligibleToClose(request)

            setStatus(request, ProjectRequest.Status.CLOSED)
            logAction(request, "request closed")
        } catch (ValidationException e) {
            return e.errors
        }
        return null
    }

    ProjectRequest setStatus(ProjectRequest request, ProjectRequest.Status status, Project project = null) {
        if (status == ProjectRequest.Status.PROJECT_CREATED) {
            assert project: "Status expects a project, but none is given"
            request.project = project
        }
        request.status = status
        request.save(flush: true)
    }

    ProjectRequest setComments(ProjectRequest request, String comments) {
        request.comments = comments
        request.save(flush: true)
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    void finish(ProjectRequest request, Project project) {
        setStatus(request, ProjectRequest.Status.PROJECT_CREATED, project)
    }

    boolean requesterIsEligibleToAccept(ProjectRequest projectRequest) {
        return isUserEligibleApproverForRequest(projectRequest.requester, projectRequest)
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    void addProjectRequestUsersToProject(ProjectRequest projectRequest) {
        projectRequest.users.each { ProjectRequestUser projectRequestUser ->
            projectRequestUserService.toUserProjectRole(projectRequest.project, projectRequestUser)
        }
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    ProjectRequest findProjectRequestByProject(Project project) {
        return CollectionUtils.atMostOneElement(ProjectRequest.findAllByProject(project))
    }

    void saveAdditionalFieldValuesForProjectRequest(String fieldValue, String fieldId, ProjectRequest projectRequest) {
        AbstractFieldDefinition afd = AbstractFieldDefinition.findById(fieldId as Long)
        if (afd.projectFieldType == ProjectFieldType.TEXT) {
            TextFieldValue tfv = new TextFieldValue()
            tfv.definition = afd
            tfv.textValue = fieldValue
            tfv.save(flush: true)
            projectRequest.projectFields.add(tfv)
        } else if (afd.projectFieldType == ProjectFieldType.INTEGER) {
            IntegerFieldValue ifv = new IntegerFieldValue()
            ifv.definition = afd
            ifv.integerValue = fieldValue.toInteger()
            ifv.save(flush: true)
            projectRequest.projectFields.add(ifv)
        }
        projectRequest.save(flush: true)
    }

    void sendEmailOnMaintenanceApproval(ProjectRequest request) {
        String link = linkGenerator.link(
                controller: "projectRequest",
                action: "view",
                absolute: true,
                id: request.id,
        )
        String message = messageSourceService.createMessage("notification.template.projectRequest.submit.body", [
                requester  : request.requester.realName,
                projectName: request.name,
                link       : link,
        ])
        String subject = messageSourceService.createMessage("notification.template.projectRequest.submit.subject", [
                projectName: request.name,
        ])
        List<String> recipients = [processingOptionService.findOptionAsString(ProcessingOption.OptionName.EMAIL_OTP_MAINTENANCE)]
        List<String> ccs = [mailHelperService.emailRecipientNotification]
        mailHelperService.sendEmail(subject, message, recipients, ccs)
    }

    void sendEmailOnCompleteApproval(ProjectRequest request) {
        String link = linkGenerator.link(
                controller: "projectCreation",
                action: "index",
                absolute: true,
                params: [
                        'projectRequest.id': request.id,
                ]
        )
        String message = messageSourceService.createMessage("notification.template.projectRequest.create.body", [
                requester  : request.requester.realName,
                projectName: request.name,
                approvers  : getApproversOfProjectRequest(request)*.user.join("\n"),
                link       : link,
        ])
        String subject = messageSourceService.createMessage("notification.template.projectRequest.create.subject", [
                projectName: request.name,
        ])
        mailHelperService.sendEmail(subject, message, mailHelperService.emailRecipientNotification)
    }

    void sendEmailOnEdit(ProjectRequest request) {
        String link = linkGenerator.link(
                controller: "projectRequest",
                action: "view",
                absolute: true,
                id: request.id,
        )
        String message = messageSourceService.createMessage("notification.template.projectRequest.edit.body", [
                requester  : request.requester.realName,
                projectName: request.name,
                link       : link,
        ])
        String subject = messageSourceService.createMessage("notification.template.projectRequest.edit.subject", [
                projectName: request.name,
        ])
        List<String> recipients = getApproversOfProjectRequest(request)*.user*.email
        List<String> ccs = [mailHelperService.emailRecipientNotification]
        mailHelperService.sendEmail(subject, message, recipients, ccs)
    }

    AuditLog logAction(ProjectRequest request, String description) {
        String staticLogPrefix = "[Request ${request.id}: ${request.name}]"
        return auditLogService.logAction(AuditLog.Action.PROJECT_REQUEST, "${staticLogPrefix} ${description}")
    }
}
