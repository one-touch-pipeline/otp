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

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.security.UserAndRoles

@Rollback
@Integration
class ProjectRoleServiceIntegrationSpec extends Specification implements UserAndRoles {

    ProjectRoleService projectRoleService

    @Unroll
    void "test listAvailableProjectRolesAuthByCurrentUser, for role #role"() {
        given:
        createUserAndRoles()
        createAllBasicProjectRoles()

        List<ProjectRole> projectRoleList = []

        when:
        doWithAuth(role) {
            projectRoleList.addAll(projectRoleService.listAvailableProjectRolesAuthenticatedByCurrentUser())
        }

        then:
        TestCase.assertContainSame(projectRoleList, result())

        where:
        role     | result
        ADMIN    | { ProjectRole.all }
        OPERATOR | { ProjectRole.all }
        USER     | { ProjectRole.all - ProjectRole.findAllByName(ProjectRole.Basic.PI.name()) }
        TESTUSER | { ProjectRole.all - ProjectRole.findAllByName(ProjectRole.Basic.PI.name()) }
    }
}
