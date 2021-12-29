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

import grails.databinding.BindUsing
import grails.databinding.SimpleMapDataBindingSource
import grails.plugin.springsecurity.annotation.Secured
import grails.validation.ValidationException
import groovy.transform.TupleConstructor
import org.springframework.validation.Errors

import de.dkfz.tbi.otp.FlashMessage
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.taxonomy.SpeciesWithStrain
import de.dkfz.tbi.otp.project.additionalField.AbstractFieldDefinition
import de.dkfz.tbi.otp.project.additionalField.ProjectPageType
import de.dkfz.tbi.otp.searchability.Keyword
import de.dkfz.tbi.otp.utils.StringUtils
import de.dkfz.tbi.util.MultiObjectValueSource
import de.dkfz.tbi.otp.security.*

import java.time.LocalDate

@Secured('isFullyAuthenticated()')
class ProjectRequestController {

    static allowedMethods = [
            index   : "GET",
            check   : "GET",
            open    : "GET",
            resolved: "GET",
            save    : "POST",
            edit    : "POST",
            saveEdit: "POST",
            view    : "GET",
            all     : "GET",
            approve : "POST",
            deny    : "POST",
            close   : "POST",
    ]

    private static final String ACTION_INDEX = "index"
    private static final String ACTION_CHECK = "check"
    private static final String ACTION_VIEW = "view"

    ProcessingOptionService processingOptionService
    ProjectRequestService projectRequestService
    SecurityService securityService

    private Map getSharedModel() {
        List<ProjectRequest> submitted = projectRequestService.submittedRequests
        List<ProjectRequest> unresolved = projectRequestService.unresolvedRequestsOfUser
        List<ProjectRequest> resolved = projectRequestService.resolvedOfCurrentUser

        return [
                actionHighlight: unresolved ? "work-but-nothing-todo" : "no-work-and-nothing-todo",
                submitted      : submitted,
                unresolved     : unresolved,
                resolved       : resolved,
        ]
    }

    def index(Long id) {
        ProjectRequest projectRequest = projectRequestService.get(id)
        String projectNamePattern = processingOptionService.findOptionAsString(ProcessingOption.OptionName.REGEX_PROJECT_NAME_NEW_PROJECT_REQUEST)
        String projectNameDescription = processingOptionService.findOptionAsString(ProcessingOption.OptionName.DESCRIPTION_PROJECT_NAME_NEW_PROJECT_REQUEST)
        ProjectRequestUser requester = new ProjectRequestUser()
        requester.user = securityService.currentUserAsUser
        Map<String, ?> defaults = [
                keywords          : [""],
                seqTypes          : [null],
                users             : [requester] as Set,
                speciesWithStrains: [],
                projectType       : Project.ProjectType.SEQUENCING,
        ]

        Map<String, ?> projectRequestHelper = [:]
        Map<String, String> abstractValues = [:]
        if (projectRequest) {
            projectRequestHelper << [
                    users        : projectRequest.users ?: [],
                    storagePeriod: projectRequest.storageUntil ? StoragePeriod.USER_DEFINED : StoragePeriod.INFINITELY,
            ]
            abstractValues = projectRequestService.listAdditionalFieldValues(projectRequest)
        }

        MultiObjectValueSource multiObjectValueSource = new MultiObjectValueSource(flash.cmd, projectRequestHelper, projectRequest, defaults)

        List<AbstractFieldDefinition> fieldDefinitions = projectRequestService.listAndFetchAbstractFields(multiObjectValueSource
                .getByFieldName("projectType") as Project.ProjectType, ProjectPageType.PROJECT_REQUEST)

        return sharedModel + [
                projectNamePattern    : projectNamePattern,
                projectNameDescription: projectNameDescription,
                cmd                   : flash.cmd as ProjectRequestCreationCommand,
                projectRequestToEdit  : projectRequest,
                projectTypes          : Project.ProjectType.values(),
                storagePeriods        : StoragePeriod.values(),
                tumorEntities         : TumorEntity.listOrderByName(),
                species               : SpeciesWithStrain.all.sort { it.displayString } + [id: "other", displayString: "Other(s)"],
                keywords              : Keyword.listOrderByName(),
                seqTypes              : SeqType.all.sort { it.displayNameWithLibraryLayout },
                availableRoles        : ProjectRole.findAll(),
                source                : multiObjectValueSource,
                abstractFields        : fieldDefinitions,
                abstractValues        : abstractValues,
        ]
    }

    def check() {
        return sharedModel
    }

    def open() {
        return sharedModel
    }

    def resolved() {
        return sharedModel
    }

    def all() {
        return sharedModel + [
                all: projectRequestService.allProjectRequests.sort { it.dateCreated }.reverse(),
        ]
    }

