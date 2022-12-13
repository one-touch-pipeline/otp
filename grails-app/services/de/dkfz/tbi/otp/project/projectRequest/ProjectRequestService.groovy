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

import grails.gorm.transactions.Transactional
import grails.web.mapping.LinkGenerator
import org.codehaus.groovy.runtime.InvokerHelper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.access.prepost.PreAuthorize

import de.dkfz.tbi.otp.ProjectSelectionService
import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.ngsdata.ProjectRole
import de.dkfz.tbi.otp.ngsdata.UserProjectRoleService
import de.dkfz.tbi.otp.project.*
import de.dkfz.tbi.otp.project.additionalField.*
import de.dkfz.tbi.otp.security.*
import de.dkfz.tbi.otp.security.user.SwitchedUserDeniedException
import de.dkfz.tbi.otp.utils.*

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

    ProjectRequest saveProjectRequestFromCommand(ProjectRequestCreationCommand cmd) throws SwitchedUserDeniedException {
        securityService.ensureNotSwitchedUser()
        ProjectRequest projectRequest
        Set<ProjectRequestUser> users = projectRequestUserService.saveProjectRequestUsersFromCommands(cmd.users)
        ProjectRequestPersistentState state = projectRequestPersistentStateService
                .saveProjectRequestPersistentStateForProjectRequest(cmd.projectRequest, users as List)
        Map<String, Object> projectRequestParameters = [
                name                    : cmd.name,
                state                   : state,
                description             : cmd.description,
                keywords                : cmd.keywords,
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
                users                   : users,
        ]
        if (cmd.projectRequest) {
            projectRequest = cmd.projectRequest
            InvokerHelper.setProperties(projectRequest, projectRequestParameters)
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
    ProjectRequest findProjectRequestByProject(Project project) {
        return CollectionUtils.atMostOneElement(ProjectRequest.findAllByProject(project))
    }

    void deleteProjectRequest(ProjectRequest projectRequest) {
        Set<ProjectRequestUser> projectRequestUsers = projectRequest.users
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
        List<String> recipient = [requester.email]
        List<String> ccs = projectRequest.state.usersThatNeedToApprove*.email
        String subject = messageSourceService.createMessage("notification.projectRequest.submit.subject", [
                projectName: projectRequest.name,
                projectId: projectRequest.id,
        ])
        String body = messageSourceService.createMessage("notification.projectRequest.submit.body", [
                link         : getProjectRequestLinkWithoutParams(projectRequest),
                requester    : "${requester.username} (${requester.realName})",
                teamSignature: processingOptionService.findOptionAsString(ProcessingOption.OptionName.HELP_DESK_TEAM_NAME),
        ])
        mailHelperService.sendEmail(subject, body, recipient, ccs)
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    void addProjectRequestUsersToProject(ProjectRequest projectRequest) {
        projectRequest.users.each { ProjectRequestUser projectRequestUser ->
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

    void sendOperatorRejectEmail(ProjectRequest projectRequest, String rejectComment) {
        User requester = projectRequest.requester
        List<String> recipient = [requester.email]
        List<String> ccs = projectRequest.state.usersThatNeedToApprove*.email
        String subject = messageSourceService.createMessage("notification.projectRequest.operatorReject.subject", [
                projectName: projectRequest.name,
                projectId: projectRequest.id,
        ])
        String body = messageSourceService.createMessage("notification.projectRequest.operatorReject.body", [
                requester    : "${requester.username} (${requester.realName})",
                comment      : rejectComment,
                link         : getProjectRequestLinkWithoutParams(projectRequest),
                teamSignature: processingOptionService.findOptionAsString(ProcessingOption.OptionName.HELP_DESK_TEAM_NAME),
        ])
        mailHelperService.sendEmail(subject, body, recipient, ccs)
    }

    void sendPassOnEmail(ProjectRequest projectRequest) {
        User requester = projectRequest.requester
        List<User> projectAuthorities = ((projectRequest.state.usersThatNeedToApprove as List) + [requester]).unique()
        List<String> recipients = projectAuthorities*.email
        List<String> ccs = [requester.email]
        String projectAuthoritiesUsernames = projectAuthorities*.username.join(', ')
        String subject = messageSourceService.createMessage("notification.projectRequest.passOn.subject", [
                projectName: projectRequest.name,
                projectId: projectRequest.id,
        ])
        String body = messageSourceService.createMessage("notification.projectRequest.passOn.body", [
                recipients        : projectAuthoritiesUsernames,
                projectAuthorities: projectAuthoritiesUsernames,
                projectName       : projectRequest.name,
                link              : getProjectRequestLinkWithoutParams(projectRequest),
                teamSignature     : processingOptionService.findOptionAsString(ProcessingOption.OptionName.HELP_DESK_TEAM_NAME),
        ])
        mailHelperService.sendEmail(subject, body, recipients, ccs)
    }

    void sendPiRejectEmail(ProjectRequest projectRequest, String rejectComment) {
        User requester = projectRequest.requester
        List<String> recipient = [requester.email]
        List<String> ccs = projectRequest.state.usersThatNeedToApprove*.email
        String subject = messageSourceService.createMessage("notification.projectRequest.piReject.subject", [
                projectName: projectRequest.name,
                projectId: projectRequest.id,
        ])
        String body = messageSourceService.createMessage("notification.projectRequest.piReject.body", [
                requester       : "${requester.username} (${requester.realName})",
                projectName     : projectRequest.name,
                projectAuthority: securityService.currentUser.username,
                comment         : rejectComment,
                link            : getProjectRequestLinkWithoutParams(projectRequest),
                teamSignature   : processingOptionService.findOptionAsString(ProcessingOption.OptionName.HELP_DESK_TEAM_NAME),
        ])
        mailHelperService.sendEmail(subject, body, recipient, ccs)
    }

    void sendApprovedEmail(ProjectRequest projectRequest) {
        User requester = projectRequest.requester
        List<String> recipient = ((ProjectRequestPersistentStateService.getAllProjectRequestAuthorities(projectRequest.state)*.email) + [requester.email])
                .unique()
        String subject = messageSourceService.createMessage("notification.projectRequest.approved.subject", [
                projectName: projectRequest.name,
                projectId: projectRequest.id,
        ])
        String body = messageSourceService.createMessage("notification.projectRequest.approved.body", [
                requester    : "${requester.username} (${requester.realName})",
                projectName  : projectRequest.name,
                teamSignature: processingOptionService.findOptionAsString(ProcessingOption.OptionName.HELP_DESK_TEAM_NAME),
        ])
        mailHelperService.sendEmail(subject, body, recipient)
    }

    void sendPartiallyApprovedEmail(ProjectRequest projectRequest) {
        List<User> allProjectAuthorities = ProjectRequestPersistentStateService.getAllProjectRequestAuthorities(projectRequest.state)
        User requester = projectRequest.requester
        List<String> recipient = [requester.email]
        List<String> ccs = allProjectAuthorities*.email
        String subject = messageSourceService.createMessage("notification.projectRequest.partiallyApproved.subject", [
                projectName: projectRequest.name,
                projectId: projectRequest.id,
        ])
        String body = messageSourceService.createMessage("notification.projectRequest.partiallyApproved.body", [
                requester       : "${requester.username} (${requester.realName})",
                projectName     : projectRequest.name,
                projectAuthority: securityService.currentUser.username,
                teamSignature   : processingOptionService.findOptionAsString(ProcessingOption.OptionName.HELP_DESK_TEAM_NAME),
        ])
        mailHelperService.sendEmail(subject, body, recipient, ccs)
    }

    void sendDeleteEmail(ProjectRequest projectRequest) {
        List<User> allProjectAuthorities = ProjectRequestPersistentStateService.getAllProjectRequestAuthorities(projectRequest.state)
        User requester = projectRequest.requester
        List<User> recipients = allProjectAuthorities
        recipients?.add(requester)
        recipients.unique()
        String subject = messageSourceService.createMessage("notification.projectRequest.deleted.subject", [
                projectName: projectRequest.name,
                projectId: projectRequest.id,
        ])
        String body = messageSourceService.createMessage("notification.projectRequest.deleted.body", [
                recipients   : recipients*.username.join(", "),
                projectName  : projectRequest.name,
                deletingUser : securityService.currentUser,
                teamSignature: processingOptionService.findOptionAsString(ProcessingOption.OptionName.HELP_DESK_TEAM_NAME),
        ])
        mailHelperService.sendEmail(subject, body, recipients*.email)
    }

    void sendDraftCreateEmail(ProjectRequest projectRequest) {
        User requester = projectRequest.requester
        List<String> recipient = [requester.email]
        String subject = messageSourceService.createMessage("notification.projectRequest.draft.subject", [
                projectName: projectRequest.name,
                projectId: projectRequest.id,
        ])
        String body = messageSourceService.createMessage("notification.projectRequest.draft.body", [
                requester    : "${requester.username} (${requester.realName})",
                link         : getProjectRequestLinkWithoutParams(projectRequest),
                teamSignature: processingOptionService.findOptionAsString(ProcessingOption.OptionName.HELP_DESK_TEAM_NAME),
        ])
        mailHelperService.sendEmail(subject, body, recipient)
    }

    void sendDraftDeleteEmail(ProjectRequest projectRequest) {
        User requester = projectRequest.requester
        List<String> recipient = [requester.email]
        String subject = messageSourceService.createMessage("notification.projectRequest.draftDelete.subject", [
                projectName: projectRequest.name,
                projectId: projectRequest.id,
        ])
        String body = messageSourceService.createMessage("notification.projectRequest.draftDelete.body", [
                requester    : "${requester.username} (${requester.realName})",
                projectName  : projectRequest.name,
                teamSignature: processingOptionService.findOptionAsString(ProcessingOption.OptionName.HELP_DESK_TEAM_NAME),
        ])
        mailHelperService.sendEmail(subject, body, recipient)
    }

    void sendPiEditedEmail(ProjectRequest projectRequest) {
        List<User> allProjectAuthorities = ProjectRequestPersistentStateService.getAllProjectRequestAuthorities(projectRequest.state) as List
        List<String> recipient = allProjectAuthorities*.email
        List<String> ccs = [projectRequest.requester.email]
        String subject = messageSourceService.createMessage("notification.projectRequest.piEdit.subject", [
                projectName: projectRequest.name,
                projectId: projectRequest.id,
        ])
        String body = messageSourceService.createMessage("notification.projectRequest.piEdit.body", [
                projectAuthorities: allProjectAuthorities*.username.join(", "),
                projectAuthority  : securityService.currentUser.username,
                projectName       : projectRequest.name,
                link              : getProjectRequestLinkWithoutParams(projectRequest),
                teamSignature     : processingOptionService.findOptionAsString(ProcessingOption.OptionName.HELP_DESK_TEAM_NAME),
        ])
        mailHelperService.sendEmail(subject, body, recipient, ccs)
    }

    void sendCreatedEmail(Project project, List<String> recipient, List<String> ccs) {
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
                projectName: project.name,
                projectId: project.id,
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
        mailHelperService.sendEmail(subject, body, recipient, ccs)
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
            List<String> currentOwnerAuthorities = currentOwner.authorities*.authority
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
                        users {
                            'in'("user", currentUser)
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

    void saveProjectRequest(ProjectRequest projectRequest) {
        projectRequest.save(flush: true)
    }
}
