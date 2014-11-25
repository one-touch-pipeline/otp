package de.dkfz.tbi.otp.job.jobs.snvcalling

import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement

import org.codehaus.groovy.grails.plugins.springsecurity.SpringSecurityUtils
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired

import de.dkfz.tbi.otp.job.plan.JobExecutionPlan
import de.dkfz.tbi.otp.job.processing.Process
import de.dkfz.tbi.otp.testing.GroovyScriptAwareIntegrationTest

class SamplePairDiscoveryStartJobTests extends GroovyScriptAwareIntegrationTest {

    @Autowired
    SamplePairDiscoveryStartJob samplePairDiscoveryStartJob

    @Test
    void testExecute() {
        createUserAndRoles()
        SpringSecurityUtils.doWithAuth('admin') {
            run('scripts/workflows/SamplePairDiscoveryWorkflow.groovy')
        }
        samplePairDiscoveryStartJob.jobExecutionPlan = findJobExecutionPlan()
        assert findProcesses().size() == 0

        samplePairDiscoveryStartJob.execute()
        assert findProcesses().size() == 1

        samplePairDiscoveryStartJob.execute()
        final Collection<Process> processes = findProcesses()
        assert processes.size() == 1

        final Process process = processes.iterator().next()
        process.finished = true
        assert process.save()
        samplePairDiscoveryStartJob.execute()
        assert findProcesses().size() == 2

    }

    private Collection<Process> findProcesses() {
        return Process.findAllByJobExecutionPlan(findJobExecutionPlan())
    }

    private JobExecutionPlan findJobExecutionPlan() {
        return exactlyOneElement(JobExecutionPlan.findAllByName('SamplePairDiscoveryWorkflow'))
    }
}
