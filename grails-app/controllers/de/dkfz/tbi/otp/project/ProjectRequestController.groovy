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
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.taxonomy.SpeciesWithStrain
import de.dkfz.tbi.otp.searchability.Keyword
import de.dkfz.tbi.otp.utils.StringUtils
import de.dkfz.tbi.util.MultiObjectValueSource

import java.time.LocalDate

@Secured(['ROLE_OPERATOR', 'ROLE_TEST_PI'])
class ProjectRequestController {

    static allowedMethods = [
            index   : "GET",
            open    : "GET",
            resolved: "GET",
            save    : "POST",
            edit    : "POST",
            saveEdit: "POST",
            view    : "GET",
    ]

    private static final String ACTION_INDEX = "index"
    private static final String ACTION_OPEN = "open"
    private static final String ACTION_VIEW = "view"

    ProjectRequestService projectRequestService

    private Map getSharedModel() {
        List<ProjectRequest> unresolved = projectRequestService.unresolvedRequestsOfUser
        List<ProjectRequest> resolved = projectRequestService.resolvedOfCurrentUser

        return [
                actionHighlight: unresolved ? "work-but-nothing-todo" : "no-work-and-nothing-todo",
                unresolved     : unresolved,
                resolved       : resolved,
        ]
    }

    def index(Long id) {
        ProjectRequest projectRequest = projectRequestService.get(id)
        Map<String, ?> defaults = [
                keywords         : [""],
                seqTypes         : [null],
                users            : [],
                speciesWithStrain: [id: null],
        ]
        Map<String, ?> projectRequestHelper = [:]
        if (projectRequest) {
            projectRequestHelper << [
                users        : projectRequest.users ?: [],
                storagePeriod: projectRequest.storageUntil ? StoragePeriod.USER_DEFINED : StoragePeriod.INFINITELY,
            ]
        }

        MultiObjectValueSource multiObjectValueSource = new MultiObjectValueSource(flash.cmd, projectRequestHelper, projectRequest, defaults)
        return sharedModel + [
                cmd                 : flash.cmd as ProjectRequestCreationCommand,
                projectRequestToEdit: projectRequest,
                projectTypes        : Project.ProjectType.values(),
                storagePeriods      : StoragePeriod.values(),
                tumorEntities       : TumorEntity.listOrderByName(),
                species             : SpeciesWithStrain.all.sort { it.displayString } + [id: "other", displayString: "Other(s)"],
                keywords            : Keyword.listOrderByName(),
                seqTypes            : SeqType.all.sort { it.displayNameWithLibraryLayout },
                availableRoles      : ProjectRole.findAll(),
                source              : multiObjectValueSource,
        ]
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
                redirect(action: ACTION_OPEN)
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

    def approve(ApproveCommand cmd) {
        if (!cmd.validate()) {
            flash.message = new FlashMessage(g.message(code: "projectRequest.store.failure") as String, cmd.errors)
            redirect(action: ACTION_VIEW, id: cmd.request.id)
            return
        }
        Errors errors = projectRequestService.approveRequest(cmd.request, cmd.confirmConsent, cmd.confirmRecordOfProcessingActivities)
        if (errors) {
            flash.message = new FlashMessage(g.message(code: "projectRequest.store.failure") as String, errors)
        } else {
            flash.message = new FlashMessage(g.message(code: "projectRequest.store.success") as String)
        }
        redirect(action: ACTION_VIEW, id: cmd.request.id)
    }

    def deny(ProjectRequestCommand cmd) {
        if (!cmd.validate()) {
            flash.message = new FlashMessage(g.message(code: "projectRequest.store.failure") as String, cmd.errors)
            redirect(action: ACTION_VIEW, id: cmd.request.id)
            return
        }
        Errors errors = projectRequestService.denyRequest(cmd.request)
        if (errors) {
            flash.message = new FlashMessage(g.message(code: "projectRequest.store.failure") as String, errors)
        } else {
            flash.message = new FlashMessage(g.message(code: "projectRequest.store.success") as String)
        }
        redirect(action: ACTION_VIEW, id: cmd.request.id)
    }

    /**
     * Helper action to transform the POST request into a GET
     */
    def edit(ProjectRequestCommand cmd) {
        redirect(action: ACTION_INDEX, id: cmd.request.id)
    }

    def saveEdit(EditProjectRequestCommand cmd) {
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
        return sharedModel + [
                projectRequest  : projectRequest,
                eligibleToAccept: projectRequestService.isCurrentUserEligibleApproverForRequest(projectRequest),
                eligibleToEdit  : projectRequestService.isCurrentUserEligibleToEdit(projectRequest),
                eligibleToClose : projectRequestService.isCurrentUserEligibleToClose(projectRequest),
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
    String name
    String description
    @BindUsing({ ProjectRequestCreationCommand obj, SimpleMapDataBindingSource source ->
        Object id = source['keywords']
        List<String> ids = id instanceof String[] ? id : [id]
        return ids.findAll()
    })
    List<String> keywords
    String organizationalUnit
    String costCenter
    String fundingBody
    String grantId
    LocalDate endDate
    StoragePeriod storagePeriod
    LocalDate storageUntil
    String relatedProjects
    TumorEntity tumorEntity
    @BindUsing({ ProjectRequestCreationCommand obj, SimpleMapDataBindingSource source ->
        String id = source['speciesWithStrain']?.id
        return id?.isLong() ? SpeciesWithStrain.get(id as Long) : null
    })
    SpeciesWithStrain speciesWithStrain
    String customSpeciesWithStrain
    Project.ProjectType projectType
    String sequencingCenter
    Integer approxNoOfSamples
    @BindUsing({ ProjectRequestCreationCommand obj, SimpleMapDataBindingSource source ->
        Object id = source['seqTypes']?.id
        List<String> ids = id instanceof String[] ? id : [id]
        return ids.collect { it?.isLong() ? SeqType.get(it as Long) : null }.findAll()
    })
    List<SeqType> seqTypes = []
    String comments

    List<ProjectRequestUserCommand> users

    static constraints = {
        name blank: false
        description blank: false
        keywords validator: { val, obj ->
            if (!val) {
                return "empty"
            }
        }
        organizationalUnit blank: false
        costCenter nullable: true, blank: false
        fundingBody nullable: true, blank: false
        grantId nullable: true, blank: false
        endDate nullable: true
        storageUntil nullable: true, validator: { val, obj ->
            if (obj.storagePeriod == StoragePeriod.USER_DEFINED && !val) {
                return "empty"
            }
        }
        relatedProjects nullable: true, blank: false
        tumorEntity nullable: true
        speciesWithStrain nullable: true
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

    void setCostCenter(String s) {
        costCenter = StringUtils.blankToNull(s)
    }

    void setFundingBody(String s) {
        fundingBody = StringUtils.blankToNull(s)
    }

    void setGrantId(String s) {
        grantId = StringUtils.blankToNull(s)
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
