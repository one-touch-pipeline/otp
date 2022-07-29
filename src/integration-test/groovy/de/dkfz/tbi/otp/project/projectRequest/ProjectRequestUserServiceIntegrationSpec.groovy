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
package de.dkfz.tbi.otp.project.projectRequest

import grails.testing.mixin.integration.Integration
import grails.transaction.Rollback
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.administration.UserService
import de.dkfz.tbi.otp.domainFactory.UserDomainFactory
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.ProjectRequestUser
import de.dkfz.tbi.otp.security.*

@Rollback
@Integration
class ProjectRequestUserServiceIntegrationSpec extends Specification implements UserAndRoles, UserDomainFactory {

    ProjectRequestUserService projectRequestUserService

    ProjectRequestUserCommand createProjectRequestUserCommand(Map properties = [:]) {
        return new ProjectRequestUserCommand([
                username     : createUser(),
                projectRoles : [createProjectRole()],
                accessToFiles: true,
                manageUsers  : true,
        ] + properties)
    }

    @Unroll
    void "saveProjectRequestUserFromCommand, save ProjectRequestUser and translates all parameters"() {
        given:
        createAllBasicProjectRoles()
        User user = createUser()
        ProjectRequestUser projectRequestUser = requestUserExists ? createProjectRequestUser() : null
        // workaround, since "where" runs before the setup method
        Set<ProjectRole> projectRoles = roles.collect {
            switch (it) {
                case "pi": return pi
                case "other": return other
                case "bioinformatician": return bioinformatician
            }
        } as Set
        ProjectRequestUserCommand cmd = createProjectRequestUserCommand([
                projectRequestUser: projectRequestUser,
                username          : user.username,
                projectRoles      : projectRoles,
                accessToOtp       : accessToOtp,
                accessToFiles     : accessToFiles,
                manageUsers       : manageUsers,
        ])
        projectRequestUserService.userService = Mock(UserService)

        when:
        ProjectRequestUser result = projectRequestUserService.saveProjectRequestUserFromCommand(cmd)

        then:
        1 * projectRequestUserService.userService.findUserByUsername(user.username) >> user
        ProjectRequestUser.count == 1
        result.user == user
        result.projectRoles.containsAll(projectRoles)
        projectRoles.containsAll(result.projectRoles)
        result.accessToOtp == accessToOtp
        result.accessToFiles == accessToFiles
        result.manageUsers == manageUsersResult
        result.manageUsersAndDelegate == manageUsersAndDelegate

        where:
        roles                      | accessToOtp | accessToFiles | manageUsers | manageUsersResult || manageUsersAndDelegate | requestUserExists
        ["pi", "other"]            | true        | false         | false       | true              || true                   | true
        ["bioinformatician"]       | false       | true          | false       | false             || false                  | true
        ["bioinformatician"]       | false       | true          | false       | false             || false                  | false
        ["pi", "bioinformatician"] | true        | true          | true        | true              || true                   | true
        ["pi", "bioinformatician"] | true        | true          | true        | true              || true                   | false
    }

    void "deleteProjectRequestUser, should remove the db entry for the ProjectRequestUser"() {
        given:
        ProjectRequestUser projectRequestUser = createProjectRequestUser()
        createProjectRequestUser()

        when:
        projectRequestUserService.deleteProjectRequestUser(projectRequestUser)

        then:
        ProjectRequestUser.count == 1
    }

    void "saveProjectRequestUser, should save the db entry for the ProjectRequestUser"() {
        given:
        ProjectRequestUser projectRequestUser = createProjectRequestUser([:], false)

        when:
        projectRequestUserService.saveProjectRequestUser(projectRequestUser)

        then:
        ProjectRequestUser.count == 1
    }

    void "ableToDelegateManagement, some example cases"() {
        given:
        createAllBasicProjectRoles()
        createUserAndRoles()

        expect:
        projectRequestUserService.ableToDelegateManagement([pi] as Set<ProjectRole>)
        projectRequestUserService.ableToDelegateManagement([pi, other] as Set<ProjectRole>)
        !projectRequestUserService.ableToDelegateManagement([other, bioinformatician] as Set<ProjectRole>)
        !projectRequestUserService.ableToDelegateManagement([] as Set<ProjectRole>)
    }
}
