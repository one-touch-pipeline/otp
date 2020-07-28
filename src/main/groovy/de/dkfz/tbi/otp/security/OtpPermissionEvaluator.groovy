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
package de.dkfz.tbi.otp.security

import grails.compiler.GrailsCompileStatic
import groovy.transform.CompileDynamic
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.access.PermissionEvaluator
import org.springframework.security.acls.AclPermissionEvaluator
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.ngsdata.UserProjectRole

@Component
@GrailsCompileStatic
class OtpPermissionEvaluator implements PermissionEvaluator {

    @Autowired AclPermissionEvaluator aclPermissionEvaluator

    private static final List PERMISSIONS = ["OTP_READ_ACCESS", "MANAGE_USERS", "DELEGATE_USER_MANAGEMENT", "ADD_USER"]

    @Override
    boolean hasPermission(Authentication auth, Object targetDomainObject, Object permission) throws IllegalArgumentException {
        if (!auth) {
            return false
        }
        if (permission instanceof String && permission in PERMISSIONS) {
            switch (targetDomainObject?.class) {
                case Project:
                    return checkProjectRolePermission(auth, (Project) targetDomainObject, permission)
                case null:
                    return checkObjectIndependentPermission(auth, permission)
                default:
                    return false
            }
        }
        return aclPermissionEvaluator.hasPermission(auth, targetDomainObject, permission)
    }

    @Override
    boolean hasPermission(Authentication auth, Serializable targetId, String targetType, Object permission) throws IllegalArgumentException {
        if (!auth) {
            return false
        }
        if (permission instanceof String && permission in PERMISSIONS) {
            switch (targetType) {
                case Project.name:
                    return checkProjectRolePermission(auth, Project.get(targetId), (String) permission)
                case null:
                    return checkObjectIndependentPermission(auth, permission)
                default:
                    return false
            }
        }
        return aclPermissionEvaluator.hasPermission(auth, targetId, targetType, permission)
    }

    @CompileDynamic
    private boolean checkObjectIndependentPermission(Authentication auth, String permission) {
        User activeUser = User.findByUsername(auth.principal.username)
        if (!activeUser) {
            return false
        }

        if (!activeUser.enabled) {
            return false
        }

        switch (permission) {
            case "ADD_USER":
                return UserProjectRole.createCriteria().list {
                    eq("user", activeUser)
                    or {
                        eq("manageUsers", true)
                        eq("manageUsersAndDelegate", true)
                    }
                    eq("enabled", true)
                }
            default:
                return false
        }
    }

    @CompileDynamic
    private boolean checkProjectRolePermission(Authentication auth, Project project, String permission) {
        User activeUser = User.findByUsername(auth.principal.username)
        if (!activeUser) {
            return false
        }

        UserProjectRole userProjectRole = UserProjectRole.findByProjectAndUser(project, activeUser)
        if (!userProjectRole) {
            return false
        }
        if (!(activeUser.enabled && userProjectRole.enabled)) {
            return false
        }

        switch (permission) {
            case "OTP_READ_ACCESS":
                return userProjectRole.accessToOtp
            case "MANAGE_USERS":
                return userProjectRole.manageUsers
            case "DELEGATE_USER_MANAGEMENT":
                return userProjectRole.manageUsersAndDelegate
            default:
                return false
        }
    }
}
