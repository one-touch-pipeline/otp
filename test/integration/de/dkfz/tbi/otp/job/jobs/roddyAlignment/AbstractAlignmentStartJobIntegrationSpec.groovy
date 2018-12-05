package de.dkfz.tbi.otp.job.jobs.roddyAlignment

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.job.jobs.alignment.AbstractAlignmentStartJob
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.job.scheduler.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.logging.*
import grails.test.spock.*

class AbstractAlignmentStartJobIntegrationSpec extends IntegrationSpec {

    def "restart creates new Process on new RoddyBamFile"() {
        given:
        RoddyBamFile failedInstance = DomainFactory.createRoddyBamFile(
                fileOperationStatus: AbstractMergedBamFile.FileOperationStatus.INPROGRESS,
                md5sum: null,
        )
        Process failedProcess = DomainFactory.createProcess()
        DomainFactory.createProcessParameter(failedProcess, failedInstance)
        failedInstance.mergingWorkPackage.bamFileInProjectFolder = failedInstance
        failedInstance.mergingWorkPackage.save(flush: true)


        AbstractAlignmentStartJob roddyAlignmentStartJob = new PanCanStartJob()
        roddyAlignmentStartJob.schedulerService = Mock(SchedulerService){
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
        Process process
        LogThreadLocal.withThreadLog(System.out) {
            process = roddyAlignmentStartJob.restart(failedProcess)
        }
        RoddyBamFile restartedInstance = (RoddyBamFile)process.getProcessParameterObject()

        then:
        RoddyBamFile.list().size() == 2
        assert !failedInstance.mergingWorkPackage.needsProcessing
        restartedInstance.baseBamFile == failedInstance.baseBamFile
        restartedInstance.seqTracks == failedInstance.seqTracks
        restartedInstance.identifier > failedInstance.identifier
    }
}
