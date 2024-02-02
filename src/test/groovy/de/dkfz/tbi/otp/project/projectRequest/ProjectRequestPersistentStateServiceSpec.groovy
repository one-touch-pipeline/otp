/*
 * Copyright 2011-2024 The OTP authors
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

import grails.testing.gorm.DataTest
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.domainFactory.UserDomainFactory
import de.dkfz.tbi.otp.ngsdata.ProjectRole
import de.dkfz.tbi.otp.project.*
import de.dkfz.tbi.otp.security.User
import de.dkfz.tbi.otp.security.UserAndRoles
import de.dkfz.tbi.otp.utils.CollectionUtils

class ProjectRequestPersistentStateServiceSpec extends Specification implements UserDomainFactory, UserAndRoles, DataTest {

    ProjectRequestPersistentStateService service

    @Override
    Class[] getDomainClassesToMock() {
        return [
                User,
                ProjectRequestPersistentState,
                ProjectRole,
                ProjectRequestUser,
                ProjectRequest,
        ]
    }

    void setup() {
        service = new ProjectRequestPersistentStateService(
                projectRequestStateProvider: Mock(ProjectRequestStateProvider)
        )
    }

    @Unroll
    void "getAllProjectRequestAuthorities, should return all project request authorities"() {
        given:
        ProjectRequestPersistentState projectRequestPersistentState = createProjectRequestPersistentState([
                usersThatNeedToApprove  : usersThatNeedToApprove,
                usersThatAlreadyApproved: usersThatAlreadyApproved,
        ])

        when:
        Set<User> result = ProjectRequestPersistentStateService.getAllProjectRequestAuthorities(projectRequestPersistentState)

        then:
        result.containsAll(pis)
        pis.containsAll(result)

        where:
        pis                                                      | usersThatNeedToApprove | usersThatAlreadyApproved
        [createUser(), createUser(), createUser(), createUser()] | [pis[0], pis[1]]       | [pis[2], pis[3]]
        [createUser(), createUser()]                             | null                   | [pis[0], pis[1]]
        [createUser(), createUser()]                             | [pis[0], pis[1]]       | null
        []                                                       | null                   | null
    }

    void "deleteProjectRequestState, should delete a ProjectRequestState"() {
        given:
        ProjectRequestPersistentState projectRequestPersistentState = createProjectRequestPersistentState()

        when:
        service.deleteProjectRequestState(projectRequestPersistentState)

        then:
        ProjectRequestPersistentState.count == 0
    }

    @Unroll
    void "allProjectRequestUsersApproved, should return #expected, when #caseDescription"() {
        given:
        ProjectRequestPersistentState projectRequestPersistentState = createProjectRequestPersistentState([
                usersThatNeedToApprove  : usersThatNeedToApprove,
                usersThatAlreadyApproved: usersThatAlreadyApproved,
        ])

        expect:
        service.allProjectRequestUsersApproved(projectRequestPersistentState) == expected

        where:
        caseDescription                        | usersThatNeedToApprove       | usersThatAlreadyApproved     | expected
        "two approved and two need to approve" | [createUser(), createUser()] | [createUser(), createUser()] | false
        "all users approved"                   | []                           | [createUser(), createUser()] | true
        "no users approved"                    | [createUser(), createUser()] | []                           | false
        "no users need to approve"             | []                           | []                           | true
    }

    void "approveUser, should throw an AssertionError when users doesnt need to approve"() {
        given:
        User currentUser = createUser()
        ProjectRequestPersistentState projectRequestPersistentState = createProjectRequestPersistentState([
                usersThatNeedToApprove  : [createUser(), createUser()],
                usersThatAlreadyApproved: [currentUser, createUser()],
        ])

        when:
        service.approveUser(projectRequestPersistentState, currentUser)

        then:
        thrown(AssertionError)
    }

    void "approveUser, should move the user to approved list that approves a request"() {
        given:
        User currentUser = createUser()
        ProjectRequestPersistentState projectRequestPersistentState = createProjectRequestPersistentState([
                usersThatNeedToApprove  : [currentUser, createUser()],
                usersThatAlreadyApproved: [createUser(), createUser()],
        ])

        when:
        service.approveUser(projectRequestPersistentState, currentUser)

        then:
        !projectRequestPersistentState.usersThatNeedToApprove.contains(currentUser)
        projectRequestPersistentState.usersThatAlreadyApproved.contains(currentUser)
    }

    void "setCurrentOwner, should set the current user and save it to the database"() {
        given:
        User user = createUser()
        ProjectRequestPersistentState projectRequestPersistentState = createProjectRequestPersistentState()

        when:
        service.setCurrentOwner(projectRequestPersistentState, user)

        then:
        CollectionUtils.exactlyOneElement(ProjectRequestPersistentState.findAllByCurrentOwner(user)) == projectRequestPersistentState
    }

    void "removeCurrentOwner, should remove the current user and save this to the database"() {
        given:
        User user = createUser()
        ProjectRequestPersistentState projectRequestPersistentState = createProjectRequestPersistentState([
                currentOwner: user,
        ])

        expect:
        ProjectRequestPersistentState.findAllByCurrentOwner(user).size() == 1

        when:
        service.removeCurrentOwner(projectRequestPersistentState)

        then:
        projectRequestPersistentState.currentOwner == null
        ProjectRequestPersistentState.findAll().each {
            assert it.currentOwner != user
        }
    }

    void "saveProjectRequestStateForProjectRequest, should save new arguments and evaluate users that need to approve and not create a new db entry"() {
        given:
        createAllBasicProjectRoles()
        ProjectRequestUser projectAuthority1 = createProjectRequestUser([
                projectRoles: [pi]
        ])
        ProjectRequestUser projectAuthority2 = createProjectRequestUser([
                projectRoles: [pi]
        ])
        List<ProjectRequestUser> projectRequestUsers = [createProjectRequestUser(), projectAuthority1, createProjectRequestUser(), projectAuthority2]
        ProjectRequest projectRequest = createProjectRequest([
                requester: projectRequestUsers[0].user,
        ], [
                beanName                : "check",
                usersThatAlreadyApproved: [projectAuthority1, createProjectRequestUser()]*.user,
                usersThatNeedToApprove  : [projectAuthority2, createProjectRequestUser()]*.user,
        ])

        expect:
        ProjectRequestPersistentState.findAll().size() == 1

        when:
        ProjectRequestPersistentState projectRequestPersistentState = service.saveProjectRequestPersistentStateForProjectRequest(projectRequest, projectRequestUsers)

        then:
        1 * service.projectRequestStateProvider.getCurrentState(projectRequest) >> new Check()
        projectRequestPersistentState.currentOwner == null
        projectRequestPersistentState.beanName == projectRequest.state.beanName
        projectRequestPersistentState.usersThatAlreadyApproved == [] as Set
        projectRequestPersistentState.usersThatNeedToApprove == [projectAuthority1, projectAuthority2]*.user as Set
        ProjectRequestPersistentState.count == 1
    }

    void "saveProjectRequestStateForProjectRequest, should create a new ProjectRequestPersistentState if projectRequest is null"() {
        given:
        createAllBasicProjectRoles()
        ProjectRequestUser projectAuthority1 = createProjectRequestUser([
                projectRoles: [pi]
        ])
        ProjectRequestUser projectAuthority2 = createProjectRequestUser([
                projectRoles: [pi]
        ])
        List<ProjectRequestUser> projectRequestUsers = [createProjectRequestUser(), projectAuthority1, createProjectRequestUser(), projectAuthority2]

        expect:
        ProjectRequestPersistentState.findAll().size() == 0

        when:
        ProjectRequestPersistentState projectRequestPersistentState = service.saveProjectRequestPersistentStateForProjectRequest(null, projectRequestUsers)

        then:
        1 * service.projectRequestStateProvider.getCurrentState(null) >> new Initial()
        projectRequestPersistentState.currentOwner == null
        projectRequestPersistentState.beanName == "initial"
        projectRequestPersistentState.usersThatAlreadyApproved == [] as Set
        projectRequestPersistentState.usersThatNeedToApprove == [projectAuthority1.user, projectAuthority2.user] as Set
        ProjectRequestPersistentState.count == 1
    }
}
