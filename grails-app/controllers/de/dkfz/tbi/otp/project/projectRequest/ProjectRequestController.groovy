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

import grails.converters.JSON
import grails.databinding.BindUsing
import grails.databinding.SimpleMapDataBindingSource
import grails.plugin.springsecurity.annotation.Secured
import grails.validation.Validateable
import grails.validation.ValidationException
import groovy.transform.TupleConstructor
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize

import de.dkfz.tbi.otp.CheckAndCall
import de.dkfz.tbi.otp.FlashMessage
import de.dkfz.tbi.otp.config.TypeValidators
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.taxonomy.SpeciesWithStrain
import de.dkfz.tbi.otp.project.*
import de.dkfz.tbi.otp.project.Project.ProjectType
import de.dkfz.tbi.otp.project.additionalField.*
import de.dkfz.tbi.otp.searchability.Keyword
import de.dkfz.tbi.otp.security.*
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.utils.StringUtils
import de.dkfz.tbi.otp.workflow.shared.WorkflowException
import de.dkfz.tbi.util.TimeFormats

import java.time.LocalDate

@Secured('isFullyAuthenticated()')
class ProjectRequestController implements CheckAndCall {

    ProjectRequestService projectRequestService
    ProcessingOptionService processingOptionService
    SecurityService securityService

    @Autowired
    ProjectRequestStateProvider projectRequestStateProvider

    private static final String ACTION_INDEX = "index"
    private static final String ACTION_UNRESOLVED = "unresolved"
    private static final String ACTION_VIEW = "view"

    static allowedMethods = [
            index              : "GET",
            unresolved         : "GET",
            view               : "GET",
            submitView         : "POST",
            saveView           : "POST",
            saveIndex          : "POST",
            all                : "GET",
            resolved           : "GET",
            getAdditionalFields: "POST",
            submitIndex        : "POST",
            saveEdit           : "POST",
            reject             : "POST",
            passOn             : "POST",
            edit               : "POST",
            approve            : "POST",
            delete             : "POST",
            create             : "POST",
    ]

    def index() {
        String projectNamePattern = processingOptionService.findOptionAsString(ProcessingOption.OptionName.REGEX_PROJECT_NAME_NEW_PROJECT_REQUEST)
        String projectNameDescription = processingOptionService.findOptionAsString(ProcessingOption.OptionName.DESCRIPTION_PROJECT_NAME_NEW_PROJECT_REQUEST)
        ProjectRequestCreationCommand cmd = flash.cmd as ProjectRequestCreationCommand
        // This is required cause sometimes the projectRequest state is not processed right by the command object from flash
        if (flash.cmd && flash.cmd.projectRequest) {
            cmd.projectRequest = ProjectRequest.get(flash.cmd.projectRequest.id)
        }
        List<String> sequencingCenters = SeqCenter.findAll()*.name
        if (cmd?.sequencingCenters) {
            sequencingCenters.addAll(cmd?.sequencingCenters)
        }
        return [
                buttonActions         : projectRequestStateProvider.getCurrentState(cmd?.projectRequest).getIndexActions(cmd?.projectRequest),
                keywords              : Keyword.listOrderByName(),
                projectNameDescription: projectNameDescription,
                projectNamePattern    : projectNamePattern,
                projectTypes          : Project.ProjectType.values(),
                seqTypes              : SeqType.all.sort { it.displayNameWithLibraryLayout },
                speciesWithStrains    : SpeciesWithStrain.all.sort { it.displayString },
                storagePeriod         : StoragePeriod.values(),
                availableRoles        : ProjectRole.findAll(),
                sequencingCenters     : sequencingCenters.unique(),
                cmd                   : cmd,
        ]
    }

