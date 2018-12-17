package de.dkfz.tbi.otp.job.jobs

import grails.test.spock.IntegrationSpec

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair
import de.dkfz.tbi.otp.job.jobs.AbstractBamFilePairAnalysis.StartJobIntegrationSpec
import de.dkfz.tbi.otp.job.jobs.bamFilePairAnalysis.AbstractBamFilePairAnalysisStartJob
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.job.scheduler.SchedulerService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.tracking.OtrsTicket
import de.dkfz.tbi.otp.utils.logging.LogThreadLocal

abstract class AbstractBamFilePairAnalysisStartJobIntegrationSpec extends IntegrationSpec implements StartJobIntegrationSpec {

    void "getConfig when config is null, throw an exception"() {
        given:
        createPipeline()
        SamplePair samplePair = DomainFactory.createSamplePair()

        when:
        getService().getConfig(samplePair)

        then:
        RuntimeException e = thrown()
        e.message ==~ /No .*Config.* found for .*/
    }

    void "getConfig when call fine"() {
        given:
        SamplePair samplePair = DomainFactory.createSamplePair()
        Pipeline pipeline = createPipeline()

        when:
        ConfigPerProjectAndSeqType configPerProject = createConfig(samplePair, pipeline)

        then:
        configPerProject == getService().getConfig(samplePair)
    }

    void "findSamplePairToProcess, all fine"() {
        given:
        SamplePair samplePair = setupSamplePair()
        DomainFactory.createExomeSeqType()

        expect:
        samplePair == getService().findSamplePairToProcess(ProcessingPriority.NORMAL)
    }

    void "findSamplePairToProcess, wrong seqType should return null"() {
        given:
        SeqType notWgsSeqType = DomainFactory.createExomeSeqType()
        SamplePair samplePair = setupSamplePair()
        samplePair.mergingWorkPackage1.seqType = notWgsSeqType
        assert samplePair.mergingWorkPackage1.save(flush: true)
        samplePair.mergingWorkPackage2.seqType = notWgsSeqType
        assert samplePair.mergingWorkPackage2.save(flush: true)

        expect:
        null == getService().findSamplePairToProcess(ProcessingPriority.NORMAL)
    }

    void "prepareCreatingTheProcessAndTriggerTracking, when input is null shall throw an exception"() {
        when:
        getService().prepareCreatingTheProcessAndTriggerTracking(null)

        then:
        AssertionError e = thrown()
        e.message.contains("bamFilePairAnalysis must not be null")
    }

    void "prepareCreatingTheProcessAndTriggerTracking, when all fine"() {
        given:
        BamFilePairAnalysis instance = getInstance()
        OtrsTicket otrsTicket = DomainFactory.createOtrsTicket()
        RunSegment.list().each {
            it.otrsTicket = otrsTicket
            it.save(flush: true)
        }

        expect:
        getStartedDate(otrsTicket) == null
        getProcessingStatus(instance.samplePair) != SamplePair.ProcessingStatus.NO_PROCESSING_NEEDED

        when:
        getService().prepareCreatingTheProcessAndTriggerTracking(instance)

        then:
        getStartedDate(otrsTicket) != null
        getProcessingStatus(instance.samplePair) == SamplePair.ProcessingStatus.NO_PROCESSING_NEEDED
    }


    void "test method restart, fail when process is null"() {
        when:
        getService().restart(null)

        then:
        AssertionError error = thrown()
        error.message.contains("assert process")
    }

    void "test method restart with AbstractBamFilePairAnalysisStartJob"() {
        given:
        AbstractBamFilePairAnalysisStartJob service = getService()
        RemoteShellHelper remoteShellHelper = service.remoteShellHelper
        SchedulerService schedulerService = service.schedulerService

        DomainFactory.createProcessingOptionLazy(name: ProcessingOption.OptionName.TIME_ZONE, type: null, value: "Europe/Berlin")
        BamFilePairAnalysis failedInstance = getInstance()

        Process failedProcess = DomainFactory.createProcess()
        DomainFactory.createProcessParameter(failedProcess, failedInstance)

        service.remoteShellHelper = Mock(RemoteShellHelper) {
            1 * executeCommandReturnProcessOutput(_, _) >> { Realm realm, String cmd ->
                assert cmd == "rm -rf ${failedInstance.instancePath.absoluteDataManagementPath}"
            }
        }

        service.schedulerService = Mock(SchedulerService) {
            1 * createProcess(_, _, _) >> { StartJob startJob, List<Parameter> input, ProcessParameter processParameter2 ->
                Process process2 = DomainFactory.createProcess(
                        jobExecutionPlan: failedProcess.jobExecutionPlan
                )
                processParameter2.process = process2
                assert processParameter2.save(flush: true)
                return process2
            }
        }

        when:
        Process process
        LogThreadLocal.withThreadLog(System.out) {
            process = service.restart(failedProcess)
        }
        BamFilePairAnalysis restartedInstance = BamFilePairAnalysis.get(ProcessParameter.findByProcess(process).value)

        then:
        BamFilePairAnalysis.list().size() == 2
        restartedInstance.config == failedInstance.config

        failedInstance.withdrawn

        cleanup:
        service.remoteShellHelper = remoteShellHelper
        service.schedulerService = schedulerService
    }
}
