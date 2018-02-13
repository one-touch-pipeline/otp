package de.dkfz.tbi.otp.utils

import grails.plugin.springsecurity.SpringSecurityService
import org.junit.Test

import static org.junit.Assert.*
import static de.dkfz.tbi.otp.utils.JobExecutionPlanDSL.*

import grails.plugin.springsecurity.SpringSecurityUtils
import org.springframework.security.acls.domain.BasePermission

import de.dkfz.tbi.otp.administration.GroupCommand
import de.dkfz.tbi.otp.job.plan.JobExecutionPlan
import de.dkfz.tbi.otp.security.Group
import de.dkfz.tbi.otp.security.User
import de.dkfz.tbi.otp.security.UserRole
import de.dkfz.tbi.otp.testing.AbstractIntegrationTest


class JobExecutionPlanDSLTests extends AbstractIntegrationTest {
    SpringSecurityService springSecurityService
    def grailsApplication
    def planValidatorService
    def groupService

    @Test
    void testEmptyPlan() {
        assertEquals(0, JobExecutionPlan.count())
        plan("test") {
        }
        JobExecutionPlan jep = JobExecutionPlan.list().last()
        assertFalse(planValidatorService.validate(jep).isEmpty())
        plan("test2") {
            start("startJob", "testStartJob")
        }
        jep = JobExecutionPlan.list().last()
        assertFalse(planValidatorService.validate(jep).isEmpty())
    }

    @Test
    void testWatchDog() {
        assertEquals(0, JobExecutionPlan.count())
        plan("test") {
            start("startJob", "testStartJob")
            job("test", "testJob") {
                watchdog("testEndStateAwareJob")
            }
        }
        JobExecutionPlan jep = JobExecutionPlan.list().last()
        def errors = planValidatorService.validate(jep)
        assertTrue(errors.isEmpty())
    }

    @Test
    void testGrantReadToGroup() {
        createUserAndRoles()
        Group group = null
        SpringSecurityUtils.doWithAuth("admin") {
            GroupCommand cmd = new GroupCommand(name: "test", description: "test", readJobSystem: true)
            group = groupService.createGroup(cmd)
            group.save(flush: true)
            plan("test") {
                start("startJob", "testStartJob")
                job("test", "testJob") {
                    watchdog("testEndStateAwareJob")
                }
            }
        }
        JobExecutionPlan jep = JobExecutionPlan.list().last()
        // create a user and add him to the group
        UserRole.create(User.findByUsername("testuser"), group.role, true)

        SpringSecurityUtils.doWithAuth("testuser") {
            assertTrue(aclUtilService.hasPermission(springSecurityService.authentication, jep, BasePermission.READ))
            assertFalse(aclUtilService.hasPermission(springSecurityService.authentication, jep, BasePermission.WRITE))
        }
        // other user should not see
        SpringSecurityUtils.doWithAuth("user") {
            assertFalse(aclUtilService.hasPermission(springSecurityService.authentication, jep, BasePermission.READ))
            assertFalse(aclUtilService.hasPermission(springSecurityService.authentication, jep, BasePermission.WRITE))
        }
    }

    @Test
    void testGrantWriteToGroup() {
        createUserAndRoles()
        Group group = null
        SpringSecurityUtils.doWithAuth("admin") {
            GroupCommand cmd = new GroupCommand(name: "test", description: "test", readJobSystem: true, writeJobSystem: true)
            group = groupService.createGroup(cmd)
            group.save(flush: true)
            plan("test") {
                start("startJob", "testStartJob")
                job("test", "testJob") {
                    watchdog("testEndStateAwareJob")
                }
            }
        }
        JobExecutionPlan jep = JobExecutionPlan.list().last()
        // create a user and add him to the group
        UserRole.create(User.findByUsername("testuser"), group.role, true)

        SpringSecurityUtils.doWithAuth("testuser") {
            assertTrue(aclUtilService.hasPermission(springSecurityService.authentication, jep, BasePermission.READ))
            assertTrue(aclUtilService.hasPermission(springSecurityService.authentication, jep, BasePermission.WRITE))
        }
        // other user should not see
        SpringSecurityUtils.doWithAuth("user") {
            assertFalse(aclUtilService.hasPermission(springSecurityService.authentication, jep, BasePermission.READ))
            assertFalse(aclUtilService.hasPermission(springSecurityService.authentication, jep, BasePermission.WRITE))
        }
    }
}