    def unresolved() {
        List<ProjectRequest> requestsUserIsInvolved = projectRequestService.getRequestsUserIsInvolved(false)
        List<ProjectRequest> requestsToBeCheckedByUser = projectRequestService.sortRequestToBeHandledByUser(requestsUserIsInvolved)
        return [
                check: ProjectRequestTableCommand.fromProjectRequest(requestsToBeCheckedByUser),
                unresolved: ProjectRequestTableCommand.fromProjectRequest(requestsUserIsInvolved - requestsToBeCheckedByUser),
        ]
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    def all() {
        List<ProjectRequest> allProjectRequests = ProjectRequest.list().sort { it.dateCreated }.reverse()
        return [
                all: ProjectRequestTableCommand.fromProjectRequest(allProjectRequests)
        ]
    }

    def resolved() {
        List<ProjectRequest> resolvedProjectRequests = projectRequestService.getRequestsUserIsInvolved(true)
        return [
                resolved: ProjectRequestTableCommand.fromProjectRequest(resolvedProjectRequests),
        ]
    }

    def view(ProjectRequest projectRequest) {
        Map<String, String> abstractValues = projectRequestService.listAdditionalFieldValues(projectRequest)

        List<AbstractFieldDefinition> fieldDefinitions = projectRequestService.listAndFetchAbstractFields(projectRequest.projectType,
                ProjectPageType.PROJECT_REQUEST)

        boolean isProjectAuthority = ProjectRoleService.projectRolesContainAuthoritativeRole(projectRequest.users.find {
            it.user == securityService.currentUserAsUser
        }?.projectRoles)

        return [
                currentUserIsProjectAuthority: isProjectAuthority,
                buttonActions                : projectRequestStateProvider.getCurrentState(projectRequest).getViewActions(projectRequest),
                abstractFields               : fieldDefinitions,
                abstractValues               : abstractValues,
                projectRequest               : projectRequest,
        ]
    }

    def submitIndex(ProjectRequestCreationCommand cmd) {
        saveProjectRequest({ it.submit(cmd) }, cmd, false)
    }

    def submitView(ProjectRequestCreationCommand cmd) {
        ProjectRequestCreationCommand cmdFromProjectRequest = ProjectRequestCreationCommand.fromProjectRequest(cmd.projectRequest)
        saveProjectRequest({ it.submit(cmdFromProjectRequest) }, cmdFromProjectRequest, false)
    }

    def saveIndex(ProjectRequestCreationCommand cmd) {
        try {
            saveProjectRequest({ it.save(cmd) }, cmd, true)
        } catch (ProjectRequestBeingEditedException e) {
            flash.message =  new FlashMessage(g.message(code: "projectRequest.edit.already") as String, e.message)
        }
    }

    def saveView(ProjectRequestCreationCommand cmd) {
        try {
            ProjectRequestCreationCommand cmdFromProjectRequest = ProjectRequestCreationCommand.fromProjectRequest(cmd.projectRequest)
            saveProjectRequest({ it.save(cmdFromProjectRequest) }, cmdFromProjectRequest, true)
        } catch (ProjectRequestBeingEditedException e) {
            flash.message =  new FlashMessage(g.message(code: "projectRequest.edit.already") as String, e.message)
        }
    }

    def reject(ProjectRequestRejectCommand cmd) {
        if (!commandIsValid(cmd)) {
            redirect(action: ACTION_VIEW, id: cmd.projectRequest.id)
            return
        }
        projectRequestStateProvider.getCurrentState(cmd.projectRequest).reject(cmd.projectRequest, cmd.additionalComment)
        redirect(action: ACTION_UNRESOLVED)
    }

    def passOn(ProjectRequest projectRequest) {
        projectRequestStateProvider.getCurrentState(projectRequest).passOn(projectRequest)
        redirect(action: ACTION_VIEW, id: projectRequest.id)
    }

    def edit(ProjectRequest projectRequest) {
        try {
            flash.cmd = projectRequestStateProvider.getCurrentState(projectRequest).edit(projectRequest)
        } catch (ProjectRequestBeingEditedException e) {
            flash.message =  new FlashMessage(g.message(code: "projectRequest.edit.already") as String, e.message)
        }
        redirect(action: ACTION_INDEX)
    }

    def approve(ApprovalCommand cmd) {
        if (!commandIsValid(cmd)) {
            redirect(action: ACTION_VIEW, id: cmd.projectRequest.id)
            return
        }
        try {
            projectRequestStateProvider.getCurrentState(cmd.projectRequest).approve(cmd)
        } catch (SwitchedUserDeniedException e) {
            flash.message = new FlashMessage(g.message(code: "error.switchedUserDeniedException.header") as String, e.message)
        }
        redirect(action: ACTION_VIEW, id: cmd.projectRequest.id)
    }

    @SuppressWarnings('ExplicitFlushForDeleteRule')
    def delete(ProjectRequest projectRequest) {
        projectRequestStateProvider.getCurrentState(projectRequest).delete(projectRequest)
        redirect(action: ACTION_UNRESOLVED)
    }

    def create(ProjectRequest projectRequest) {
        projectRequestStateProvider.getCurrentState(projectRequest).create()
        redirect(controller: "projectCreation", action: "index", params: [
                "projectRequest.id": projectRequest.id
        ])
    }

    def getAdditionalFields(AbstractFieldCommand cmd) {
        checkDefaultErrorsAndCallMethod(cmd) {
            try {
                List<AbstractFieldDefinition> fieldDefinitions = projectRequestService
                        .listAndFetchAbstractFields(cmd.projectType, ProjectPageType.PROJECT_REQUEST)
                Map<String, String> abstractValues = projectRequestService.listAdditionalFieldValues(cmd.projectRequest)
                List<Map<String, String>> abstractFields = fieldDefinitions.collect {
                    return convertToMapForFrontend(it, cmd.projectType, abstractValues)
                }
                Map map = [
                        abstractFields: abstractFields,
                        abstractValues: abstractValues,
                ]
                render map as JSON
            } catch (WorkflowException workflowException) {
                log.error(workflowException.message)
                return response.sendError(HttpStatus.BAD_REQUEST.value(), workflowException.message)
            }
        }
    }

    private Map<String, String> convertToMapForFrontend(AbstractFieldDefinition abstractFieldDefinition,
                                                        ProjectType projectType, Map<String, String> abstractValues) {
        String fieldType = ''
        String inputType = ''
        if (abstractFieldDefinition.projectFieldType == ProjectFieldType.TEXT) {
            if ((abstractFieldDefinition as TextFieldDefinition).typeValidator == TypeValidators.MULTI_LINE_TEXT) {
                fieldType = 'textArea'
            } else {
                fieldType = 'input'
                inputType = 'text'
            }
        } else if (abstractFieldDefinition.projectFieldType == ProjectFieldType.INTEGER) {
            fieldType = 'input'
            inputType = 'number'
        }
        String required = 'false'
        if ((projectType == ProjectType.SEQUENCING &&
                abstractFieldDefinition.fieldUseForSequencingProjects == FieldExistenceType.REQUIRED) ||
                (projectType == ProjectType.USER_MANAGEMENT &&
                        abstractFieldDefinition.fieldUseForDataManagementProjects == FieldExistenceType.REQUIRED)) {
            required = 'true'
        }
        String value = ''
        if (abstractValues) {
            value = abstractValues[abstractFieldDefinition.id as String]
        } else if (abstractFieldDefinition.defaultValue) {
            value = abstractFieldDefinition.defaultValue
        }
        return [
                id                : abstractFieldDefinition.id as String,
                name              : abstractFieldDefinition.name,
                fieldType         : fieldType,
                inputType         : inputType,
                required          : required,
                value             : value,
                descriptionRequest: abstractFieldDefinition.descriptionRequest,
        ]
    }

    private void saveProjectRequest(Closure<Long> closure, ProjectRequestCreationCommand cmd, boolean redirectView) {
        if (!commandIsValid(cmd)) {
            flash.cmd = cmd
            redirect(action: ACTION_INDEX)
            return
        }
        try {
            closure(projectRequestStateProvider.getCurrentState(cmd.projectRequest))
            flash.message = new FlashMessage(g.message(code: "projectRequest.store.success") as String)
            redirect(redirectView ? [action: ACTION_VIEW, id: cmd.projectRequest.id] : [action: ACTION_UNRESOLVED])
            return
        } catch (ValidationException e) {
            flash.message = new FlashMessage(g.message(code: "projectRequest.store.failure") as String, e.errors)
            flash.cmd = cmd
        } catch (LdapUserCreationException e) {
            flash.message = new FlashMessage(g.message(code: "projectRequest.store.failure") as String, [e.message])
            flash.cmd = cmd
        } catch (SwitchedUserDeniedException e) {
            flash.message = new FlashMessage(g.message(code: "error.switchedUserDeniedException.header") as String, e.message)
        }
        redirect(action: ACTION_INDEX)
    }

    private boolean commandIsValid(Validateable cmd) {
        if (!cmd.validate()) {
            flash.message = new FlashMessage(g.message(code: "projectRequest.store.failure") as String, cmd.errors)
            flash.cmd = cmd
            return false
        }
        return true
    }
}

class AbstractFieldCommand implements Validateable {
    Project.ProjectType projectType
    ProjectRequest projectRequest

