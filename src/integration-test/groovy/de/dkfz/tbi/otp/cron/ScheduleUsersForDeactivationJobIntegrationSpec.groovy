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
package de.dkfz.tbi.otp.cron

import grails.testing.mixin.integration.Integration
import grails.gorm.transactions.Rollback
import grails.web.mapping.LinkGenerator
import spock.lang.Specification

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.security.user.UserService
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.domainFactory.UserDomainFactory
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.security.User
import de.dkfz.tbi.otp.security.UserAndRoles
import de.dkfz.tbi.otp.security.user.identityProvider.IdentityProvider
import de.dkfz.tbi.otp.utils.MailHelperService
import de.dkfz.tbi.otp.utils.MessageSourceService

import java.time.*

@Rollback
@Integration
class ScheduleUsersForDeactivationJobIntegrationSpec extends Specification implements DomainFactoryCore, UserAndRoles, UserDomainFactory {

    void "getUsersToCheckForDeactivation, returns a unique list of all internal users with an active UserProjectRole"() {
        given:
        ScheduleUsersForDeactivationJob job = new ScheduleUsersForDeactivationJob()

        User userA = createUser()
        User userB = createUser()
        User userC = createUser()
        [
                [userA, true],
                [userA, true],
                [userA, false],
                [userB, true],
                [userC, false],
        ].each { List<?> properties ->
            createUserProjectRole(
                    user: properties[0],
                    enabled: properties[1],
            )
        }

        List<User> result

        when:
        result = job.usersToCheckForDeactivation

        then:
        TestCase.assertContainSame([userA, userB], result)
    }

    void "getPlannedDeactivationDate, is now + the offset defined in LDAP_ACCOUNT_DEACTIVATION_GRACE_PERIOD"() {
        given:
        int offset = 10
        ScheduleUsersForDeactivationJob job = new ScheduleUsersForDeactivationJob([
                processingOptionService: new ProcessingOptionService(),
                configService: Mock(TestConfigService) {
                    _ * getClock() >> { return Clock.fixed(Instant.ofEpochSecond(0), ZoneId.systemDefault()) }
                    _ * getTimeZoneId() >> { ZoneId.systemDefault() }
                },
        ])
        findOrCreateProcessingOption(name: ProcessingOption.OptionName.LDAP_ACCOUNT_DEACTIVATION_GRACE_PERIOD, value: "${offset}")

        Date result

        when:
        result = job.plannedDeactivationDate

        then:
        result.time == offset * 24 * 60 * 60 * 1000
    }

    void "resetPlannedDeactivationDateOfUser, sets plannedDeactivationDate to null"() {
        given:
        ScheduleUsersForDeactivationJob job = new ScheduleUsersForDeactivationJob(
                userService: new UserService(),
                userProjectRoleService: new UserProjectRoleService(),
        )
        User user = createUser(plannedDeactivationDate: date)

        when:
        job.resetPlannedDeactivationDateOfUser(user)

        then:
        user.plannedDeactivationDate == null

        where:
        date << [new Date(), null]
    }

    void "getMailBodyWithInvalidUsers, lists all users sorted"() {
        given:
        ScheduleUsersForDeactivationJob job = new ScheduleUsersForDeactivationJob()

        User userA = createUser(username: "a", realName: "RealNameA")
        Set<UserProjectRole> userProjectRolesA = [
                createUserProjectRole(user: userA),
                createUserProjectRole(user: userA),
        ] as Set<UserProjectRole>

        User userB = createUser(username: "b", realName: "RealNameB")
        Set<UserProjectRole> userProjectRolesB = [
                createUserProjectRole(user: userB),
        ] as Set<UserProjectRole>

        String result
        String expected = """\
        |  - a (RealNameA) in project(s): ${userProjectRolesA*.project*.name.join(", ")}
        |  - b (RealNameB) in project(s): ${userProjectRolesB*.project*.name.join(", ")}""".stripMargin()

        when:
        result = job.getMailBodyWithInvalidUsers(userProjectRolesA + userProjectRolesB)

        then:
        result == expected
    }

