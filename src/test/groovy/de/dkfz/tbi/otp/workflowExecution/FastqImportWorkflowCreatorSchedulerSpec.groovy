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
import de.dkfz.tbi.otp.domainFactory.pipelines.RoddyPanCancerFactory
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.tracking.NotificationCreator
import de.dkfz.tbi.otp.utils.MessageSourceService
import de.dkfz.tbi.otp.utils.exceptions.OtpRuntimeException
import de.dkfz.tbi.otp.workflow.WorkflowCreateState
import de.dkfz.tbi.otp.workflow.datainstallation.DataInstallationInitializationService
import de.dkfz.tbi.otp.workflowExecution.decider.AllDecider
import de.dkfz.tbi.otp.workflowExecution.decider.DeciderResult

class FastqImportWorkflowCreatorSchedulerSpec extends AbstractWorkflowCreatorSchedulerSpec implements UserDomainFactory, RoddyPanCancerFactory {

    @Override
    List<Class> getDomainClasses() {
        return [
                FastqFile,
                MergingWorkPackage,
                MetaDataFile,
                ProcessingOption,
                RoddyBamFile,
                FastqImportInstance,
        ]
    }

    MetaDataFile metaDataFile
    FastqImportInstance fastqImportInstance

    @Override
    Long getImportId() {
        return fastqImportInstance.id
    }

    void setup() {
        metaDataFile = DomainFactory.createMetaDataFile()
        fastqImportInstance = metaDataFile.fastqImportInstance
    }

    void "scheduleCreateWorkflow, if system runs and waiting return null, then do not call createWorkflowRuns"() {
        given:
        FastqImportWorkflowCreatorScheduler scheduler = createWorkflowCreatorScheduler()

        when:
        scheduler.scheduleCreateWorkflow()

        then:
        1 * scheduler.workflowSystemService.isEnabled() >> true

        1 * scheduler.fastqImportInstanceService.waiting() >> null
        0 * scheduler.metaDataFileService.findByFastqImportInstance(_)
        0 * scheduler.fastqImportInstanceService.updateState(_, _)

        0 * scheduler.createWorkflowsAsync(_)
    }

    @Unroll
    void "scheduleCreateWorkflow, check if createWorkflowAsync is called or nor for the case that #cases"() {
        given:
        FastqImportWorkflowCreatorScheduler scheduler = createWorkflowCreatorScheduler()
        FastqImportInstance fastqImportInstanceWaiting = createFastqImportInstance([state: WorkflowCreateState.WAITING])
        createFastqImportInstance([state: WorkflowCreateState.PROCESSING])

        when:
        scheduler.scheduleCreateWorkflow()

        then:
        1 * scheduler.workflowSystemService.isEnabled() >> true
        1 * scheduler.fastqImportInstanceService.waiting() >> (n > 0 ? fastqImportInstanceWaiting : null)
        n * scheduler.fastqImportInstanceService.updateState(_, WorkflowCreateState.PROCESSING)

        where:
        cases                       | n
        "One waiting import exists" | 1
        "No waiting import exists"  | 0
    }