    static constraints = {
        projectRequest nullable: true, blank: true
    }
}

@TupleConstructor
enum StoragePeriod {
    INFINITELY("Store data until PI requests deletion"),
    TEN_YEARS("Store data for ten years"),
    USER_DEFINED("Store data until given deletion date:"),

    final String description

    String getName() {
        return name()
    }
}

class ProjectRequestCreationCommand implements Validateable {
    ProjectRequest projectRequest
    String name
    Project.ProjectType projectType
    String description

    @BindUsing({ ProjectRequestCreationCommand obj, SimpleMapDataBindingSource source ->
        Object input = source['keywords']
        List<String> keywordList = (input instanceof String ? [input] : input) as List<String>
        return keywordList.collect { keyword ->
            return keyword ? Keyword.findOrSaveByName(StringUtils.trimAndShortenWhitespace(keyword)) : null
        }.findAll()
    })
    List<Keyword> keywords
    LocalDate endDate
    StoragePeriod storagePeriod
    LocalDate storageUntil
    String relatedProjects

    @BindUsing({  ProjectRequestCreationCommand obj, SimpleMapDataBindingSource source ->
        Object input = source['speciesWithStrainList']
        return (input instanceof String ? [input] : input) as List<String>
    })
    List<String> speciesWithStrainList
    List<SpeciesWithStrain> speciesWithStrains = []
    List<String> customSpeciesWithStrains = []
    List<String> sequencingCenters = []
    Integer approxNoOfSamples
    List<SeqType> seqTypes
    String comments
    List<ProjectRequestUserCommand> users