    void "sendDeactivationMails, sends one for each authority user and a single one for the service"() {
        given:
        User userA = createUser()
        User userB = createUser()
        UserProjectRole userProjectRole = createUserProjectRole()

        ScheduleUsersForDeactivationJob job = new ScheduleUsersForDeactivationJob([
                processingOptionService: Mock(ProcessingOptionService) {
                    _ * findOptionAsString(_) { return "option" }
                    _ * findOptionAsLong(_) { return 0L }
                },
                configService          : Mock(TestConfigService) {
                    _ * getClock() >> { return Clock.fixed(Instant.ofEpochSecond(0), ZoneId.systemDefault()) }
                    _ * getTimeZoneId() >> { ZoneId.systemDefault() }
                },
                mailHelperService: Mock(MailHelperService) {
                    2 * sendEmail(_, _, _) >> { }
                    1 * sendEmailToTicketSystem(_, _) >> { }
                },
        ])
        Map<User, Set<UserProjectRole>> map = [
                (userA): [userProjectRole] as Set<UserProjectRole>,
                (userB): [userProjectRole] as Set<UserProjectRole>,
                (null) : [userProjectRole] as Set<UserProjectRole>,
        ]

        expect:
        job.sendDeactivationMails(map)
    }

    void "sendReactivationMails, sends one mail for each authority in the users projects"() {
        given:
        ScheduleUsersForDeactivationJob job = new ScheduleUsersForDeactivationJob([
                processingOptionService: Mock(ProcessingOptionService) {
                    _ * findOptionAsString(_) { return "option" }
                },
                mailHelperService      : Mock(MailHelperService),
                userProjectRoleService : Mock(UserProjectRoleService),
                messageSourceService   : Mock(MessageSourceService) {
                    _ * createMessage(_) { return "message" }
                },
                linkGenerator: Mock(LinkGenerator),
        ])
        List<Project> projects = [createProject(), createProject(), createProject()]
        User user = createUser()
        User projectAuthorityA = createUser()
        User projectAuthorityB = createUser()

        when:
        job.sendReactivationMails(user)

        then:
        _ * job.linkGenerator.link(_) >> 'some link'
        1 * job.userProjectRoleService.projectsAssociatedToProjectAuthority(user) >> [
                (projectAuthorityA): [projects[0], projects[1], projects[2]],
                (projectAuthorityB): [projects[1]],
        ]
        1 * job.mailHelperService.sendEmail(_, _, projectAuthorityA.email, [user.email]) >> { }
        1 * job.mailHelperService.sendEmail(_, _, projectAuthorityB.email, [user.email]) >> { }
    }

    void "buildActionPlan, correctly groups users"() {
        given:
        createAllBasicProjectRoles()
        List<User> expiredUsers = []
        ScheduleUsersForDeactivationJob job = new ScheduleUsersForDeactivationJob([
                processingOptionService: new ProcessingOptionService(),
                identityProvider: Mock(IdentityProvider) {
                    _ * isUserDeactivated(_) >> { User user ->
                        return user in expiredUsers
                    }
                    _ * exists(_) >> { return true }
                },
        ])

        Closure<List<UserProjectRole>> createProjectWithUsersHelper = { List<Map> propertyList ->
            Project project = createProject()
            return propertyList.collect { Map properties ->
                createUserProjectRole([project: project] + properties)
            }
        }

        User pi1 = createUser()
        User pi2 = createUser()

        User expiredUserA = createUser()
        User expiredUserB = createUser()
        User expiredUserC = createUser()
        User expiredAndScheduledUser = createUser(plannedDeactivationDate: new Date())
        User notExpiredAndScheduledUser = createUser(plannedDeactivationDate: new Date())

        expiredUsers.addAll([
                expiredUserA,
                expiredUserB,
                expiredUserC,
                expiredAndScheduledUser,
        ])

        // Single project authority
        List<UserProjectRole> uprsProjectA = createProjectWithUsersHelper([
                [user: pi1, projectRoles: [pi]],
                [user: expiredUserA],
                [user: expiredUserB],
                [user: expiredAndScheduledUser],
        ])

        // Multiple project authorities
        List<UserProjectRole> uprsProjectB = createProjectWithUsersHelper([
                [user: pi1, projectRoles: [pi]],
                [user: pi2, projectRoles: [pi]],
                [user: expiredUserA],
                [user: notExpiredAndScheduledUser],
                [user: expiredUserB, enabled: false],
                [user: expiredUserC, enabled: false],
        ])

        // Without project authority
        List<UserProjectRole> uprsProjectC = createProjectWithUsersHelper([
                [user: expiredUserA],
                [user: notExpiredAndScheduledUser],
        ])

        ActionPlan result

        when:
        result = job.buildActionPlan()

        then:
        TestCase.assertContainSame(result.usersToSetDate, [expiredUserA, expiredUserB])
        TestCase.assertContainSame(result.usersToResetDate, [notExpiredAndScheduledUser])
        TestCase.assertContainSame(result.notificationMap, [
                (pi1) : (uprsProjectA[1, 2] + uprsProjectB[2]) as Set,
                (pi2) : ([uprsProjectB[2]]) as Set,
                (null): ([uprsProjectC[0]]) as Set,
        ])
    }

