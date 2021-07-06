/*
 * Copyright 2011-2019 The OTP authors
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
package de.dkfz.tbi.otp.job.jobs.fastqc

import grails.testing.mixin.integration.Integration
import grails.transaction.Rollback
import spock.lang.Specification

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.job.plan.JobExecutionPlan
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.job.scheduler.SchedulerService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.tracking.*
import de.dkfz.tbi.otp.utils.SessionUtils

@Rollback
@Integration
class FastqcStartJobIntegrationSpec extends Specification {

    void setup() {
        SessionUtils.metaClass.static.withNewSession = { Closure c -> c() }
    }

    void cleanup() {
        TestCase.removeMetaClass(SessionUtils)
    }

    void "execute calls setStartedForSeqTracks"() {
        given:
        SeqTrack seqTrack = DomainFactory.createSeqTrack()
        OtrsTicket otrsTicket = DomainFactory.createOtrsTicket()
        JobExecutionPlan plan = DomainFactory.createJobExecutionPlan(enabled: true)
        DomainFactory.createDataFile(fastqImportInstance: DomainFactory.createFastqImportInstance(otrsTicket: otrsTicket), seqTrack: seqTrack)

        FastqcStartJob fastqcStartJob = new FastqcStartJob()
        fastqcStartJob.schedulerService = Stub(SchedulerService) {
            createProcess(_, _, _) >> null
            isActive() >> true
        }
        fastqcStartJob.optionService = new ProcessingOptionService()
        fastqcStartJob.seqTrackService = Stub(SeqTrackService) {
            getSeqTrackReadyForFastqcProcessing(_) >> seqTrack
        }
        fastqcStartJob.notificationCreator = new NotificationCreator(otrsTicketService: new OtrsTicketService())
        fastqcStartJob.jobExecutionPlan = plan

        when:
        fastqcStartJob.execute()

        then:
        assert otrsTicket.fastqcStarted != null
   }

    void "test method restart"() {
        given:
        SeqTrack failedInstance = DomainFactory.createSeqTrack()
        Process failedProcess = DomainFactory.createProcess()
        DomainFactory.createProcessParameter(failedProcess, failedInstance)

        FastqcStartJob fastqcStartJob = new FastqcStartJob()
        fastqcStartJob.schedulerService = Mock(SchedulerService) {
            1 * createProcess(_, _, _) >> { StartJob startJob, List<Parameter> input, ProcessParameter processParameterSecond ->
                Process processSecond = DomainFactory.createProcess(
                    jobExecutionPlan: failedProcess.jobExecutionPlan,
                )
                processParameterSecond.process = processSecond
                assert processParameterSecond.save(flush: true)
                return processSecond
            }
        }

        when:
        Process process = fastqcStartJob.restart(failedProcess)
        SeqTrack restartedInstance = (SeqTrack)(process.processParameterObject)

        then:
        SeqTrack.list().size() == 1
        restartedInstance == failedInstance
        restartedInstance.fastqcState == SeqTrack.DataProcessingState.IN_PROGRESS
    }
}
