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
import grails.web.mapping.LinkGenerator
import spock.lang.Specification

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.administration.LdapService
import de.dkfz.tbi.otp.administration.UserService
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.security.User
import de.dkfz.tbi.otp.security.UserAndRoles
import de.dkfz.tbi.otp.utils.MailHelperService

import java.time.*

@Rollback
@Integration
class ScheduleUsersForDeactivationJobIntegrationSpec extends Specification implements DomainFactoryCore, UserAndRoles {

    void "getUsersToCheckForDeactivation, returns a unique list of all internal users with an active UserProjectRole"() {
        given:
        ScheduleUsersForDeactivationJob job = new ScheduleUsersForDeactivationJob()

        User userA = DomainFactory.createUser()
        User userB = DomainFactory.createUser()
        User userC = DomainFactory.createUser()
        [
                [userA, true],
                [userA, true],
                [userA, false],
                [userB, true],
                [userC, false],
        ].each { List<?> properties ->
            DomainFactory.createUserProjectRole(
                    user: properties[0],
                    enabled: properties[1],
            )
        }

        List<User> result

        when:
        result = job.usersToCheckForDeactivation

        then:
        [userA, userB].sort() == result.sort()
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
        DomainFactory.createProcessingOptionLazy(name: ProcessingOption.OptionName.LDAP_ACCOUNT_DEACTIVATION_GRACE_PERIOD, value: "${offset}")

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
        )
        User user = DomainFactory.createUser(plannedDeactivationDate: date)

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

        User userA = DomainFactory.createUser(username: "A", realName: "RealNameA")
        Set<UserProjectRole> userProjectRolesA = [
                DomainFactory.createUserProjectRole(user: userA),
                DomainFactory.createUserProjectRole(user: userA),
        ] as Set<UserProjectRole>

        User userB = DomainFactory.createUser(username: "B", realName: "RealNameB")
        Set<UserProjectRole> userProjectRolesB = [
                DomainFactory.createUserProjectRole(user: userB),
        ] as Set<UserProjectRole>

        String result
        String expected = """\
        |  - A (RealNameA) in project(s): ${userProjectRolesA*.project*.name.join(", ")}
        |  - B (RealNameB) in project(s): ${userProjectRolesB*.project*.name.join(", ")}""".stripMargin()

        when:
        result = job.getMailBodyWithInvalidUsers(userProjectRolesA + userProjectRolesB)

