/*
 * Copyright 2011-2024 The OTP authors
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
package de.dkfz.tbi.otp.workflowExecution

import spock.lang.Unroll

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePairDeciderService
import de.dkfz.tbi.otp.domainFactory.UserDomainFactory
import de.dkfz.tbi.otp.domainFactory.pipelines.externalBam.ExternalBamFactory
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.ngsdata.SeqType
import de.dkfz.tbi.otp.tracking.NotificationCreator
import de.dkfz.tbi.otp.utils.MessageSourceService
import de.dkfz.tbi.otp.utils.exceptions.OtpRuntimeException
import de.dkfz.tbi.otp.workflow.WorkflowCreateState
import de.dkfz.tbi.otp.workflow.bamImport.BamImportInitializationService
import de.dkfz.tbi.otp.workflow.bamImport.BamImportService
import de.dkfz.tbi.otp.workflowExecution.decider.AllDecider
import de.dkfz.tbi.otp.workflowExecution.decider.DeciderResult

class BamImportWorkflowCreatorSchedulerSpec extends AbstractWorkflowCreatorSchedulerSpec implements UserDomainFactory, ExternalBamFactory {

    @Override
    List<Class> getDomainClasses() {
        return [
                ExternalMergingWorkPackage,
                ExternallyProcessedBamFile,
                BamImportInstance,
                ProcessingOption,
        ]
    }

    BamImportInstance importInstance

    @Override
    Long getImportId() {
        return importInstance.id
    }

    void setup() {
        importInstance = createBamImportInstance([
                externallyProcessedBamFiles: [
                        DomainFactory.createExternallyProcessedBamFile(),
                        DomainFactory.createExternallyProcessedBamFile()
                ],
                workflowCreateState: WorkflowCreateState.WAITING,
        ])
    }

    void "scheduleCreateWorkflow, if system runs and waiting return null, then do not call createWorkflowRuns"() {
        given:
        BamImportWorkflowCreatorScheduler scheduler = createWorkflowCreatorScheduler()

        when:
        scheduler.scheduleCreateWorkflow()

        then:
        1 * scheduler.workflowSystemService.isEnabled() >> true

        1 * scheduler.bamImportService.waiting() >> null
        0 * scheduler.bamImportService.updateState(_, _)
        0 * scheduler.createWorkflowsAsync(_)
    }

    @Unroll
    void "scheduleCreateWorkflow, check if createWorkflowAsync is called or nor for the case that #cases"() {
        given:
        BamImportWorkflowCreatorScheduler scheduler = createWorkflowCreatorScheduler()
        BamImportInstance importInstanceWaiting = createBamImportInstance([workflowCreateState: WorkflowCreateState.WAITING])
        createBamImportInstance([workflowCreateState: WorkflowCreateState.PROCESSING])

        when:
        scheduler.scheduleCreateWorkflow()

        then:
        1 * scheduler.workflowSystemService.isEnabled() >> true
        1 * scheduler.bamImportService.waiting() >> (n > 0 ? importInstanceWaiting : null)
        n * scheduler.bamImportService.updateState(_, WorkflowCreateState.PROCESSING)

        where:
        cases                       | n
        "One waiting import exists" | 1
        "No waiting import exists"  | 0
    }

    @Unroll
    void "createWorkflowsTask, if all fine and #name, call all methods for success called"() {
        given:
        BamImportWorkflowCreatorScheduler scheduler = createWorkflowCreatorScheduler()
        findOrCreateProcessingOption(ProcessingOption.OptionName.OTP_SYSTEM_USER, createUser().username)
        List<SeqType> seqTypes = DomainFactory.createAllAnalysableSeqTypes()

        List<WorkflowRun> runs = [createWorkflowRun()]
        List<WorkflowArtefact> workflowArtefacts = runs.collect {
            createWorkflowArtefact([
                    producedBy  : it,
                    artefactType: ArtefactType.BAM,
            ])
        }
        DeciderResult deciderResult = new DeciderResult()
        deciderResult.newArtefacts.addAll(workflowArtefacts)

        List<AbstractBamFile> expectedListForSnv = []

        ExternallyProcessedBamFile bamFile = importInstance.externallyProcessedBamFiles[0]
        bamFile.workflowArtefact = workflowArtefacts.first()
        bamFile.workPackage = createMergingWorkPackage([
                seqType: analysableSeqType ? seqTypes.first() : createSeqType([
                        name: "OtherName",
                ])
        ])
        if (analysableSeqType) {
            expectedListForSnv << bamFile.workPackage
        }

        applicationContext // initialize the applicationContext

        when:
        scheduler.createWorkflowsTask(importId)

        then:
        1 * scheduler.bamImportService.updateState(importId, WorkflowCreateState.SUCCESS)
        0 * scheduler.bamImportService._

        1 * scheduler.bamImportService.countInstancesInWaitingState() >> 1
        0 * scheduler.messageSourceService.getMessage('workflow.bamImport.failedLoadingBamImportInstance', _)
        1 * scheduler.bamImportInitializationService.createWorkflowRuns(importInstance) >> runs
        0 * scheduler.bamImportInitializationService._
        1 * scheduler.allDecider.decide(_) >> deciderResult
        1 * scheduler.samplePairDeciderService.findOrCreateSamplePairs(expectedListForSnv)
        0 * scheduler.samplePairDeciderService._
        1 * scheduler.notificationCreator.sendBamImportWorkflowCreateSuccessMail(_, importId, _, _)

        where:
        name                             | analysableSeqType
        "bam and not analysable seqType" | false
        "bam and analysable seqType"     | true
    }

    void "createWorkflowsTask, if decider throws exception, then send error E-Mail to ticketing system"() {
        given:
        BamImportWorkflowCreatorScheduler scheduler = createWorkflowCreatorScheduler()
        List<WorkflowRun> runs = [createWorkflowRun()]
        OtpRuntimeException otpRuntimeException = new OtpRuntimeException("Decider throws exceptions")

        when:
        scheduler.createWorkflowsTask(importId)

        then:
        1 * scheduler.bamImportService.updateState(importId, WorkflowCreateState.FAILED)
        0 * scheduler.bamImportService._

        1 * scheduler.bamImportService.countInstancesInWaitingState() >> 1
        0 * scheduler.messageSourceService.getMessage('workflow.bamImport.failedLoadingBamImportInstance', _)
        1 * scheduler.bamImportInitializationService.createWorkflowRuns(importInstance) >> runs
        0 * scheduler.bamImportInitializationService._
        1 * scheduler.allDecider.decide(_) >> { throw otpRuntimeException }
        0 * scheduler.samplePairDeciderService.findOrCreateSamplePairs(_)
        1 * scheduler.notificationCreator.sendBamImportWorkflowCreateErrorMail(_, importId, _, otpRuntimeException)
    }

    @Override
    AbstractWorkflowCreatorScheduler createWorkflowCreatorScheduler() {
        return new BamImportWorkflowCreatorScheduler([
                allDecider                    : Mock(AllDecider),
                bamImportService              : Mock(BamImportService),
                messageSourceService          : Mock(MessageSourceService),
                bamImportInitializationService: Mock(BamImportInitializationService),
                notificationCreator           : Mock(NotificationCreator),
                workflowSystemService         : Mock(WorkflowSystemService),
                samplePairDeciderService      : Mock(SamplePairDeciderService),
        ])
    }
}