    @Unroll
    void "createWorkflowsTask, if all fine and #name, call all methods for success called"() {
        given:
        FastqImportWorkflowCreatorScheduler scheduler = createWorkflowCreatorScheduler()
        findOrCreateProcessingOption(ProcessingOption.OptionName.OTP_SYSTEM_USER, createUser().username)
        List<SeqType> seqTypes = DomainFactory.createAllAnalysableSeqTypes()

        List<WorkflowRun> runs = [createWorkflowRun()]
        List<WorkflowArtefact> workflowArtefacts = runs.collect {
            createWorkflowArtefact([
                    producedBy  : it,
                    artefactType: artefactType,
            ])
        }
        DeciderResult deciderResult = new DeciderResult()
        deciderResult.newArtefacts.addAll(workflowArtefacts)

        List<AbstractBamFile> expectedListForSnv = []
        if (artefactType == ArtefactType.BAM) {
            RoddyBamFile roddyBamFile = createBamFile([
                    workflowArtefact: workflowArtefacts.first(),
                    workPackage     : createMergingWorkPackage([
                            seqType: analysableSeqType ? seqTypes.first() : createSeqType([
                                    name    : "OtherName",
                                    dirName : "dummy"
                            ])
                    ]),
            ])
            if (analysableSeqType) {
                expectedListForSnv << roddyBamFile.workPackage
            }
        }

        applicationContext // initialize the applicationContext

        when:
        scheduler.createWorkflowsTask(importId)

        then:
        1 * scheduler.fastqImportInstanceService.updateState(fastqImportInstance, WorkflowCreateState.SUCCESS)
        0 * scheduler.fastqImportInstanceService._

        1 * scheduler.fastqImportInstanceService.countInstancesInWaitingState() >> 1
        0 * scheduler.messageSourceService.getMessage('workflow.bamImport.failedLoadingFastqImportInstance', _)
        1 * scheduler.dataInstallationInitializationService.createWorkflowRuns(fastqImportInstance) >> runs
        0 * scheduler.dataInstallationInitializationService._
        1 * scheduler.allDecider.decide(_) >> deciderResult
        1 * scheduler.samplePairDeciderService.findOrCreateSamplePairs(expectedListForSnv)
        0 * scheduler.samplePairDeciderService._
        1 * scheduler.notificationCreator.sendWorkflowCreateSuccessMail(_, _)

        where:
        name                                | artefactType       | analysableSeqType
        "no bam and not analysable seqType" | ArtefactType.FASTQ | false
        "no bam and analysable seqType"     | ArtefactType.FASTQ | true
        "bam and not analysable seqType"    | ArtefactType.BAM   | false
        "bam and analysable seqType"        | ArtefactType.BAM   | true
    }

    void "createWorkflowsTask, if decider throws exception, then send error E-Mail to ticketing system"() {
        given:
        FastqImportWorkflowCreatorScheduler scheduler = createWorkflowCreatorScheduler()
        List<WorkflowRun> runs = [createWorkflowRun()]
        OtpRuntimeException otpRuntimeException = new OtpRuntimeException("Decider throws exceptions")

        when:
        scheduler.createWorkflowsTask(importId)

        then:
        1 * scheduler.fastqImportInstanceService.updateState(fastqImportInstance.id, WorkflowCreateState.FAILED)
        0 * scheduler.fastqImportInstanceService._

        1 * scheduler.fastqImportInstanceService.countInstancesInWaitingState() >> 1
        0 * scheduler.messageSourceService.getMessage('workflow.bamImport.failedLoadingFastqImportInstance', _)
        1 * scheduler.dataInstallationInitializationService.createWorkflowRuns(fastqImportInstance) >> runs
        0 * scheduler.dataInstallationInitializationService._
        1 * scheduler.allDecider.decide(_) >> { throw otpRuntimeException }
        0 * scheduler.samplePairDeciderService.findOrCreateSamplePairs(_)
        1 * scheduler.notificationCreator.sendWorkflowCreateErrorMail(metaDataFile, otpRuntimeException)
    }

    @Override
    AbstractWorkflowCreatorScheduler createWorkflowCreatorScheduler() {
        return new FastqImportWorkflowCreatorScheduler([
                allDecider                           : Mock(AllDecider),
                dataInstallationInitializationService: Mock(DataInstallationInitializationService),
                fastqImportInstanceService           : Mock(FastqImportInstanceService),
                messageSourceService                 : Mock(MessageSourceService),
                metaDataFileService                  : new MetaDataFileService(),
                notificationCreator                  : Mock(NotificationCreator),
                samplePairDeciderService             : Mock(SamplePairDeciderService),
                workflowSystemService                : Mock(WorkflowSystemService),
        ])
    }
}