    void "executeActionPlan, executes changes as expected"() {
        given:
        createAllBasicProjectRoles()
        findOrCreateProcessingOption(name: ProcessingOption.OptionName.LDAP_ACCOUNT_DEACTIVATION_GRACE_PERIOD, value: "10")

        ScheduleUsersForDeactivationJob job = new ScheduleUsersForDeactivationJob([
                processingOptionService: new ProcessingOptionService(),
                configService          : Mock(TestConfigService) {
                    _ * getClock() >> { return Clock.fixed(Instant.ofEpochSecond(0), ZoneId.systemDefault()) }
                    _ * getTimeZoneId() >> { ZoneId.systemDefault() }
                },
                mailHelperService      : Mock(MailHelperService) {
                    1 * sendEmail(_, _, _) >> { }
                    1 * sendEmailToTicketSystem(_, _) >> { }
                },
                userService            : new UserService(),
                userProjectRoleService : new UserProjectRoleService(),
                linkGenerator          : Mock(LinkGenerator) {
                    _ * link(_) >> { return "generated_link" }
                },
        ])

        User user = createUser()

        ActionPlan actionPlan = new ActionPlan()
        actionPlan.usersToSetDate = (0..1).collect { createUser() }
        actionPlan.usersToResetDate = (0..1).collect { createUser(plannedDeactivationDate: new Date()) }
        actionPlan.notificationMap = [
                (user): [createUserProjectRole()] as Set,
                (null): [createUserProjectRole()] as Set,
        ]

        when:
        job.executeActionPlan(actionPlan)

        then:
        actionPlan.usersToSetDate.every { it.plannedDeactivationDate }
        actionPlan.usersToResetDate.every { !it.plannedDeactivationDate }
    }

    void "getValidAuthorityUsers, check criteria"() {
        given:
        createAllBasicProjectRoles()
        ActionPlan plan = new ActionPlan()

        Project project = createProject()
        UserProjectRole uprA = createUserProjectRole(project: project, projectRoles: [pi])
        UserProjectRole uprB = createUserProjectRole(project: project, projectRoles: [pi])
        createUserProjectRole(project: project, projectRoles: [bioinformatician])
        createUserProjectRole(project: project, enabled: false)
        createUserProjectRole(project: project, user: createUser(username: null))

        expect:
        TestCase.assertContainSame([uprB]*.user, plan.getValidAuthorityUsers(uprA))
    }

    void "getValidAuthorityUsers, when the person to be deactivated is the last authority left"() {
        given:
        createAllBasicProjectRoles()
        ActionPlan plan = new ActionPlan()

        Project project = createProject()
        UserProjectRole upr = createUserProjectRole(project: project, projectRoles: [pi])
        createUserProjectRole(project: project, projectRoles: [bioinformatician])
        createUserProjectRole(project: project, enabled: false)
        createUserProjectRole(project: project, user: createUser(username: null))

        expect:
        [] == plan.getValidAuthorityUsers(upr)
    }
}
