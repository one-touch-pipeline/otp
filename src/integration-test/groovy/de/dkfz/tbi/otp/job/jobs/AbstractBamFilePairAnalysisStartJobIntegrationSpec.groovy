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
package de.dkfz.tbi.otp.job.jobs

import grails.testing.mixin.integration.Integration
import grails.transaction.Rollback
import spock.lang.Specification

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair
import de.dkfz.tbi.otp.job.jobs.AbstractBamFilePairAnalysis.StartJobIntegrationSpec
import de.dkfz.tbi.otp.job.jobs.bamFilePairAnalysis.AbstractBamFilePairAnalysisStartJob
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.job.scheduler.SchedulerService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.tracking.OtrsTicket
import de.dkfz.tbi.otp.utils.logging.LogThreadLocal
import de.dkfz.tbi.otp.workflowExecution.ProcessingPriority

import java.nio.file.Paths

@Rollback
@Integration
abstract class AbstractBamFilePairAnalysisStartJobIntegrationSpec extends Specification implements StartJobIntegrationSpec {

    void "getConfig when config is null, throw an exception"() {
        given:
        createPipeline()
        SamplePair samplePair = DomainFactory.createSamplePair()

        when:
        service.getConfig(samplePair)

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
        configPerProject == service.getConfig(samplePair)
    }

    void "findSamplePairToProcess, all fine"() {
        given:
        SamplePair samplePair = setupSamplePair()
        DomainFactory.createExomeSeqType()

        expect:
        samplePair == service.findSamplePairToProcess(ProcessingPriority.NORMAL)
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
        service.findSamplePairToProcess(ProcessingPriority.NORMAL) == null
    }

    void "prepareCreatingTheProcessAndTriggerTracking, when input is null shall throw an exception"() {
        when:
        service.prepareCreatingTheProcessAndTriggerTracking(null)

        then:
        AssertionError e = thrown()
        e.message.contains("bamFilePairAnalysis must not be null")
    }

    void "prepareCreatingTheProcessAndTriggerTracking, when all fine"() {
        given:
        BamFilePairAnalysis instance = this.instance
        OtrsTicket otrsTicket = DomainFactory.createOtrsTicket()
        FastqImportInstance.list().each {
            it.otrsTicket = otrsTicket
            it.save(flush: true)
        }

        expect:
        getStartedDate(otrsTicket) == null
        getProcessingStatus(instance.samplePair) != SamplePair.ProcessingStatus.NO_PROCESSING_NEEDED

        when:
        service.prepareCreatingTheProcessAndTriggerTracking(instance)

        then:
        getStartedDate(otrsTicket) != null
        getProcessingStatus(instance.samplePair) == SamplePair.ProcessingStatus.NO_PROCESSING_NEEDED
    }

    void "test method restart, fail when process is null"() {
        when:
        service.restart(null)

        then:
        AssertionError error = thrown()
        error.message.contains("assert process")
    }

    void "test method restart with AbstractBamFilePairAnalysisStartJob"() {
        given:
        AbstractBamFilePairAnalysisStartJob service = this.service
        RemoteShellHelper remoteShellHelper = service.remoteShellHelper
        SchedulerService schedulerService = service.schedulerService

        DomainFactory.createProcessingOptionLazy(name: ProcessingOption.OptionName.TIME_ZONE, type: null, value: "Europe/Berlin")
        BamFilePairAnalysis failedInstance = this.instance

        Process failedProcess = DomainFactory.createProcess()
        DomainFactory.createProcessParameter(failedProcess, failedInstance)

        service.bamFileAnalysisServiceFactoryService = Mock(BamFileAnalysisServiceFactoryService) {
            getService(failedInstance) >> {
                Mock(AbstractBamFileAnalysisService) {
                    getWorkDirectory(failedInstance) >> Paths.get("/asdf")
                }
            }
        }

        service.remoteShellHelper = Mock(RemoteShellHelper) {
            1 * executeCommandReturnProcessOutput(_, _) >> { Realm realm, String cmd ->
                assert cmd == "rm -rf /asdf"
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
