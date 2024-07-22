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

import grails.gorm.transactions.Transactional
import grails.web.mapping.LinkGenerator
import groovy.transform.CompileDynamic
import org.codehaus.groovy.runtime.InvokerHelper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.access.prepost.PreAuthorize

import de.dkfz.tbi.otp.ProjectSelectionService
import de.dkfz.tbi.otp.administration.MailHelperService
import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.ngsdata.ProjectRole
import de.dkfz.tbi.otp.ngsdata.UserProjectRole
import de.dkfz.tbi.otp.ngsdata.UserProjectRoleService
import de.dkfz.tbi.otp.project.*
import de.dkfz.tbi.otp.project.additionalField.*
import de.dkfz.tbi.otp.searchability.Keyword
import de.dkfz.tbi.otp.security.*
import de.dkfz.tbi.otp.security.user.DepartmentService
import de.dkfz.tbi.otp.security.user.SwitchedUserDeniedException
import de.dkfz.tbi.otp.security.user.UserService
import de.dkfz.tbi.otp.utils.*
import de.dkfz.tbi.otp.utils.exceptions.OtpRuntimeException

import java.time.LocalDate

@Transactional
class ProjectRequestService {

    @Autowired
    LinkGenerator linkGenerator

    AuditLogService auditLogService
    MessageSourceService messageSourceService
    MailHelperService mailHelperService
    ProjectRequestUserService projectRequestUserService
    ProjectRequestPersistentStateService projectRequestPersistentStateService
    ProjectRequestStateProvider projectRequestStateProvider
    ProcessingOptionService processingOptionService
    SecurityService securityService
    UserProjectRoleService userProjectRoleService
    ConfigService configService
    DepartmentService departmentService
    UserService userService

    @CompileDynamic
    boolean piBelongsToDepartment(List<String> piUsernameList, String department) {
        List<String> piUsernames = User.findAllByUsernameInList(piUsernameList)*.username
        return departmentService.getListOfPIForDepartment(department).any { it.key in piUsernames }
    }

    @CompileDynamic
    String getDepartmentIfExists(Map<String, String> additionalFieldValue) {
        if (processingOptionService.findOptionAsString(ProcessingOption.OptionName.ENABLE_PROJECT_REQUEST_PI).toBoolean()) {
            Map.Entry<String, String> organizationalUnit = additionalFieldValue.find {
                CollectionUtils.atMostOneElement(AbstractFieldDefinition.findAllById(it.key as Long)).name == 'Organizational Unit'
            }
            return organizationalUnit ? organizationalUnit.value : null
        }
        return null
    }

    List<User> getDepartmentHeads(Map<String, String> additionalFieldsValue) {
        String department = getDepartmentIfExists(additionalFieldsValue)
        if (!department) {
            return []
        }

        return departmentService.getListOfHeadsForDepartment(department)
    }

