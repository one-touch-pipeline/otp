package de.dkfz.tbi.otp.job.jobs.merging

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.job.scheduler.*
import de.dkfz.tbi.otp.ngsdata.*
import grails.test.spock.*
import org.springframework.beans.factory.annotation.*

class MergingStartJobIntegrationSpec extends IntegrationSpec{
    @Autowired
    MergingStartJob mergingStartJob

    def "test method restart"() {
        given:
        MergingPass failedInstance = DomainFactory.createMergingPass()

        Process failedProcess = DomainFactory.createProcess()
        DomainFactory.createProcessParameter(failedProcess, failedInstance)

        mergingStartJob.schedulerService = Mock(SchedulerService){
            1 * createProcess(_, _, _) >> { StartJob startJob, List<Parameter> input, ProcessParameter processParameterSecond ->
                Process processSecond = DomainFactory.createProcess(
                        jobExecutionPlan: failedProcess.jobExecutionPlan
                )
                processParameterSecond.process = processSecond
                assert processParameterSecond.save(flush: true)
                return processSecond
            }
        }

        when:
        Process process = mergingStartJob.restart(failedProcess)
        MergingPass restartedInstance = (MergingPass)process.getProcessParameterObject()

        then:
        MergingPass.list().size() == 2
        restartedInstance.mergingSet == failedInstance.mergingSet
        restartedInstance.mergingSet.status == MergingSet.State.INPROGRESS
    }
}