    def save(ProjectRequestCreationCommand cmd) {
        if (cmd.save) {
            if (!cmd.validate()) {
                flash.message = new FlashMessage(g.message(code: "projectRequest.store.failure") as String, cmd.errors)
                flash.cmd = cmd
                redirect(action: ACTION_INDEX)
                return
            }
            try {
                ProjectRequest createdRequest = projectRequestService.create(cmd)
                String baseMessage = "${g.message(code: "projectRequest.store.success")}. "
                if (projectRequestService.requesterIsEligibleToAccept(createdRequest)) {
                    flash.message = new FlashMessage("${baseMessage}${g.message(code: "projectRequest.store.success.work")}")
                    redirect(action: ACTION_VIEW, id: createdRequest.id)
                } else {
                    flash.message = new FlashMessage("${baseMessage}${g.message(code: "projectRequest.store.success.waiting")}")
                    redirect(action: ACTION_CHECK)
                }
                return
            } catch (ValidationException e) {
                flash.message = new FlashMessage(g.message(code: "projectRequest.store.failure") as String, e.errors)
                flash.cmd = cmd
            } catch (LdapUserCreationException e) {
                flash.message = new FlashMessage(g.message(code: "projectRequest.store.failure") as String, [e.message])
                flash.cmd = cmd
            }
            redirect(action: ACTION_INDEX)
        }
        else {
            flash.cmd = cmd
            ProjectRequest projectRequest = ProjectRequest.findByName(cmd.name)
            if (projectRequest) {
                redirect(action: ACTION_INDEX, id: projectRequest.id)
            } else {
                redirect(action: ACTION_INDEX)
            }
        }
    }

    def approve(ApproveCommand cmd) {
        if (!cmd.validate() && projectRequestService.confirmationRequired(cmd.request)) {
            flash.message = new FlashMessage(g.message(code: "projectRequest.store.failure") as String, cmd.errors)
            redirect(action: ACTION_VIEW, id: cmd.request.id)
            return
        }
        Errors errors = projectRequestService.approveRequest(cmd.request, cmd.comments, cmd.confirmConsent, cmd.confirmRecordOfProcessingActivities)
        if (errors) {
            flash.message = new FlashMessage(g.message(code: "projectRequest.store.failure") as String, errors)
        } else {
            flash.message = new FlashMessage(g.message(code: "projectRequest.store.success") as String)
        }
        redirect(action: ACTION_INDEX)
    }

    def deny(ProjectRequestCommand cmd) {
        if (!cmd.validate()) {
            flash.message = new FlashMessage(g.message(code: "projectRequest.store.failure") as String, cmd.errors)
            redirect(action: ACTION_VIEW, id: cmd.request.id)
            return
        }
        Errors errors = projectRequestService.denyRequest(cmd.request, cmd.comments)
        if (errors) {
            flash.message = new FlashMessage(g.message(code: "projectRequest.store.failure") as String, errors)
        } else {
            flash.message = new FlashMessage(g.message(code: "projectRequest.store.success") as String)
        }
        redirect(action: ACTION_INDEX)
    }

    /**
     * Helper action to transform the POST request into a GET
     */
    def edit(ProjectRequestCommand cmd) {
        redirect(action: ACTION_INDEX, id: cmd.request.id)
    }

    def saveEdit(EditProjectRequestCommand cmd) {
        cmd.save = "save"
        if (!cmd.validate()) {
            flash.message = new FlashMessage(g.message(code: "projectRequest.edit.failure") as String, cmd.errors)
            flash.cmd = cmd
            redirect(action: ACTION_INDEX, id: cmd.request.id)
            return
        }
        String action = ACTION_INDEX
        try {
            projectRequestService.edit(cmd)
            action = ACTION_VIEW
            flash.message = new FlashMessage(g.message(code: "projectRequest.edit.success") as String)
        } catch (ValidationException e) {
            flash.message = new FlashMessage(g.message(code: "projectRequest.edit.failure") as String, e.errors)
            flash.cmd = cmd
        } catch (LdapUserCreationException e) {
            flash.message = new FlashMessage(g.message(code: "projectRequest.edit.failure") as String, [e.message])
            flash.cmd = cmd
        }
        redirect(action: action, id: cmd.request.id)
    }

    def close(ProjectRequestCommand cmd) {
        if (!cmd.validate()) {
            flash.message = new FlashMessage(g.message(code: "projectRequest.close.failure") as String, cmd.errors)
            redirect(action: ACTION_VIEW, id: cmd.request.id)
            return
        }
        Errors errors = projectRequestService.closeRequest(cmd.request)
        if (errors) {
            flash.message = new FlashMessage(g.message(code: "projectRequest.close.failure") as String, errors)
        } else {
            flash.message = new FlashMessage(g.message(code: "projectRequest.close.success") as String)
        }
        redirect(action: ACTION_VIEW, id: cmd.request.id)
    }

