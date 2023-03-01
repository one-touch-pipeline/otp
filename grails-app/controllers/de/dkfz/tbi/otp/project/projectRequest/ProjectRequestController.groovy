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
import de.dkfz.tbi.otp.security.SecurityService
import de.dkfz.tbi.otp.security.User
import de.dkfz.tbi.otp.security.user.SwitchedUserDeniedException
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.utils.StringUtils
import de.dkfz.tbi.otp.workflow.shared.WorkflowException
import de.dkfz.tbi.util.TimeFormats

import java.time.LocalDate

@PreAuthorize('isFullyAuthenticated()')
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
            cmd.projectRequest = CollectionUtils.exactlyOneElement(ProjectRequest.findAllById(flash.cmd.projectRequest.id))
        }
        return [
                buttonActions         : projectRequestStateProvider.getCurrentState(cmd?.projectRequest).getIndexActions(cmd?.projectRequest),
                keywords              : Keyword.listOrderByName(),
                projectNameDescription: projectNameDescription,
                projectNamePattern    : projectNamePattern,
                projectTypes          : ProjectType.values(),
                seqTypes              : SeqType.all.sort { it.displayNameWithLibraryLayout },
                speciesWithStrains    : SpeciesWithStrain.all.sort { it.displayString },
                storagePeriod         : StoragePeriod.values(),
                availableRoles        : ProjectRole.findAll(),
                sequencingCenters     : SeqCenter.all.unique().sort(),
                faqProjectTypeLink    : processingOptionService.findOptionAsString(ProcessingOption.OptionName.NOTIFICATION_TEMPLATE_FAQ_PROJECT_TYPE_LINK),
                cmd                   : cmd,
        ]
    }

    def unresolved() {
        List<ProjectRequest> requestsUserIsInvolved = projectRequestService.getRequestsUserIsInvolved(false)
        List<ProjectRequest> requestsToBeCheckedByUser = projectRequestService.sortRequestToBeHandledByUser(requestsUserIsInvolved)
        return [
                check     : ProjectRequestTableCommand.fromProjectRequest(requestsToBeCheckedByUser, projectRequestService),
                unresolved: ProjectRequestTableCommand.fromProjectRequest(requestsUserIsInvolved - requestsToBeCheckedByUser, projectRequestService),
        ]
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    def all() {
        List<ProjectRequest> allProjectRequests = ProjectRequest.list().sort { it.dateCreated }.reverse()
        return [
                all: ProjectRequestTableCommand.fromProjectRequest(allProjectRequests, projectRequestService)
        ]
    }

    def resolved() {
        List<ProjectRequest> resolvedProjectRequests = projectRequestService.getRequestsUserIsInvolved(true)
        return [
                resolved: ProjectRequestTableCommand.fromProjectRequest(resolvedProjectRequests, projectRequestService),
        ]
    }

    def view(ProjectRequest projectRequest) {
        Map<String, String> abstractValues = projectRequestService.listAdditionalFieldValues(projectRequest)

        List<AbstractFieldDefinition> fieldDefinitions = projectRequestService.listAndFetchAbstractFields(projectRequest.projectType,
                ProjectPageType.PROJECT_REQUEST)

        boolean isProjectAuthority = ProjectRoleService.projectRolesContainAuthoritativeRole(projectRequest.users.find {
            it.user == securityService.currentUser
        }?.projectRoles)

        return [
                currentUserIsProjectAuthority: isProjectAuthority,
                buttonActions                : projectRequestStateProvider.getCurrentState(projectRequest).getViewActions(projectRequest),
                abstractFields               : fieldDefinitions,
                abstractValues               : abstractValues,
                projectRequest               : projectRequest,
                stateDisplayName             : projectRequestStateProvider.getCurrentState(projectRequest).displayName,
                projectRequestId             : projectRequest.id,
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
            flash.message = new FlashMessage(g.message(code: "projectRequest.store.failure") as String, e.message)
        }
    }

    def saveView(ProjectRequestCreationCommand cmd) {
        try {
            ProjectRequestCreationCommand cmdFromProjectRequest = ProjectRequestCreationCommand.fromProjectRequest(cmd.projectRequest)
            saveProjectRequest({ it.save(cmdFromProjectRequest) }, cmdFromProjectRequest, true)
        } catch (ProjectRequestBeingEditedException e) {
            flash.message = new FlashMessage(g.message(code: "projectRequest.store.failure") as String, e.message)
        }
    }

    def reject(ProjectRequestRejectCommand cmd) {
        if (cmd.hasErrors()) {
            flash.cmd = cmd
            flash.message = new FlashMessage(g.message(code: "projectRequest.store.failure") as String, cmd.errors)
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
            redirect(action: ACTION_INDEX)
        } catch (ProjectRequestBeingEditedException e) {
            flash.message = new FlashMessage(g.message(code: "projectRequest.edit.failure") as String, e.message)
            redirect(action: ACTION_VIEW, id: projectRequest.id)
        }
    }

    def approve(ApprovalCommand cmd) {
        if (cmd.hasErrors()) {
            flash.cmd = cmd
            flash.message = new FlashMessage(g.message(code: "projectRequest.store.failure") as String, cmd.errors)
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
    JSON delete(ProjectRequest projectRequest) {
        projectRequestStateProvider.getCurrentState(projectRequest).delete(projectRequest)
        render([:] as JSON)
    }

    def create(ProjectRequest projectRequest) {
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
                render(map as JSON)
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
        if (cmd.hasErrors()) {
            flash.cmd = cmd
            flash.message = new FlashMessage(g.message(code: "projectRequest.store.failure") as String, cmd.errors)
            redirect(action: ACTION_INDEX)
            return
        }
        try {
            Long projectRequestId = closure(projectRequestStateProvider.getCurrentState(cmd.projectRequest))
            flash.message = new FlashMessage(g.message(code: "projectRequest.store.success") as String)
            redirect(redirectView ? [action: ACTION_VIEW, id: projectRequestId] : [action: ACTION_UNRESOLVED])
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
}

class AbstractFieldCommand implements Validateable {
    ProjectType projectType
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
    ProjectType projectType
    String description
    List<String> keywords
    LocalDate endDate
    StoragePeriod storagePeriod
    LocalDate storageUntil
    String relatedProjects

    @BindUsing({ ProjectRequestCreationCommand obj, SimpleMapDataBindingSource source ->
        Object input = source['speciesWithStrainList']
        return (input instanceof String ? [input] : input) as List<String>
    })
    List<String> speciesWithStrainList
    List<SpeciesWithStrain> speciesWithStrains = []
    List<String> customSpeciesWithStrains = []

    @BindUsing({ ProjectRequestCreationCommand obj, SimpleMapDataBindingSource source ->
        Object input = source['sequencingCenterList']
        return (input instanceof String ? [input] : input) as List<String>
    })
    List<String> sequencingCenterList
    List<SeqCenter> sequencingCenters = []
    List<String> customSequencingCenters = []
    Integer approxNoOfSamples

    @BindUsing({ ProjectRequestCreationCommand obj, SimpleMapDataBindingSource source ->
        Object input = source['seqTypesList']
        return (input instanceof String ? [input] : input) as List<String>
    })
    List<String> seqTypesList
    List<SeqType> seqTypes = []
    List<String> customSeqTypes = []
    String requesterComment
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
            if (obj.storagePeriod == StoragePeriod.USER_DEFINED) {
                if (!val) {
                    return "empty"
                } else if (val < LocalDate.now()) {
                    return "projectRequest.storageUntil.past"
                }
            }
        }
        speciesWithStrainList nullable: true, validator: { val, obj ->
            if (obj.projectType == ProjectType.SEQUENCING && !val) {
                return "empty"
            }
        }
        customSpeciesWithStrains nullable: true
        customSequencingCenters nullable: true
        sequencingCenters nullable: true
        relatedProjects nullable: true, blank: false
        seqTypes nullable: true
        customSeqTypes nullable: true
        requesterComment nullable: true, blank: false
        approxNoOfSamples nullable: true, validator: { val, obj ->
            if (obj.projectType == ProjectType.SEQUENCING && !val) {
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
        additionalFieldValue validator: { additionalFieldValueMap, obj, errors ->
            additionalFieldValueMap.each {
                AbstractFieldDefinition afd = AbstractFieldDefinition.get(it.key as Long)
                if (it.value && afd instanceof TextFieldDefinition) {
                    if (afd.allowedTextValues && !(it.value in afd.allowedTextValues)) {
                        String errorMessage = "Field ${afd.name} with value ${it.value} is not valid, it should be one of ${afd.allowedTextValues}"
                        errors.reject("${errorMessage}", null, "${errorMessage}")
                    }
                    if (afd.typeValidator && !afd.typeValidator.validate(it.value)) {
                        String errorMessage = "Field ${afd.name} with type ${it.value} is not valid, it should be of type ${afd.typeValidator}"
                        errors.reject("${errorMessage}", null, "${errorMessage}")
                    }
                    if (afd.regularExpression && !(it.value ==~ afd.regularExpression)) {
                        String errorMessage = "Field ${afd.name} : ${afd.regularExpressionError}"
                        errors.reject("${errorMessage}", null, "${errorMessage}")
                    }
                }
                else if (it.value && afd instanceof IntegerFieldDefinition) {
                    if (afd.allowedIntegerValues && !(afd.allowedIntegerValues.contains(it.value.toInteger()))) {
                        String errorMessage = "Field ${afd.name} with value ${it.value} is not valid, it should be one of ${afd.allowedIntegerValues}"
                        errors.reject("${errorMessage}", null, "${errorMessage}")
                    }
                }
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

    // assigns existing sequencingCenter to the according list and the remaining strings are assigned to customSequencingCenter
    void setSequencingCenterList(List<String> stringList) {
        sequencingCenterList = stringList
        stringList.each {
            if (it.isNumber()) {
                SeqCenter foundSeqCenter = SeqCenter.get(it as Long)
                if (foundSeqCenter) {
                    sequencingCenters.add(foundSeqCenter)
                } else {
                    customSequencingCenters.add(it)
                }
            } else {
                customSequencingCenters.add(it)
            }
        }
    }

    // assigns existing seqTypes to the list and the remaining strings are assigned to customSeqTypes
    void setSeqTypesList(List<String> stringList) {
        seqTypesList = stringList
        stringList.each {
            if (it.isNumber()) {
                SeqType foundSeqType = SeqType.get(it as Long)
                if (foundSeqType) {
                    seqTypes.add(foundSeqType)
                } else {
                    customSeqTypes.add(it)
                }
            } else {
                customSeqTypes.add(it)
            }
        }
    }

    void setRelatedProjects(String s) {
        relatedProjects = StringUtils.blankToNull(s)
    }

    void setRequesterComment(String s) {
        requesterComment = StringUtils.blankToNull(s)
    }

    static ProjectRequestCreationCommand fromProjectRequest(ProjectRequest projectRequest) {
        LocalDate storageUntil = projectRequest.storageUntil
        StoragePeriod storagePeriod = storageUntil ? StoragePeriod.USER_DEFINED : StoragePeriod.INFINITELY

        List<String> keywords = projectRequest.keywords as List ?: [null]
        List<String> speciesWithStrainList = projectRequest.customSpeciesWithStrains as List ?: []
        List<String> seqTypesList = projectRequest.customSeqTypes as List ?: []
        List<String> sequencingCenterList = projectRequest.customSequencingCenters as List ?: []
        projectRequest.speciesWithStrains.each {
            speciesWithStrainList.add(it.id as String)
        }
        projectRequest.seqTypes.each {
            seqTypesList.add(it.id as String)
        }
        projectRequest.sequencingCenters.each {
            sequencingCenterList.add(it.id as String)
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
                sequencingCenterList: sequencingCenterList,
                sequencingCenters: projectRequest.sequencingCenters as List ?: [],
                customSequencingCenters: projectRequest.customSequencingCenters as List ?: [],
                approxNoOfSamples: projectRequest.approxNoOfSamples,
                seqTypesList: seqTypesList,
                seqTypes: projectRequest.seqTypes as List ?: [],
                customSeqTypes: projectRequest.customSeqTypes as List ?: [],
                requesterComment: projectRequest.requesterComment,
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
    String currentOwner
    String stateDisplayName
    String dateCreated
    String lastUpdated
    String name
    Long id

    static List<ProjectRequestTableCommand> fromProjectRequest(List<ProjectRequest> projectRequests, ProjectRequestService service) {
        return projectRequests.collect { ProjectRequest projectRequest ->
            return new ProjectRequestTableCommand(
                    requester: projectRequest.requester,
                    users: projectRequest.users,
                    stateDisplayName: service.projectRequestStateProvider.getCurrentState(projectRequest).displayName,
                    currentOwner: service.getCurrentOwnerDisplayName(projectRequest),
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
