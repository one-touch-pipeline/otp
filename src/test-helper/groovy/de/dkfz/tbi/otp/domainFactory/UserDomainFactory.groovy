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
package de.dkfz.tbi.otp.domainFactory

import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.ngsdata.ProjectRole
import de.dkfz.tbi.otp.ngsdata.UserProjectRole
import de.dkfz.tbi.otp.project.*
import de.dkfz.tbi.otp.security.*
import de.dkfz.tbi.otp.utils.CollectionUtils

trait UserDomainFactory implements DomainFactoryCore {

    User createUser(Map properties = [:]) {
        return createDomainObject(User, [
                username: "user_${nextId}",
                password: "password_${nextId}",
                email   : "user${nextId}@dummy.de",
                realName: "realName_${nextId}",
                enabled : true,
        ], properties)
    }

    ProjectRole createProjectRole(Map properties = [:]) {
        return createDomainObject(ProjectRole, [
                name: "roleName_${nextId}",
        ], properties)
    }

    UserRole createUserRole(Map properties = [:]) {
        return createDomainObject(UserRole, [
                user: createUser(),
                role: DomainFactory.createRoleLazy(authority: "Role_${nextId}"),
        ], properties)
    }

    UserProjectRole createUserProjectRole(Map properties = [:]) {
        return createDomainObject(UserProjectRole, [
                user                  : { createUser() },
                project               : { createProject() },
                projectRoles          : { [createProjectRole()] },
                accessToOtp           : true,
                accessToFiles         : false,
                manageUsers           : false,
                manageUsersAndDelegate: false,
                receivesNotifications : true,
        ], properties)
    }

    ProjectRequestUser createProjectRequestUser(Map properties = [:], boolean saveAndValidate = true) {
        return createDomainObject(ProjectRequestUser, [
                user         : { createUser() },
                projectRoles : { [createProjectRole()] },
                accessToFiles: true,
                manageUsers  : true,
        ], properties, saveAndValidate)
    }

    ProjectRequestPersistentState createProjectRequestPersistentState(Map properties = [:]) {
        return createDomainObject(ProjectRequestPersistentState, [
                beanName              : "check",
                usersThatNeedToApprove: { [createUser()] },
        ], properties)
    }

    ProjectRequest createProjectRequest(Map properties = [:],
                                        Map stateProperties = [:], boolean saveAndValidate = true) {
        return createDomainObject(ProjectRequest, [
                name       : "name_${nextId}",
                description: "description_${nextId}",
                projectType: Project.ProjectType.SEQUENCING,
                requester  : { createUser() },
                users      : {
                    [
                            createProjectRequestUser([
                                    projectRoles: [createOrGetAuthorityProjectRole()] as Set<ProjectRole>,
                            ]),
                    ]
                },
                state      : createProjectRequestPersistentState(stateProperties),
                project    : stateProperties["beanName"] == "created" ? { createProject() } : null,
        ], properties, saveAndValidate)
    }

    ProjectRole createOrGetAuthorityProjectRole(Map properties = [:]) {
        String projectRoleName = ProjectRole.AUTHORITY_PROJECT_ROLES.first()
        return CollectionUtils.atMostOneElement(ProjectRole.findAllByName(projectRoleName)) ?: createDomainObject(ProjectRole, [
                name: projectRoleName,
        ], properties)
    }

    Department createDepartment(Map properties = [:]) {
        return createDomainObject(Department, [
                ouNumber: "OU${nextId}",
                costCenter: "${nextId}",
                departmentHeads: { [createUser()] },
        ], properties)
    }

    PIUser createPIUser(Map properties = [:]) {
        return createDomainObject(PIUser, [
                pi: { createUser() },
                deputyPI: { createUser() },
                dateRightsGranted: new Date(),
        ], properties)
    }
}

class UserDomainFactoryInstance implements UserDomainFactory {
    final static UserDomainFactoryInstance INSTANCE = new UserDomainFactoryInstance()
}