    def view(Long id) {
        ProjectRequest projectRequest = projectRequestService.get(id)
        if (!projectRequest) {
            render status: 404
            return
        }
        Map<String, String> abstractValues = projectRequestService.listAdditionalFieldValues(projectRequest)

        List<AbstractFieldDefinition> fieldDefinitions = projectRequestService.listAndFetchAbstractFields(projectRequest.projectType,
                ProjectPageType.PROJECT_REQUEST)

        return sharedModel + [
                projectRequest     : projectRequest,
                eligibleToAccept   : projectRequestService.isCurrentUserEligibleApproverForRequest(projectRequest),
                eligibleToEdit     : projectRequestService.isCurrentUserEligibleToEdit(projectRequest),
                eligibleToClose    : projectRequestService.isCurrentUserEligibleToClose(projectRequest),
                confirmationRequired: projectRequestService.confirmationRequired(projectRequest),
                abstractFields     : fieldDefinitions,
                abstractValues     : abstractValues,
        ]
    }
}

@TupleConstructor
enum StoragePeriod {
    INFINITELY("Store data infinitely, delete on request of PI"),
    TEN_YEARS("Store data for ten years"),
    USER_DEFINED("Store data until given deletion date:"),

    final String description

    String getName() {
        name()
    }
}

class ProjectRequestCreationCommand {
    String save
    String name
    String description

    @BindUsing({ ProjectRequestCreationCommand obj, SimpleMapDataBindingSource source ->
        Object id = source['keywords']
        List<String> ids = id instanceof String[] ? id : [id]
        return ids.findAll()
    })
    List<String> keywords
    LocalDate endDate
    StoragePeriod storagePeriod
    LocalDate storageUntil
    String relatedProjects
    TumorEntity tumorEntity

    @BindUsing({ ProjectRequestCreationCommand obj, SimpleMapDataBindingSource source ->
        Object id = source['speciesWithStrains'].id
        List<Long> ids = id instanceof String[] ? id : [id]
        return ids.collect { SpeciesWithStrain.get(it) }.findAll()
    })
    List<SpeciesWithStrain> speciesWithStrains = []
    String customSpeciesWithStrain
    Project.ProjectType projectType
    String sequencingCenter
    Integer approxNoOfSamples

    @BindUsing({ ProjectRequestCreationCommand obj, SimpleMapDataBindingSource source ->
        Object id = source['seqTypes']?.id
        List<Long> ids = id instanceof String[] ? id : [id]
        return ids.collect { SeqType.get(it) }.findAll()
    })
    List<SeqType> seqTypes = []
    String comments

    List<ProjectRequestUserCommand> users

    List<String> additionalFieldName = []
    Map<String, String> additionalFieldValue = [:]

    static constraints = {
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
        relatedProjects nullable: true, blank: false
        tumorEntity nullable: true
        customSpeciesWithStrain nullable: true
        sequencingCenter nullable: true, blank: false
        approxNoOfSamples nullable: true
        seqTypes nullable: true
        comments nullable: true, blank: false

        users validator: { val, obj ->
            List<ProjectRequestUser> value = val?.toList()?.findAll() ?: []
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

    void setRelatedProjects(String s) {
        relatedProjects = StringUtils.blankToNull(s)
    }

    void setCustomSpeciesWithStrain(String s) {
        customSpeciesWithStrain = StringUtils.blankToNull(s)
    }

    void setSequencingCenter(String s) {
        sequencingCenter = StringUtils.blankToNull(s)
    }

    void setApproxNoOfSamplesString(String number) {
        try {
            this.approxNoOfSamples = Integer.decode(number)
        } catch (NumberFormatException e) {
            this.approxNoOfSamples = null
        }
    }

    void setComments(String s) {
        comments = StringUtils.blankToNull(s)
    }
}

class ProjectRequestUserCommand {
    String username

    Set<ProjectRole> projectRoles
    boolean accessToOtp
    boolean accessToFiles
    boolean manageUsers

    static constraints = {
        projectRoles validator: { val, obj ->
            if (!val) {
                return "empty"
            }
        }
    }
}

class EditProjectRequestCommand extends ProjectRequestCreationCommand {
    ProjectRequest request
}

class ProjectRequestCommand {
    ProjectRequest request
    String comments

    static constraints = {
        comments nullable: true, blank: true
    }

    void setComments(String s) {
        comments = StringUtils.blankToNull(s)
    }
}

class ApproveCommand extends ProjectRequestCommand {
    boolean confirmConsent
    boolean confirmRecordOfProcessingActivities

    static constraints = {
        confirmConsent validator: { val, obj ->
            if (!val) {
                return "confirm"
            }
        }
        confirmRecordOfProcessingActivities validator: { val, obj ->
            if (!val) {
                return "confirm"
            }
        }
    }
}
