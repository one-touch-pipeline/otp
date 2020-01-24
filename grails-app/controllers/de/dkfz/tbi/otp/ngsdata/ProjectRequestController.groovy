/*
 * Copyright 2011-2019 The OTP authors
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
package de.dkfz.tbi.otp.ngsdata

import grails.databinding.BindUsing
import grails.databinding.SimpleMapDataBindingSource
import grails.validation.ValidationException
import groovy.transform.TupleConstructor
import org.springframework.validation.Errors

import de.dkfz.tbi.otp.FlashMessage
import de.dkfz.tbi.otp.ngsdata.taxonomy.SpeciesWithStrain
import de.dkfz.tbi.otp.searchability.Keyword
import de.dkfz.tbi.otp.utils.StringUtils

import java.time.LocalDate

class ProjectRequestController {

    static allowedMethods = [
            index: "GET",
            save : "POST",
            view: "GET",
            update: "POST",
    ]

    ProjectRequestService projectRequestService


    def index() {
        return [
                cmd                       : flash.cmd as ProjectRequestCreationCommand,
                projectTypes              : Project.ProjectType.values(),
                storagePeriods            : StoragePeriod.values(),
                tumorEntities             : TumorEntity.listOrderByName(),
                species                   : SpeciesWithStrain.all.sort { it.toString() },
                keywords                  : Keyword.listOrderByName(),
                seqTypes                  : SeqType.all.sort { it.displayNameWithLibraryLayout },
                awaitingRequests          : projectRequestService.getWaiting(),
                createdAndApprovedRequests: projectRequestService.getCreatedAndApproved(),
        ]
    }

    def save(ProjectRequestCreationCommand cmd) {
        if (!cmd.validate()) {
            flash.message = new FlashMessage(g.message(code: "projectRequest.store.failure") as String, cmd.errors)
            flash.cmd = cmd
            redirect(action: "index")
            return
        }
        try {
            projectRequestService.create(cmd)
            flash.message = new FlashMessage(g.message(code: "projectRequest.store.success") as String)
        } catch (ValidationException e) {
            flash.message = new FlashMessage(g.message(code: "projectRequest.store.failure") as String, e.errors)
            flash.cmd = cmd
        } catch (LdapUserCreationException e) {
            flash.message = new FlashMessage(g.message(code: "projectRequest.store.failure") as String, [e.message])
            flash.cmd = cmd
        }
        redirect(action: "index")
    }

    def view(Long id) {
        ProjectRequest projectRequest = projectRequestService.get(id)
        if (!projectRequest) {
            render status: 404
            return
        }
        return [
                projectRequest: projectRequest,
        ]
    }

    def update(ProjectRequestUpdateCommand cmd) {
        if (!cmd.validate()) {
            flash.message = new FlashMessage(g.message(code: "projectRequest.store.failure") as String, cmd.errors)
            redirect(action: "view", id: cmd.request.id)
            return
        }
        Errors errors = projectRequestService.update(cmd.request, cmd.status, cmd.confirmConsent, cmd.confirmRecordOfProcessingActivities)
        if (errors) {
            flash.message = new FlashMessage(g.message(code: "projectRequest.store.failure") as String, errors)
            redirect(action: "view", id: cmd.request.id)
        } else {
            flash.message = new FlashMessage(g.message(code: "projectRequest.store.success") as String)
            redirect(action: "index")
        }
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
    SpeciesWithStrain speciesWithStrain
    String projectType
    String sequencingCenter
    Integer approxNoOfSamples
    @BindUsing({ ProjectRequestCreationCommand obj, SimpleMapDataBindingSource source ->
        Object id = source['seqType']?.id
        List<String> ids = id instanceof String[] ? id : [id]
        return ids.collect { it?.isLong() ? SeqType.get(it as Long) : null }.findAll()
    })
    List<SeqType> seqType = []
    String comments

    String pi
    List<String> deputyPis
    List<String> responsibleBioinformaticians
    List<String> bioinformaticians
    List<String> submitters

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
        sequencingCenter nullable: true, blank: false
        approxNoOfSamples nullable: true
        seqType validator: { val, obj ->
            if (!val) {
                return "empty"
            }
        }
        comments nullable: true, blank: false
        pi validator: { val, obj ->
            List<String> pi = [val]
            List<String> userWithMultipleRoles = (pi.intersect(obj.deputyPis.findAll()) + pi.intersect(obj.responsibleBioinformaticians.findAll()) + pi.intersect(obj.bioinformaticians.findAll()) + pi.intersect(obj.submitters.findAll()) +
                    obj.deputyPis.intersect(obj.responsibleBioinformaticians.findAll()) + obj.deputyPis.intersect(obj.bioinformaticians.findAll()) + obj.deputyPis.intersect(obj.submitters.findAll()) +
                    obj.responsibleBioinformaticians.intersect(obj.bioinformaticians.findAll()) + obj.responsibleBioinformaticians.intersect(obj.submitters.findAll()) +
                    obj.bioinformaticians.intersect(obj.submitters.findAll())).unique()
            if (userWithMultipleRoles) {
                return ["multiple.roles", userWithMultipleRoles.join(", ")]
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

class ProjectRequestUpdateCommand {
    ProjectRequest request
    String approve
    String deny
    boolean confirmConsent
    boolean confirmRecordOfProcessingActivities

    ProjectRequest.Status getStatus() {
        if (deny) {
            return ProjectRequest.Status.DENIED_BY_PI
        }
        if (approve) {
            return ProjectRequest.Status.APPROVED_BY_PI_WAITING_FOR_OPERATOR
        }
        return null
    }

    static constraints = {
        approve nullable: true, validator: { val, obj ->
            if (val && !(obj.confirmConsent && obj.confirmRecordOfProcessingActivities)) {
                return "confirm"
            }
            if (val == obj.deny) {
                return "invalid"
            }
        }
        deny nullable: true
    }
}
