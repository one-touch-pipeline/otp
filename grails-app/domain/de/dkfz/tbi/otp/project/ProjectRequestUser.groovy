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

import de.dkfz.tbi.otp.ngsdata.ProjectRole
import de.dkfz.tbi.otp.security.User
import de.dkfz.tbi.otp.utils.Entity
import de.dkfz.tbi.otp.utils.StringUtils

class ProjectRequestUser implements Entity {

    User user

    Set<ProjectRole> projectRoles
    boolean accessToOtp
    boolean accessToFiles
    boolean manageUsers
    boolean manageUsersAndDelegate

    static hasMany = [
        projectRoles: ProjectRole
    ]

    static constraints = {
        projectRoles validator: { val, obj ->
            if (!val) {
                return "empty"
            }
        }
    }

    static mapping = {
        projectRoles index: "project_request_user_project_roles_idx"
    }

    /**
     * This is a necessary helper so we can use this object in conjunction with ProjectRequestUserCommand
     * in the template projectRequestUserForm.
     */
    String getUsername() {
        return user.username
    }

    @Override
    String toString() {
        return "PRU ${id}: ${user} ${projectRolesAsSemanticString}"
    }

    String getProjectRolesAsSemanticString() {
        return StringUtils.joinAsSemanticString(projectRoles.sort { it.name }, ", ", " and ")
    }
}
