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

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.domainFactory.UserDomainFactory
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.security.UserAndRoles

import static de.dkfz.tbi.otp.ngsdata.ProjectRole.Basic.*

@Rollback
@Integration
class ProjectRoleServiceIntegrationSpec extends Specification implements UserAndRoles, DomainFactoryCore, UserDomainFactory {

    ProjectRoleService projectRoleService

    @Unroll
    void "test getRolesCurrentUserCanGrant, for #description"() {
        given:
        createUserAndRoles()
        createAllBasicProjectRoles()
        Project project = createProject()
        if (manageUsers != null) {
            createUserProjectRole(
                    project               : project,
                    user                  : getUser(role),
                    manageUsers           : manageUsers,
                    manageUsersAndDelegate: canDelegate,
            )
        }

        Set<ProjectRole> projectRoles = [] as Set

        when:
        doWithAuth(role) {
            projectRoles.addAll(projectRoleService.getRolesCurrentUserCanGrant(project))
        }

        then:
        TestCase.assertContainSame(projectRoles, result())

        // For admin/operator null means they don't need a UserProjectRole since they get access via their system role
        where:
        description                              | role     | manageUsers | canDelegate | result
        "admin"                                  | ADMIN    | null        | null        | { allRolesExcept() }
        "operator"                               | OPERATOR | null        | null        | { allRolesExcept() }
        "manageUsersAndDelegate"                 | USER     | true        | true        | { allRolesExcept(PI) }
        "manageUsers only"                       | TESTUSER | true        | false       | { allRolesExcept(PI, COORDINATOR) }
        "explicit false management permissions"  | TESTUSER | false       | false       | { [] }
        "no management permissions"              | USER     | null        | null        | { [] }
    }

    private Set<ProjectRole> allRolesExcept(ProjectRole.Basic... exclusions) {
        Set<ProjectRole> allRoles = ProjectRole.all as Set
        if (!exclusions) {
            return allRoles
        }
        return allRoles - ProjectRole.findAllByNameInList(exclusions*.name())
    }
}
