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

import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.security.User
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.utils.Entity

/** This table is used externally. Please discuss a change in the team */
class UserProjectRole implements Serializable, Entity {

    /** This attribute is used externally. Please discuss a change in the team */
    Project project
    /** This attribute is used externally. Please discuss a change in the team */
    User user
    /** This attribute is used externally. Please discuss a change in the team */
    Set<ProjectRole> projectRoles
    /** This attribute is used externally. Please discuss a change in the team */
    boolean enabled = true
    boolean accessToOtp = false
    boolean accessToFiles = false
    boolean manageUsers = false
    boolean manageUsersAndDelegate = false
    /** This attribute is used externally. Please discuss a change in the team */
    boolean receivesNotifications = true

    /**
     * If a file change was requested and not yet executed
     */
    boolean fileAccessChangeRequested = false

    static hasMany = [
        projectRoles: ProjectRole
    ]

    static constraints = {
        project(unique: 'user')
    }

    static List<String> getAccessRelatedProperties() {
        return [
                "projectRoles",
                "enabled",
                "accessToOtp",
                "accessToFiles",
                "manageUsers",
                "manageUsersAndDelegate",
                "receivesNotifications",
        ]
    }

    boolean equalByAccessRelatedProperties(UserProjectRole userProjectRole) {
        return accessRelatedProperties.every { this."$it" == userProjectRole."$it" }
    }

    boolean isPi() {
        return projectRoles.contains(CollectionUtils.exactlyOneElement(ProjectRole.findAllByName(ProjectRole.Basic.PI.name())))
    }

    @Override
    String toString() {
        return "UPR ${id}: ${project} ${user} ${projectRoles}"
    }

    String toStringWithAllProperties() {
        return [
                ["user", user],
                ["project", project],
                ["projectRoles", projectRoles*.name.join(", ")],
                ["enabled", enabled],
                ["accessToOtp", accessToOtp],
                ["accessToFiles", accessToFiles],
                ["manageUsers", manageUsers],
                ["manageUsersAndDelegate", manageUsersAndDelegate],
                ["receivesNotifications", receivesNotifications],
                ["fileAccessChangeRequested", fileAccessChangeRequested],
        ]*.join(": ").join("; ")
    }
}
