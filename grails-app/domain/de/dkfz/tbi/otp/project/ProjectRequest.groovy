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
package de.dkfz.tbi.otp.project

import grails.gorm.hibernate.annotation.ManagedEntity

import de.dkfz.tbi.otp.Comment
import de.dkfz.tbi.otp.CommentableWithHistory
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.taxonomy.SpeciesWithStrain
import de.dkfz.tbi.otp.project.additionalField.AbstractFieldValue
import de.dkfz.tbi.otp.searchability.Keyword
import de.dkfz.tbi.otp.security.User
import de.dkfz.tbi.otp.utils.Entity

import static de.dkfz.tbi.otp.utils.CollectionUtils.atMostOneElement

@ManagedEntity
class ProjectRequest implements ProjectPropertiesGivenWithRequest, Entity, CommentableWithHistory {
    User requester

    Set<ProjectRequestUser> piUsers
    Set<ProjectRequestUser> users
    Project project

    ProjectRequestPersistentState state
    Set<String> customSpeciesWithStrains
    Set<SeqCenter> sequencingCenters
    Set<String> customSequencingCenters
    Integer approxNoOfSamples
    Set<SeqType> seqTypes
    Set<String> customSeqTypes
    String requesterComment
    List<Comment> comments

    static constraints = {
        project nullable: true, validator: { Project val, ProjectRequest obj ->
            if (!val && obj.state.beanName == 'created') {
                return "required"
            }
            if (val && obj.state.beanName != 'created') {
                return "illegal"
            }
        }
        name blank: false, unique: true, validator: { String val, ProjectRequest obj ->
            Project project = atMostOneElement(Project.findAllByName(val))
            if (project && project != obj.project) {
                return "duplicate.project"
            }
        }
        description blank: false
        endDate nullable: true
        storageUntil nullable: true
        relatedProjects nullable: true
        tumorEntity nullable: true
        speciesWithStrains nullable: true
        approxNoOfSamples nullable: true
        requesterComment nullable: true

        piUsers validator: { val, obj ->
            List<ProjectRequestUser> value = val?.toList()?.findAll() ?: []
            if (!value.findAll().any { ProjectRequestUser piUser ->
                ProjectRoleService.projectRolesContainAuthoritativeRole(piUser.projectRoles)
            }) {
                return "projectRequest.users.no.authority"
            }
            if (value*.username.size() != value*.username.unique().size() || !val*.username.intersect(obj.users*.username).isEmpty()) {
                return "projectRequest.users.unique"
            }
        }
        users validator: { val, obj ->
            List<ProjectRequestUser> value = val?.toList()?.findAll() ?: []
            if (value*.username.size() != value*.username.unique().size() || !val*.username.intersect(obj.piUsers*.username).isEmpty()) {
                return "projectRequest.users.unique"
            }
        }
    }

    static hasMany = [
            seqTypes          : SeqType,
            piUsers           : ProjectRequestUser,
            users             : ProjectRequestUser,
            projectFields     : AbstractFieldValue,
            speciesWithStrains: SpeciesWithStrain,
            sequencingCenters : SeqCenter,
            keywords          : Keyword,
    ]

    static Closure mapping = {
        description type: "text"
        requester index: "project_request_requester_idx"
        users index: "project_request_users_idx"
        piUsers joinTable: "project_request_pi_user"
        seqTypes index: "project_request_seqTypes_idx"
        state index: "project_request_state_idx"
    }
}
