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

import de.dkfz.tbi.otp.Comment
import de.dkfz.tbi.otp.CommentableWithHistory
import de.dkfz.tbi.otp.ngsdata.ProjectRoleService
import de.dkfz.tbi.otp.ngsdata.SeqType
import de.dkfz.tbi.otp.ngsdata.taxonomy.SpeciesWithStrain
import de.dkfz.tbi.otp.project.additionalField.AbstractFieldValue
import de.dkfz.tbi.otp.security.User
import de.dkfz.tbi.otp.utils.Entity

import static de.dkfz.tbi.otp.utils.CollectionUtils.atMostOneElement

class ProjectRequest implements ProjectPropertiesGivenWithRequest, Entity, CommentableWithHistory {
    User requester

    Set<ProjectRequestUser> users
    Project project

    ProjectRequestPersistentState state
    Set<String> customSpeciesWithStrains
    Set<String> keywords
    Set<String> sequencingCenters
    Integer approxNoOfSamples
    Set<SeqType> seqTypes
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

        users validator: { val, obj ->
            List<ProjectRequestUser> value = val?.toList()?.findAll() ?: []
            if (!value.findAll().any { ProjectRequestUser user ->
                ProjectRoleService.projectRolesContainAuthoritativeRole(user.projectRoles)
            }) {
                return "projectRequest.users.no.authority"
            }
            if (value*.username.size() != value*.username.unique().size()) {
                return "projectRequest.users.unique"
            }
        }
    }

    static hasMany = [
            seqTypes          : SeqType,
            users             : ProjectRequestUser,
            projectFields     : AbstractFieldValue,
            speciesWithStrains: SpeciesWithStrain,
    ]

    static mapping = {
        description type: "text"
        requester index: "project_request_requester_idx"
        users index: "project_request_users_idx"
        seqTypes index: "project_request_seqTypes_idx"
    }
}
