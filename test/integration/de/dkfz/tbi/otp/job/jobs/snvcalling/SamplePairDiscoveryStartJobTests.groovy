package de.dkfz.tbi.otp.job.jobs.snvcalling

import de.dkfz.tbi.otp.job.scheduler.SchedulerService
import grails.plugin.springsecurity.SpringSecurityUtils
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired

import de.dkfz.tbi.otp.job.jobs.TestJobHelper
import de.dkfz.tbi.otp.job.processing.Process
import de.dkfz.tbi.otp.scriptTests.GroovyScriptAwareTestCase

class SamplePairDiscoveryStartJobTests extends GroovyScriptAwareTestCase {

    @Autowired
    SamplePairDiscoveryStartJob samplePairDiscoveryStartJob

    SchedulerService schedulerService

    private static String PLAN_NAME = 'SamplePairDiscoveryWorkflow'

    boolean originalSchedulerActive

    @Before
    void setUp() {
        originalSchedulerActive = schedulerService.schedulerActive
        schedulerService.schedulerActive = true
    }

    @After
    void tearDown() {
        schedulerService.schedulerActive = originalSchedulerActive
    }

    @Test
    void testExecute() {
        createUserAndRoles()
        SpringSecurityUtils.doWithAuth(getADMIN()) {
            runScript('scripts/workflows/SamplePairDiscoveryWorkflow.groovy')
        }
        samplePairDiscoveryStartJob.jobExecutionPlan = TestJobHelper.findJobExecutionPlan(PLAN_NAME)
        assert TestJobHelper.findProcessesForPlanName(PLAN_NAME).size() == 0

        samplePairDiscoveryStartJob.execute()
        assert TestJobHelper.findProcessesForPlanName(PLAN_NAME).size() == 1

        samplePairDiscoveryStartJob.execute()
        final Collection<Process> processes = TestJobHelper.findProcessesForPlanName(PLAN_NAME)
        assert processes.size() == 1

        final Process process = processes.iterator().next()
        process.finished = true
        assert process.save(flush: true)
        samplePairDiscoveryStartJob.execute()
        assert TestJobHelper.findProcessesForPlanName(PLAN_NAME).size() == 2
    }
}