    List<String> additionalFieldName = []
    Map<String, String> additionalFieldValue = [:]

    static constraints = {
        projectRequest nullable: true
        name blank: false
        description blank: false
        keywords validator: { val, obj ->
            if (!val) {
                return "empty"
            }
        }
        endDate nullable: true
        storageUntil nullable: true, validator: { val, obj ->
            if (obj.storagePeriod == StoragePeriod.USER_DEFINED && !val) {
                return "empty"
            }
        }
        customSpeciesWithStrains nullable: true
        sequencingCenters nullable: true
        relatedProjects nullable: true, blank: false
        seqTypes nullable: true
        comments nullable: true, blank: false
        approxNoOfSamples nullable: true, validator: { val, obj ->
            if (obj.projectType == Project.ProjectType.SEQUENCING && !val) {
                return "projectRequest.approxNoOfSamples.null"
            }
        }
        users validator: { val, obj ->
            List<ProjectRequestUserCommand> value = val?.toList()?.findAll() ?: []
            if (!value.any { ProjectRequestUserCommand cmd ->
                ProjectRoleService.projectRolesContainAuthoritativeRole(cmd.projectRoles)
            }) {
                return "projectRequest.users.no.authority"
            }
            if (value*.username.size() != value*.username.unique().size()) {
                return "projectRequest.users.unique"
            }
        }
    }

    // assigns existing speciesWithStrains to the according list and the remaining strings are assigned to customSpeciesWithStrains
    void setSpeciesWithStrainList(List<String> stringList) {
        speciesWithStrainList = stringList
        stringList.each {
            if (it.isNumber()) {
                SpeciesWithStrain foundSpeciesWithStrain = SpeciesWithStrain.get(it as Long)
                if (foundSpeciesWithStrain) {
                    speciesWithStrains.add(foundSpeciesWithStrain)
                } else {
                    customSpeciesWithStrains.add(it)
                }
            } else {
                customSpeciesWithStrains.add(it)
            }
        }
    }

    void setRelatedProjects(String s) {
        relatedProjects = StringUtils.blankToNull(s)
    }

    void setComments(String s) {
        comments = StringUtils.blankToNull(s)
    }

