package de.dkfz.tbi.otp.job.jobs.transferMergedBamFile

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.job.scheduler.*
import de.dkfz.tbi.otp.ngsdata.*
import grails.test.spock.*

class TransferMergedBamFileStartJobIntegrationSpec extends IntegrationSpec {

    def "test method restart"() {
        given:
        ProcessedMergedBamFile failedInstance = DomainFactory.createProcessedMergedBamFile()

        Process failedProcess = DomainFactory.createProcess()
        DomainFactory.createProcessParameter(failedProcess, failedInstance)

        TransferMergedBamFileStartJob transferMergedBamFileStartJob = new TransferMergedBamFileStartJob()
        transferMergedBamFileStartJob.schedulerService = Mock(SchedulerService){
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
        Process process = transferMergedBamFileStartJob.restart(failedProcess)
        ProcessedMergedBamFile restartedInstance = (ProcessedMergedBamFile)process.getProcessParameterObject()

        then:
        ProcessedMergedBamFile.list().size() == 1
        restartedInstance.fileOperationStatus == AbstractMergedBamFile.FileOperationStatus.INPROGRESS
        restartedInstance == failedInstance
    }
}