    @CompileDynamic
    ProjectRequest saveProjectRequestFromCommand(ProjectRequestCreationCommand cmd) throws OtpRuntimeException {
        securityService.ensureNotSwitchedUser()
        ProjectRequest projectRequest
        Set<ProjectRequestUser> users = []
        Set<ProjectRequestUser> piUsers = []
        if (cmd.users.findAll()*.username.findAll().size() > 0) {
            users = projectRequestUserService.saveProjectRequestUsersFromCommands(cmd.users)
        }
        if (cmd.piUsers.findAll()*.username.findAll().size() > 0) {
            // check if the processing Option is enabled then the PI/deputy should belong to the department
            String department = getDepartmentIfExists(cmd.additionalFieldValue)
            if (!department || (department && piBelongsToDepartment(cmd.piUsers*.username, department))) {
                piUsers = projectRequestUserService.saveProjectRequestUsersFromCommands(cmd.piUsers)
            }
        }

        ProjectRequestPersistentState state = projectRequestPersistentStateService
                .saveProjectRequestPersistentStateForProjectRequest(cmd.projectRequest, piUsers as List)
        Set<Keyword> newKeywords = cmd.keywords.collect { Keyword.findOrCreateWhere(name: it) } as Set

        Map<String, Object> projectRequestParameters = [
                name                    : cmd.name,
                state                   : state,
                description             : cmd.description,
                keywords                : newKeywords,
                endDate                 : cmd.endDate,
                storageUntil            : resolveStoragePeriodToLocalDate(cmd.storagePeriod, cmd.storageUntil),
                relatedProjects         : cmd.relatedProjects,
                speciesWithStrains      : cmd.speciesWithStrains,
                customSpeciesWithStrains: cmd.customSpeciesWithStrains as Set,
                projectType             : cmd.projectType,
                sequencingCenters       : cmd.sequencingCenters as Set,
                customSequencingCenters : cmd.customSequencingCenters as Set,
                approxNoOfSamples       : cmd.approxNoOfSamples,
                seqTypes                : cmd.seqTypes as Set,
                customSeqTypes          : cmd.customSeqTypes as Set,
                requesterComment        : cmd.requesterComment,
                comments                : cmd.projectRequest?.comments ?: [],
                requester               : cmd.projectRequest?.requester ?: securityService.currentUser,
                piUsers                 : piUsers,
                users                   : users,
        ]
        if (cmd.projectRequest) {
            Set<Keyword> keywordsToDelete = cmd.projectRequest.keywords.findAll { Keyword keyword ->
                !(keyword in newKeywords) &&
                        !Project.createCriteria().list {
                            keywords {
                                'in'('id', [keyword.id])
                            }
                        } && !ProjectRequest.createCriteria().list {
                    keywords {
                        'in'('id', [keyword.id])
                    }
                    ne('id', cmd.projectRequest.id)
                }
            }
            projectRequest = cmd.projectRequest
            InvokerHelper.setProperties(projectRequest, projectRequestParameters)
            keywordsToDelete*.delete(flush: true)
            logAction(projectRequest, "request edited")
        } else {
            projectRequest = new ProjectRequest(projectRequestParameters)
            logAction(projectRequest, "request created")
        }
        projectRequest.save(flush: true)
        if (cmd.additionalFieldValue) {
            cmd.additionalFieldValue.each { entry ->
                saveAdditionalFieldValuesForProjectRequest(entry.value, entry.key, projectRequest)
            }
        }
        return projectRequest
    }