        then:
        result == expected
    }

    void "sendDeactivationMails, sends one for each authority user and a single one for the service"() {
        given:
        User userA = DomainFactory.createUser()
        User userB = DomainFactory.createUser()
        UserProjectRole userProjectRole = DomainFactory.createUserProjectRole()

        ScheduleUsersForDeactivationJob job = new ScheduleUsersForDeactivationJob([
                processingOptionService: Mock(ProcessingOptionService) {
                    _ * findOptionAsString(_) { return "option" }
                    _ * findOptionAsLong(_) { return 0L }
                },
                configService: Mock(TestConfigService) {
                    _ * getClock() >> { return Clock.fixed(Instant.ofEpochSecond(0), ZoneId.systemDefault()) }
                    _ * getTimeZoneId() >> { ZoneId.systemDefault() }
                },
                mailHelperService: Mock(MailHelperService) {
                    2 * sendEmail(_, _, _, _) >> { }
                    1 * sendEmail(_, _, _) >> { }
                },
                linkGenerator: Mock(LinkGenerator) {
                    _ * link(_) >> { return "generated_link" }
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

    void "buildActionPlan, correctly groups users"() {
        given:
        createAllBasicProjectRoles()
        List<User> expiredUsers = []
        ScheduleUsersForDeactivationJob job = new ScheduleUsersForDeactivationJob([
                processingOptionService: new ProcessingOptionService(),
                ldapService: Mock(LdapService) {
                    _ * isUserDeactivated(_) >> { User user ->
                        return user in expiredUsers
                    }
                    _ * existsInLdap(_) >> { return true }
                },
        ])

        Closure<List<UserProjectRole>> createProjectWithUsersHelper = { List<Map> propertyList ->
            Project project = createProject()
            return propertyList.collect { Map properties ->
                DomainFactory.createUserProjectRole([project: project] + properties)
            }
        }

        User pi1 = DomainFactory.createUser()
        User pi2 = DomainFactory.createUser()

        User expiredUserA = DomainFactory.createUser()
        User expiredUserB = DomainFactory.createUser()
        User expiredUserC = DomainFactory.createUser()
        User expiredAndScheduledUser = DomainFactory.createUser(plannedDeactivationDate: new Date())
        User notExpiredAndScheduledUser = DomainFactory.createUser(plannedDeactivationDate: new Date())

        expiredUsers.addAll([
                expiredUserA,
                expiredUserB,
                expiredUserC,
                expiredAndScheduledUser,
        ])

        // Single project authority
        List<UserProjectRole> uprsProjectA = createProjectWithUsersHelper([
                [user: pi1, projectRoles: [PI]],
                [user: expiredUserA],
                [user: expiredUserB],
                [user: expiredAndScheduledUser],
        ])

        // Multiple project authorities
        List<UserProjectRole> uprsProjectB = createProjectWithUsersHelper([
                [user: pi1, projectRoles: [PI]],
                [user: pi2, projectRoles: [PI]],
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
        TestCase.assertContainSame(result.usersToSetDate.sort(), [expiredUserA, expiredUserB].sort())
        TestCase.assertContainSame(result.usersToResetDate.sort(), [notExpiredAndScheduledUser].sort())
        TestCase.assertContainSame(result.notificationMap, [
                (pi1) : (uprsProjectA[1, 2] + uprsProjectB[2]) as Set,
                (pi2) : ([uprsProjectB[2]]) as Set,
                (null): ([uprsProjectC[0]]) as Set,
        ])
    }

    void "executeActionPlan, executes changes as expected"() {
        given:
        createAllBasicProjectRoles()
        DomainFactory.createProcessingOptionLazy(name: ProcessingOption.OptionName.LDAP_ACCOUNT_DEACTIVATION_GRACE_PERIOD, value: "10")

        ScheduleUsersForDeactivationJob job = new ScheduleUsersForDeactivationJob([
                processingOptionService: new ProcessingOptionService(),
                configService: Mock(TestConfigService) {
                    _ * getClock() >> { return Clock.fixed(Instant.ofEpochSecond(0), ZoneId.systemDefault()) }
                    _ * getTimeZoneId() >> { ZoneId.systemDefault() }
                },
                mailHelperService: Mock(MailHelperService) {
                    1 * sendEmail(_, _, _, _) >> { }
                    1 * sendEmail(_, _, _) >> { }
                },
                userService: new UserService(),
                linkGenerator: Mock(LinkGenerator) {
                    _ * link(_) >> { return "generated_link" }
                },
        ])

        User user = DomainFactory.createUser()

        ActionPlan actionPlan = new ActionPlan()
        actionPlan.usersToSetDate = (0..1).collect { DomainFactory.createUser() }
        actionPlan.usersToResetDate = (0..1).collect { DomainFactory.createUser(plannedDeactivationDate: new Date()) }
        actionPlan.notificationMap = [
                (user): [DomainFactory.createUserProjectRole()] as Set,
                (null): [DomainFactory.createUserProjectRole()] as Set,
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
        UserProjectRole uprA = DomainFactory.createUserProjectRole(project: project, projectRoles: [PI])
        UserProjectRole uprB = DomainFactory.createUserProjectRole(project: project, projectRoles: [PI])
        DomainFactory.createUserProjectRole(project: project, projectRoles: [BIOINFORMATICIAN])
        DomainFactory.createUserProjectRole(project: project, enabled: false)
        DomainFactory.createUserProjectRole(project: project, user: DomainFactory.createUser(username: null))

        expect:
        [uprB]*.user.sort() == plan.getValidAuthorityUsers(uprA).sort()
    }

    void "getValidAuthorityUsers, when the person to be deactivated is the last authority left"() {
        given:
        createAllBasicProjectRoles()
        ActionPlan plan = new ActionPlan()

        Project project = createProject()
        UserProjectRole upr = DomainFactory.createUserProjectRole(project: project, projectRoles: [PI])
        DomainFactory.createUserProjectRole(project: project, projectRoles: [BIOINFORMATICIAN])
        DomainFactory.createUserProjectRole(project: project, enabled: false)
        DomainFactory.createUserProjectRole(project: project, user: DomainFactory.createUser(username: null))

        expect:
        [] == plan.getValidAuthorityUsers(upr)
    }
}