    static ProjectRequestCreationCommand fromProjectRequest(ProjectRequest projectRequest) {
        LocalDate storageUntil = projectRequest.storageUntil
        StoragePeriod storagePeriod = storageUntil ? StoragePeriod.USER_DEFINED : StoragePeriod.INFINITELY

        List<Keyword> keywords = projectRequest.keywords.collect { CollectionUtils.atMostOneElement(Keyword.findAllByName(it)) }
        List<String> speciesWithStrainList = projectRequest.customSpeciesWithStrains as List ?: []
        projectRequest.speciesWithStrains.each {
            speciesWithStrainList.add(it.id as String)
        }

        return new ProjectRequestCreationCommand(
                projectRequest: projectRequest,
                name: projectRequest.name,
                projectType: projectRequest.projectType,
                description: projectRequest.description,
                keywords: keywords,
                endDate: projectRequest.endDate,
                storagePeriod: storagePeriod,
                storageUntil: storageUntil,
                relatedProjects: projectRequest.relatedProjects,
                speciesWithStrainList: speciesWithStrainList,
                speciesWithStrains: projectRequest.speciesWithStrains as List ?: [],
                customSpeciesWithStrains: projectRequest.customSpeciesWithStrains as List ?: [],
                sequencingCenters: projectRequest.sequencingCenters as List ?: [],
                approxNoOfSamples: projectRequest.approxNoOfSamples,
                seqTypes: projectRequest.seqTypes as List ?: [],
                comments: projectRequest.comments,
                users: ProjectRequestUserCommand.fromProjectRequestUsers(projectRequest.users),
        )
    }
}

class ProjectRequestRejectCommand implements Validateable {
    ProjectRequest projectRequest
    String additionalComment

    static constraints = {
        additionalComment validator: { val, obj ->
            return val ? true : "projectRequest.reject.comment"
        }
    }
}

class ProjectRequestTableCommand {
    User requester
    Set<ProjectRequestUser> users
    Project project
    ProjectRequestPersistentState state
    String dateCreated
    String lastUpdated
    String name
    Long id

    static List<ProjectRequestTableCommand> fromProjectRequest(List<ProjectRequest> projectRequests) {
        return projectRequests.collect { ProjectRequest projectRequest ->
            return new ProjectRequestTableCommand(
                    requester: projectRequest.requester,
                    users: projectRequest.users,
                    state: projectRequest.state,
                    dateCreated: TimeFormats.DATE.getFormattedDate(projectRequest.dateCreated as Date),
                    lastUpdated: TimeFormats.DATE.getFormattedDate(projectRequest.lastUpdated as Date),
                    name: projectRequest.name,
                    id: projectRequest.id,
            )
        }
    }
}

@TupleConstructor
class ProjectRequestUserCommand implements Validateable {
    ProjectRequestUser projectRequestUser
    String username
    Set<ProjectRole> projectRoles
    boolean accessToOtp
    boolean accessToFiles
    boolean manageUsers

    static constraints = {
        projectRequestUser nullable: true
        projectRoles validator: { val, obj ->
            return val ? true : "empty"
        }
    }

    static List<ProjectRequestUserCommand> fromProjectRequestUsers(Set<ProjectRequestUser> users) {
        return users.collect { ProjectRequestUser user ->
            return new ProjectRequestUserCommand(
                    projectRequestUser: user,
                    username: user.user.username,
                    projectRoles: user.projectRoles,
                    accessToOtp: user.accessToOtp,
                    accessToFiles: user.accessToFiles,
                    manageUsers: user.manageUsers
            )
        }
    }
}

class ApprovalCommand implements Validateable {
    ProjectRequest projectRequest
    String additionalComment
    boolean confirmConsent
    boolean confirmRecordOfProcessingActivities

    static constraints = {
        additionalComment nullable: true, blank: true
        confirmConsent validator: { val, obj ->
            return val ? true : "projectRequest.approve.confirmConsent"
        }
        confirmRecordOfProcessingActivities validator: { val, obj ->
            return val ? true : "projectRequest.approve.confirmRecordOfProcessingActivities"
        }
    }

    void setAdditionalComment(String s) {
        additionalComment = StringUtils.blankToNull(s)
    }
}

