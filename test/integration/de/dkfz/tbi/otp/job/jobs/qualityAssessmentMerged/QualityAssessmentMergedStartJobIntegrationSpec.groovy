package de.dkfz.tbi.otp.job.jobs.qualityAssessmentMerged

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.job.scheduler.*
import de.dkfz.tbi.otp.ngsdata.*
import grails.test.spock.*
import org.springframework.beans.factory.annotation.*

class QualityAssessmentMergedStartJobIntegrationSpec extends IntegrationSpec {
    @Autowired
    QualityAssessmentMergedStartJob qualityAssessmentMergedStartJob

    def "test method restart"() {
        given:
        QualityAssessmentMergedPass failedInstance = DomainFactory.createQualityAssessmentMergedPass()

        Process failedProcess = DomainFactory.createProcess()
        DomainFactory.createProcessParameter(failedProcess, failedInstance)

        qualityAssessmentMergedStartJob.schedulerService = Mock(SchedulerService){
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
        Process process = qualityAssessmentMergedStartJob.restart(failedProcess)
        QualityAssessmentMergedPass restartedInstance = (QualityAssessmentMergedPass)process.getProcessParameterObject()

        then:
        QualityAssessmentMergedPass.list().size() == 2
        restartedInstance.abstractMergedBamFile == failedInstance.abstractMergedBamFile
        restartedInstance.abstractMergedBamFile.qualityAssessmentStatus == AbstractBamFile.QaProcessingStatus.IN_PROGRESS
    }
}
