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
package de.dkfz.tbi.otp.ngsdata

import grails.gorm.transactions.Transactional
import org.springframework.security.access.prepost.PreAuthorize

import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.security.user.RolesService
import de.dkfz.tbi.otp.security.user.UserService

@Transactional
class MmmlService {

    RolesService rolesService
    UserService userService

    static final Collection PROJECT_TO_HIDE_SAMPLE_IDENTIFIER = [
            "ICGC_MMML",
            "ICGC_MMML_XP",
            "ICGC_MMML_RARE_LYMPHOMA_XP",
            "ICGC_MMML_RARE_LYMPHOMA_EXOMES",
    ].asImmutable()

    /**
     * determine, if the column sample name should be hidden in the view
     */
    static boolean hideSampleIdentifier(Project project) {
        return PROJECT_TO_HIDE_SAMPLE_IDENTIFIER.contains(project?.name)
    }

    boolean hideSampleIdentifiersForCurrentUser(Project project) {
        if (rolesService.isAdministrativeUser(userService.currentUser)) {
            return false
        }
        return hideSampleIdentifier(project)
    }

    @PreAuthorize("hasRole('ROLE_MMML_MAPPING')")
    List tableForMMMLMapping() {
        def seq = Individual.withCriteria {
            project {
                'in'("name", PROJECT_TO_HIDE_SAMPLE_IDENTIFIER)
            }
            projections {
                property("id")
                property("mockFullName")
                property("internIdentifier")
            }
            order("id", "desc")
        }
        return seq
    }
}