    @CompileDynamic
    void saveAdditionalFieldValuesForProjectRequest(String fieldValue, String fieldId, ProjectRequest projectRequest) {
        AbstractFieldDefinition afd = AbstractFieldDefinition.get(fieldId as Long)
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

    @CompileDynamic
    List<AbstractFieldDefinition> listAndFetchAbstractFields(Project.ProjectType projectType, ProjectPageType page) {
        boolean isOperator = securityService.ifAllGranted(Role.ROLE_OPERATOR)
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
        if (projectRequest) {
            return projectRequest.projectFields.collectEntries { AbstractFieldValue afv ->
                [(afv.definition.id.toString()): afv.displayValue]
            }
        }
        return [:]
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    @CompileDynamic
    ProjectRequest findProjectRequestByProject(Project project) {
        return CollectionUtils.atMostOneElement(ProjectRequest.findAllByProject(project))
    }

    @CompileDynamic
    void deleteProjectRequest(ProjectRequest projectRequest) {
        Set<ProjectRequestUser> projectRequestUsers = projectRequest.users + projectRequest.piUsers
        ProjectRequestPersistentState projectRequestPersistentState = projectRequest.state
        projectRequest.delete(flush: true)
        projectRequestPersistentStateService.deleteProjectRequestState(projectRequestPersistentState)
        projectRequestUsers.each {
            projectRequestUserService.deleteProjectRequestUser(it)
        }
    }

    void approveProjectRequest(ProjectRequest projectRequest, User user) throws SwitchedUserDeniedException {
        securityService.ensureNotSwitchedUser()
        projectRequestPersistentStateService.approveUser(projectRequest.state, user)
    }

    void sendSubmitEmail(ProjectRequest projectRequest) {
        User requester = projectRequest.requester
        List<String> recipients = [requester.email]
        List<String> ccs = projectRequest.state.usersThatNeedToApprove*.email
        String subject = messageSourceService.createMessage("notification.projectRequest.submit.subject", [
                projectRequestName: projectRequest.name,
                projectRequestId  : projectRequest.id,
        ])
        String body = messageSourceService.createMessage("notification.projectRequest.submit.body", [
                link         : getProjectRequestLinkWithoutParams(projectRequest),
                requester    : "${requester.realName} (${requester.username})",
                teamSignature: processingOptionService.findOptionAsString(ProcessingOption.OptionName.HELP_DESK_TEAM_NAME),
        ])
        mailHelperService.saveMail(subject, body, recipients, ccs)
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    void addProjectRequestUsersToProject(ProjectRequest projectRequest) {
        List<ProjectRequestUser> allUsers = (projectRequest.users + projectRequest.piUsers).toList()
        allUsers.each { ProjectRequestUser projectRequestUser ->
            userProjectRoleService.createUserProjectRole(
                    projectRequestUser.user,
                    projectRequest.project,
                    projectRequestUser.projectRoles,
                    [
                            accessToOtp           : projectRequestUser.accessToOtp,
                            accessToFiles         : projectRequestUser.accessToFiles,
                            manageUsers           : projectRequestUser.manageUsers,
                            manageUsersAndDelegate: projectRequestUser.manageUsersAndDelegate,
                            receivesNotifications : true,
                    ]
            )
        }
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    @CompileDynamic
    void addDepartmentHeadsToProject(Map<String, String> additionalFieldsValue, ProjectRequest projectRequest) {
        List<User> departmentHeads = getDepartmentHeads(additionalFieldsValue)
        if (!departmentHeads) {
            return
        }

        departmentHeads.each { User departmentHead ->
            // Check if departmentHead is already added
            List<UserProjectRole> existingUserProjectRole = UserProjectRole.findAllByProjectAndUser(projectRequest.project, departmentHead)
            if (!existingUserProjectRole) {
                userProjectRoleService.createUserProjectRole(
                        departmentHead,
                        projectRequest.project,
                        ProjectRole.findAllByName(ProjectRole.Basic.PI.name()) as Set<ProjectRole>,
                        [
                                accessToOtp           : true,
                                accessToFiles         : false,
                                manageUsers           : true,
                                manageUsersAndDelegate: true,
                                receivesNotifications : true,
                        ]
                )
            }
        }
    }

    void sendOperatorRejectEmail(ProjectRequest projectRequest, String rejectComment) {
        User requester = projectRequest.requester
        List<String> recipients = [requester.email]
        List<String> ccs = projectRequest.state.usersThatNeedToApprove*.email
        String subject = messageSourceService.createMessage("notification.projectRequest.operatorReject.subject", [
                projectRequestName: projectRequest.name,
                projectRequestId  : projectRequest.id,
        ])
        String body = messageSourceService.createMessage("notification.projectRequest.operatorReject.body", [
                requester    : "${requester.realName} (${requester.username})",
                comment      : rejectComment,
                link         : getProjectRequestLinkWithoutParams(projectRequest),
                teamSignature: processingOptionService.findOptionAsString(ProcessingOption.OptionName.HELP_DESK_TEAM_NAME),
        ])
        mailHelperService.saveMail(subject, body, recipients, ccs)
    }

    void sendPassOnEmail(ProjectRequest projectRequest) {
        User requester = projectRequest.requester
        List<User> projectAuthorities = (projectRequest.state.usersThatNeedToApprove as List).unique()
        List<String> recipients = projectAuthorities*.email
        List<String> ccs = [requester.email]
        String projectAuthoritiesUsernames = projectAuthorities.collect { "${it.realName} (${it.username})" }.join(", ")
        String subject = messageSourceService.createMessage("notification.projectRequest.passOn.subject", [
                projectRequestName: projectRequest.name,
                projectRequestId  : projectRequest.id,
        ])
        String body = messageSourceService.createMessage("notification.projectRequest.passOn.body", [
                recipients        : projectAuthoritiesUsernames,
                projectAuthorities: projectAuthoritiesUsernames,
                projectRequestName: projectRequest.name,
                link              : getProjectRequestLinkWithoutParams(projectRequest),
                teamSignature     : processingOptionService.findOptionAsString(ProcessingOption.OptionName.HELP_DESK_TEAM_NAME),
        ])
        mailHelperService.saveMail(subject, body, recipients, ccs)
    }

    void sendPiRejectEmail(ProjectRequest projectRequest, String rejectComment) {
        User requester = projectRequest.requester
        List<String> recipients = [requester.email]
        List<String> ccs = (ProjectRequestPersistentStateService.getAllProjectRequestAuthorities(projectRequest.state)*.email as List).unique()
        String subject = messageSourceService.createMessage("notification.projectRequest.piReject.subject", [
                projectRequestName: projectRequest.name,
                projectRequestId  : projectRequest.id,
        ])
        String body = messageSourceService.createMessage("notification.projectRequest.piReject.body", [
                requester         : "${requester.username} (${requester.realName})",
                projectRequestName: projectRequest.name,
                projectAuthority  : securityService.currentUser.username,
                comment           : rejectComment,
                link              : getProjectRequestLinkWithoutParams(projectRequest),
                teamSignature     : processingOptionService.findOptionAsString(ProcessingOption.OptionName.HELP_DESK_TEAM_NAME),
        ])
        mailHelperService.saveMail(subject, body, recipients, ccs)
    }

    void sendApprovedEmail(ProjectRequest projectRequest) {
        User requester = projectRequest.requester
        List<String> recipients = [requester.email]
        List<String> ccs = (ProjectRequestPersistentStateService.getAllProjectRequestAuthorities(projectRequest.state)*.email as List).unique()
        String subject = messageSourceService.createMessage("notification.projectRequest.approved.subject", [
                projectRequestName: projectRequest.name,
                projectRequestId  : projectRequest.id,
        ])
        String body = messageSourceService.createMessage("notification.projectRequest.approved.body", [
                requester         : "${requester.username} (${requester.realName})",
                projectRequestName: projectRequest.name,
                teamSignature     : processingOptionService.findOptionAsString(ProcessingOption.OptionName.HELP_DESK_TEAM_NAME),
        ])
        mailHelperService.saveMail(subject, body, recipients, ccs)
    }

    void sendPartiallyApprovedEmail(ProjectRequest projectRequest) {
        List<User> allProjectAuthorities = ProjectRequestPersistentStateService.getAllProjectRequestAuthorities(projectRequest.state)
        User requester = projectRequest.requester
        List<String> recipients = [requester.email]
        List<String> ccs = allProjectAuthorities*.email
        String subject = messageSourceService.createMessage("notification.projectRequest.partiallyApproved.subject", [
                projectRequestName: projectRequest.name,
                projectRequestId  : projectRequest.id,
        ])
        String body = messageSourceService.createMessage("notification.projectRequest.partiallyApproved.body", [
                requester         : "${requester.username} (${requester.realName})",
                projectRequestName: projectRequest.name,
                projectAuthority  : securityService.currentUser.username,
                teamSignature     : processingOptionService.findOptionAsString(ProcessingOption.OptionName.HELP_DESK_TEAM_NAME),
        ])
        mailHelperService.saveMail(subject, body, recipients, ccs)
    }

    @CompileDynamic
    Set<ProjectRequest> getAllApproved() {
        return ProjectRequest.createCriteria().listDistinct {
            state {
                eq("beanName", ProjectRequestStateProvider.getStateBeanName(Approved))
            }
        } as Set<ProjectRequest>
    }

    void sendDeleteEmail(ProjectRequest projectRequest) {
        List<User> allProjectAuthorities = ProjectRequestPersistentStateService.getAllProjectRequestAuthorities(projectRequest.state)
        User requester = projectRequest.requester
        List<String> recipients = allProjectAuthorities*.email
        recipients?.add(requester.email)
        recipients.unique()
        String projectAuthoritiesUsernames = allProjectAuthorities.collect { "${it.realName} (${it.username})" }.join(", ")
        String subject = messageSourceService.createMessage("notification.projectRequest.deleted.subject", [
                projectRequestName: projectRequest.name,
                projectRequestId  : projectRequest.id,
        ])
        String body = messageSourceService.createMessage("notification.projectRequest.deleted.body", [
                recipients        : "$projectAuthoritiesUsernames, ${requester.realName} (${requester.username})",
                projectRequestName: projectRequest.name,
                deletingUser      : securityService.currentUser,
                teamSignature     : processingOptionService.findOptionAsString(ProcessingOption.OptionName.HELP_DESK_TEAM_NAME),
        ])
        mailHelperService.saveMail(subject, body, recipients)
    }

    void sendDraftCreateEmail(ProjectRequest projectRequest) {
        User requester = projectRequest.requester
        List<String> recipients = [requester.email]
        String subject = messageSourceService.createMessage("notification.projectRequest.draft.subject", [
                projectRequestName: projectRequest.name,
                projectRequestId  : projectRequest.id,
        ])
        String body = messageSourceService.createMessage("notification.projectRequest.draft.body", [
                requester    : "${requester.realName} (${requester.username})",
                link         : getProjectRequestLinkWithoutParams(projectRequest),
                teamSignature: processingOptionService.findOptionAsString(ProcessingOption.OptionName.HELP_DESK_TEAM_NAME),
        ])
        mailHelperService.saveMail(subject, body, recipients)
    }

    void sendDraftDeleteEmail(ProjectRequest projectRequest) {
        User requester = projectRequest.requester
        List<String> recipients = [requester.email]
        String subject = messageSourceService.createMessage("notification.projectRequest.draftDelete.subject", [
                projectRequestName: projectRequest.name,
                projectRequestId  : projectRequest.id,
        ])
        String body = messageSourceService.createMessage("notification.projectRequest.draftDelete.body", [
                requester         : "${requester.username} (${requester.realName})",
                projectRequestName: projectRequest.name,
                teamSignature     : processingOptionService.findOptionAsString(ProcessingOption.OptionName.HELP_DESK_TEAM_NAME),
        ])
        mailHelperService.saveMail(subject, body, recipients)
    }

    void sendPiEditedEmail(ProjectRequest projectRequest) {
        List<User> allProjectAuthorities = ProjectRequestPersistentStateService.getAllProjectRequestAuthorities(projectRequest.state) as List
        List<String> recipients = allProjectAuthorities*.email
        List<String> ccs = [projectRequest.requester.email]
        String subject = messageSourceService.createMessage("notification.projectRequest.piEdit.subject", [
                projectRequestName: projectRequest.name,
                projectRequestId  : projectRequest.id,
        ])
        String body = messageSourceService.createMessage("notification.projectRequest.piEdit.body", [
                projectAuthorities: allProjectAuthorities*.username.join(", "),
                projectAuthority  : securityService.currentUser.username,
                projectRequestName: projectRequest.name,
                link              : getProjectRequestLinkWithoutParams(projectRequest),
                teamSignature     : processingOptionService.findOptionAsString(ProcessingOption.OptionName.HELP_DESK_TEAM_NAME),
        ])
        mailHelperService.saveMail(subject, body, recipients, ccs)
    }

    void sendCreatedEmail(ProjectRequest projectRequest, Project project, List<String> recipients, List<String> ccs) {
        String sampleOverviewLink = linkGenerator.link(
                controller: "sampleOverview",
                action: "index",
                absolute: true,
                id: project.id,
                params: [(ProjectSelectionService.PROJECT_SELECTION_PARAMETER): project.name]
        )
        String userManagementLink = linkGenerator.link(
                controller: "projectUser",
                action: "index",
                absolute: true,
                params: [(ProjectSelectionService.PROJECT_SELECTION_PARAMETER): project.name]
        )
        String projectConfigLink = linkGenerator.link(
                controller: "projectConfig",
                action: "index",
                absolute: true,
                params: [(ProjectSelectionService.PROJECT_SELECTION_PARAMETER): project.name]
        )
        String subject = messageSourceService.createMessage("notification.projectRequest.created.subject", [
                projectName       : project.name,
                projectRequestId  : projectRequest?.id ?: '-',
                projectRequestName: projectRequest?.name ?: '-',
        ])
        String body = messageSourceService.createMessage("notification.projectRequest.created.body", [
                projectName               : project.displayName,
                projectLink               : sampleOverviewLink,
                projectConfigLink         : projectConfigLink,
                projectDirectoryPath      : configService.rootPath.toPath().resolve(project.dirName),
                analysisDirectoryPath     : project.dirAnalysis,
                additionalText            : processingOptionService.findOptionAsString(ProcessingOption.OptionName.EMAIL_PROJECT_CREATION_FREETEXT),
                userManagementLink        : userManagementLink,
                clusterName               : processingOptionService.findOptionAsString(ProcessingOption.OptionName.CLUSTER_NAME),
                clusterTicketingSystemMail: processingOptionService.findOptionAsString(ProcessingOption.OptionName.EMAIL_CLUSTER_ADMINISTRATION),
                ticketingSystemMail       : processingOptionService.findOptionAsString(ProcessingOption.OptionName.EMAIL_TICKET_SYSTEM),
                teamSignature             : processingOptionService.findOptionAsString(ProcessingOption.OptionName.HELP_DESK_TEAM_NAME),
        ])
        String emailStorageAdministration = processingOptionService.findOptionAsString(ProcessingOption.OptionName.EMAIL_STORAGE_ADMINISTRATION)
        if (emailStorageAdministration) {
            ccs.add(emailStorageAdministration)
        }
        mailHelperService.saveMail(subject, body, recipients, ccs)
    }

    private String getProjectRequestLinkWithoutParams(ProjectRequest projectRequest) {
        return linkGenerator.link(
                controller: "projectRequest",
                action: "view",
                absolute: true,
                id: projectRequest.id,
                params: [:]
        )
    }

    String getCurrentOwnerDisplayName(ProjectRequest projectRequest) {
        if (projectRequest?.state?.currentOwner) {
            User currentOwner = projectRequest.state.currentOwner
            if (securityService.hasCurrentUserAdministrativeRoles()) {
                return currentOwner.username
            }
            List<String> currentOwnerAuthorities = userService.getAuthorities(currentOwner)*.authority
            List<String> containedAdministrativeRoles = Role.ADMINISTRATIVE_ROLES.findAll { currentOwnerAuthorities.contains(it) }
            return containedAdministrativeRoles ?
                    processingOptionService.findOptionAsString(ProcessingOption.OptionName.HELP_DESK_TEAM_NAME) :
                    currentOwner.username
        }
        return "-"
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

    AuditLog logAction(ProjectRequest request, String description) {
        String staticLogPrefix = "[Request ${request.id}: ${request.name}]"
        return auditLogService.logAction(AuditLog.Action.PROJECT_REQUEST, "${staticLogPrefix} ${description}")
    }

    @CompileDynamic
    List<ProjectRequest> getRequestsUserIsInvolved(boolean resolved) {
        String equalOrNotEqual = resolved ? "eq" : "ne"
        boolean currentUserIsOperator = securityService.ifAllGranted(Role.ROLE_OPERATOR)
        User currentUser = securityService.currentUser
        if (currentUserIsOperator) {
            return (ProjectRequest.withCriteria {
                state {
                    "${equalOrNotEqual}"("beanName", ProjectRequestStateProvider.getStateBeanName(Created))
                }
            } as List<ProjectRequest>).unique()
        }
        return (ProjectRequest.withCriteria {
            and {
                or {
                    and {
                        piUsers {
                            'eq'("user", currentUser)
                            projectRoles {
                                'in'("name", ProjectRole.Basic.PI.toString())
                            }
                        }
                    }
                    eq("requester", currentUser)
                }
                state {
                    "${equalOrNotEqual}"("beanName", ProjectRequestStateProvider.getStateBeanName(Created))
                }
            }
        } as List<ProjectRequest>).unique().findAll {
            return ProjectRequestPersistentStateService.getAllProjectRequestAuthorities(it.state).contains(currentUser) || it.requester == currentUser
        }
    }

    List<ProjectRequest> sortRequestToBeHandledByUser(List<ProjectRequest> projectRequests) {
        return projectRequests.findAll { projectRequest ->
            List<ProjectRequestAction> actionsUserShouldHandle = projectRequestStateProvider.getCurrentState(projectRequest).getViewActions(projectRequest)
            return [ProjectRequestAction.SUBMIT_INDEX,
                    ProjectRequestAction.SUBMIT_VIEW,
                    ProjectRequestAction.PASS_ON,
                    ProjectRequestAction.APPROVE,
                    ProjectRequestAction.CREATE,
                    ProjectRequestAction.SAVE_INDEX,
                    ProjectRequestAction.SAVE_VIEW,
                    ProjectRequestAction.REJECT].find { actionsUserShouldHandle.contains(it) }
        }
    }

    @CompileDynamic
    void saveProjectRequest(ProjectRequest projectRequest) {
        projectRequest.save(flush: true)
    }
}
