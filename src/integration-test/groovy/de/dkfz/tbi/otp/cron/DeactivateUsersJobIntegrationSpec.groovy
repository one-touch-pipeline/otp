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
package de.dkfz.tbi.otp.cron

import grails.testing.mixin.integration.Integration
import grails.transaction.Rollback
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.administration.LdapService
import de.dkfz.tbi.otp.administration.UserService
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.security.User
import de.dkfz.tbi.otp.utils.MailHelperService

@Rollback
@Integration
class DeactivateUsersJobIntegrationSpec extends Specification implements DomainFactoryCore {

    void "getUsersToCheck, only returns users with a username and plannedDeactivationDate before now"() {
        given:
        Date date = new Date(System.currentTimeMillis() + dateOffset * 24 * 60 * 60 * 1000)
        User user = DomainFactory.createUser(username: username, plannedDeactivationDate: date)

        expect:
        TestCase.assertContainSame(found ? [user] : [], DeactivateUsersJob.usersToCheck)

        where:
        dateOffset | username   || found
        -1         | "username" || true
        -1         | null       || false
        +1         | "username" || false
        +1         | null       || false
    }

    void "isInGroup, checks list returned from ldapService"() {
        given:
        DeactivateUsersJob job = new DeactivateUsersJob([
                ldapService: Mock(LdapService) {
                    _ * getGroupsOfUser(_) >> {
                        return ["GroupA", "GroupB"]
                    }
                },
        ])

        expect:
        expected == job.isInGroup(DomainFactory.createUser(), groupToCheck)

        where:
        groupToCheck || expected
        "GroupA"     || true
        "GroupC"     || false
    }

    void "notifyAdministration, uses different prefix depending on groups"() {
        given:
        DeactivateUsersJob job = new DeactivateUsersJob([
                processingOptionService: Mock(ProcessingOptionService) {
                    _ * findOptionAsString(_) { return "option" }
                    _ * findOptionAsLong(_) { return 0L }
                },
                mailHelperService: Mock(MailHelperService) {
                    1 * sendEmail({ it.contains(expectedContent) }, _, _) >> { }
                },
                userProjectRoleService: Mock(UserProjectRoleService) {
                    _ * commandTemplate(_, _) >> "removal command"
                }
        ])

        expect:
        job.notifyAdministration(DomainFactory.createUser(), groups as Set<String>)

        where:
        groups     | expectedContent
        ["A", "B"] | "TODO"
        []         | "DONE"
    }

    void "disableUserAndNotify, disables given user and sends notification mail"() {
        given:
        DeactivateUsersJob job = new DeactivateUsersJob([
                processingOptionService: Mock(ProcessingOptionService) {
                    _ * findOptionAsString(_) { return "option" }
                    _ * findOptionAsLong(_) { return 0L }
                },
                ldapService: Mock(LdapService) {
                    _ * getGroupsOfUser(_) >> { return ["group"] }
                },
                userService: new UserService(),
                mailHelperService: Mock(MailHelperService) {
                    1 * sendEmail(_, _, _) >> { }
                },
                userProjectRoleService: new UserProjectRoleService(),
        ])
        job.userProjectRoleService.ldapService = Mock(LdapService) {
            isUserInLdapAndActivated(_) >> false
        }

        User user = DomainFactory.createUser()
        List<UserProjectRole> userProjectRoles = [
                DomainFactory.createUserProjectRole(user: user),
                DomainFactory.createUserProjectRole(user: user),
                DomainFactory.createUserProjectRole(user: user, enabled: false, accessToOtp: false, receivesNotifications: false),
        ]

        when:
        job.disableUserAndNotify(user)

        then:
        userProjectRoles.every { !it.enabled }
        userProjectRoles.every { !it.accessToOtp }
        userProjectRoles.every { !it.accessToFiles }
        userProjectRoles.every { !it.manageUsers }
        userProjectRoles.every { !it.manageUsersAndDelegate }
        userProjectRoles.every { !it.receivesNotifications }
        user.plannedDeactivationDate == null
    }

    @Unroll
    void "wrappedExecute, calls disableUserAndNotify for all users found by getUsersToCheck"() {
        given:
        DeactivateUsersJob job = new DeactivateUsersJob([
                processingOptionService: Mock(ProcessingOptionService) {
                    _ * findOptionAsString(_) { return "option" }
                    _ * findOptionAsLong(_) { return 0L }
                },
                ldapService: Mock(LdapService) {
                    1 * getGroupsOfUser(_) >> { return ["group"] }
                },
                userService: new UserService(),
                mailHelperService: Mock(MailHelperService) {
                    1 * sendEmail(_, _, _) >> { }
                },
                userProjectRoleService: new UserProjectRoleService(),
        ])
        job.userProjectRoleService.ldapService = Mock(LdapService)

        Closure<User> createUserWithProjectsHelper = { String username, int offset ->
            Date date = new Date(System.currentTimeMillis() + offset * 24 * 60 * 60 * 1000)
            User user = DomainFactory.createUser(username: username, plannedDeactivationDate: date)
            DomainFactory.createUserProjectRole(user: user)
            DomainFactory.createUserProjectRole(user: user, enabled: false)
            return user
        }

        User userA = createUserWithProjectsHelper("username.a", -1)
        List<User> untouchedUsers = [
            createUserWithProjectsHelper("username.b", +1),
            createUserWithProjectsHelper(null, -1),
            createUserWithProjectsHelper(null, +1),
        ]

        when:
        job.wrappedExecute()

        then:
        UserProjectRole.findAllByUser(userA).every { !it.enabled }
        untouchedUsers.every { User user ->
            UserProjectRole.findAllByUserAndEnabled(user, true).size() == 1
            UserProjectRole.findAllByUserAndEnabled(user, false).size() == 1
        }
    }
}
