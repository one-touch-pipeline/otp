/*
 * Copyright 2011-2026 The OTP authors
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
import groovy.transform.CompileDynamic

import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.security.SecurityService
import de.dkfz.tbi.otp.utils.CollectionUtils

@Transactional
class ProjectRoleService {

    private static final String PI_ROLE_NAME = ProjectRole.Basic.PI.name()
    private static final String COORDINATOR_ROLE_NAME = ProjectRole.Basic.COORDINATOR.name()

    SecurityService securityService

    static boolean projectRolesContainAuthoritativeRole(Set<ProjectRole> projectRoles) {
        return (projectRoles*.name)?.intersect(ProjectRole.AUTHORITY_PROJECT_ROLES)
    }

    static boolean projectRolesContainCoordinator(Set<ProjectRole> projectRoles) {
        return (projectRoles*.name)?.contains(ProjectRole.Basic.COORDINATOR.name())
    }

    @CompileDynamic
    Set<ProjectRole> getRolesCurrentUserCanGrant(Project project) {
        Set<ProjectRole> allRoles = ProjectRole.all as Set
        if (securityService.hasCurrentUserAdministrativeRoles()) {
            return allRoles
        }
        UserProjectRole userProjectRole = CollectionUtils.atMostOneElement(UserProjectRole.findAllByUserAndProject(securityService.currentUser, project))
        if (userProjectRole?.manageUsersAndDelegate) {
            return allRoles - ProjectRole.findAllByName(PI_ROLE_NAME)
        }
        if (userProjectRole?.manageUsers) {
            return allRoles - ProjectRole.findAllByNameInList([PI_ROLE_NAME, COORDINATOR_ROLE_NAME])
        }
        // fallback: user without management permissions
        return [] as Set
    }
}
