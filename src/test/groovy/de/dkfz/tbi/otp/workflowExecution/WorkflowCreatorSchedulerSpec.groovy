/*
 * Copyright 2011-2022 The OTP authors
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

import grails.test.hibernate.HibernateSpec
import grails.testing.services.ServiceUnitTest
import spock.lang.Unroll

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePairDeciderService
import de.dkfz.tbi.otp.domainFactory.UserDomainFactory
import de.dkfz.tbi.otp.domainFactory.pipelines.RoddyPancanFactory
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.tracking.NotificationCreator
import de.dkfz.tbi.otp.utils.exceptions.OtpRuntimeException
import de.dkfz.tbi.otp.workflow.datainstallation.DataInstallationInitializationService
import de.dkfz.tbi.otp.workflowExecution.decider.AllDecider

class WorkflowCreatorSchedulerSpec extends HibernateSpec implements ServiceUnitTest<WorkflowCreatorScheduler>, RoddyPancanFactory, UserDomainFactory,
        WorkflowSystemDomainFactory {

    @Override
    List<Class> getDomainClasses() {
        return [
                MergingWorkPackage,
                MetaDataFile,
                ProcessingOption,
                RoddyBamFile,
        ]
    }

    @Override
    Closure doWithSpring() {
        { ->
            processingOptionService(ProcessingOptionService)
        }
    }

    void "scheduleCreateWorkflow, if system does't run, don't call createWorkflowRuns"() {
        given:
        WorkflowCreatorScheduler scheduler = createWorkflowCreatorScheduler()

        when:
        scheduler.scheduleCreateWorkflow()

        then:
        1 * scheduler.workflowSystemService.isEnabled() >> false

        0 * scheduler.fastqImportInstanceService.waiting()
        0 * scheduler.metaDataFileService.findByFastqImportInstance(_)
        0 * scheduler.fastqImportInstanceService.updateState(_, _)

        0 * scheduler.fastqImportInstanceService.countInstancesInWaitingState()
        0 * scheduler.dataInstallationInitializationService.createWorkflowRuns(_)
        0 * scheduler.allDecider.decide(_, _)
        0 * scheduler.samplePairDeciderService.findOrCreateSamplePairs(_)
        0 * scheduler.notificationCreator.sendWorkflowCreateSuccessMail(_)
    }

    void "scheduleCreateWorkflow, if system runs and waiting return null, then do not call createWorkflowRuns"() {
        given:
        WorkflowCreatorScheduler scheduler = createWorkflowCreatorScheduler()

        when:
        scheduler.scheduleCreateWorkflow()

        then:
        1 * scheduler.workflowSystemService.isEnabled() >> true

        1 * scheduler.fastqImportInstanceService.waiting() >> null
        0 * scheduler.metaDataFileService.findByFastqImportInstance(_)
        0 * scheduler.fastqImportInstanceService.updateState(_, _)

        0 * scheduler.fastqImportInstanceService.countInstancesInWaitingState()
        0 * scheduler.dataInstallationInitializationService.createWorkflowRuns(_)
        0 * scheduler.allDecider.decide(_, _)
        0 * scheduler.samplePairDeciderService.findOrCreateSamplePairs(_)
        0 * scheduler.notificationCreator.sendWorkflowCreateSuccessMail(_)
    }

    @Unroll
    void "createWorkflows, if system runs and waiting returns instance and all fine and #name, call all methods for success called"() {
        given:
        WorkflowCreatorScheduler scheduler = createWorkflowCreatorScheduler()

        findOrCreateProcessingOption(ProcessingOption.OptionName.OTP_SYSTEM_USER, createUser().username)
        List<SeqType> seqTypes = DomainFactory.createAllAnalysableSeqTypes()

        MetaDataFile metaDataFile = DomainFactory.createMetaDataFile()
        FastqImportInstance fastqImportInstance = DomainFactory.createFastqImportInstance()
        List<WorkflowRun> runs = [createWorkflowRun()]
        List<WorkflowArtefact> workflowArtefacts = runs.collect {
            createWorkflowArtefact([
                    producedBy  : it,
                    artefactType: artefactType,
            ])
        }

        List<AbstractMergedBamFile> expectedListForSnv = []
        if (artefactType == ArtefactType.BAM) {
            RoddyBamFile roddyBamFile = createBamFile([
                    workflowArtefact: workflowArtefacts.first(),
                    workPackage     : createMergingWorkPackage([
                            seqType: analysableSeqType ? seqTypes.first() : createSeqType([
                                    name: "OtherName",
                            ])
                    ]),
            ])
            if (analysableSeqType) {
                expectedListForSnv << roddyBamFile.workPackage
            }
        }

        applicationContext //initialize the applicationContext

        when:
        scheduler.scheduleCreateWorkflow()

        then:
        1 * scheduler.workflowSystemService.isEnabled() >> true

        1 * scheduler.fastqImportInstanceService.waiting() >> fastqImportInstance
        1 * scheduler.metaDataFileService.findByFastqImportInstance(_) >> metaDataFile
        1 * scheduler.fastqImportInstanceService.updateState(_, FastqImportInstance.WorkflowCreateState.PROCESSING)
        1 * scheduler.fastqImportInstanceService.updateState(_, FastqImportInstance.WorkflowCreateState.SUCCESS)

        1 * scheduler.fastqImportInstanceService.countInstancesInWaitingState() >> 1
        1 * scheduler.dataInstallationInitializationService.createWorkflowRuns(_) >> runs
        1 * scheduler.allDecider.decide(_, _) >> workflowArtefacts
        1 * scheduler.samplePairDeciderService.findOrCreateSamplePairs(expectedListForSnv)
        0 * scheduler.samplePairDeciderService._
        1 * scheduler.notificationCreator.sendWorkflowCreateSuccessMail(_)

        where:
        name                                | artefactType       | analysableSeqType
        "no bam and not analysable seqType" | ArtefactType.FASTQ | false
        "no bam and analysable seqType"     | ArtefactType.FASTQ | true
        "bam and not analysable seqType"    | ArtefactType.BAM   | false
        "bam and analysable seqType"        | ArtefactType.BAM   | true
    }

    void "createWorkflows, if system runs and waiting returns instance and decider throws exception, then send error E-Mail to ticketing system"() {
        given:
        WorkflowCreatorScheduler scheduler = createWorkflowCreatorScheduler()
        MetaDataFile metaDataFile = DomainFactory.createMetaDataFile()
        FastqImportInstance fastqImportInstance = DomainFactory.createFastqImportInstance()
        List<WorkflowRun> runs = [createWorkflowRun()]
        OtpRuntimeException otpRuntimeException = new OtpRuntimeException("Decider throws exceptions")

        when:
        scheduler.scheduleCreateWorkflow()

        then:
        1 * scheduler.workflowSystemService.isEnabled() >> true

        1 * scheduler.fastqImportInstanceService.waiting() >> fastqImportInstance
        1 * scheduler.metaDataFileService.findByFastqImportInstance(_) >> metaDataFile
        1 * scheduler.fastqImportInstanceService.updateState(_, FastqImportInstance.WorkflowCreateState.PROCESSING)
        1 * scheduler.fastqImportInstanceService.updateState(_, FastqImportInstance.WorkflowCreateState.FAILED)

        1 * scheduler.fastqImportInstanceService.countInstancesInWaitingState() >> 1
        1 * scheduler.dataInstallationInitializationService.createWorkflowRuns(_) >> runs
        1 * scheduler.allDecider.decide(_, _) >> { throw otpRuntimeException }
        0 * scheduler.samplePairDeciderService.findOrCreateSamplePairs(_)
        1 * scheduler.notificationCreator.sendWorkflowCreateErrorMail(metaDataFile, otpRuntimeException)
    }

    private WorkflowCreatorScheduler createWorkflowCreatorScheduler() {
        return new WorkflowCreatorScheduler([
                allDecider                           : Mock(AllDecider),
                dataInstallationInitializationService: Mock(DataInstallationInitializationService),
                fastqImportInstanceService           : Mock(FastqImportInstanceService),
                metaDataFileService                  : Mock(MetaDataFileService),
                notificationCreator                  : Mock(NotificationCreator),
                workflowSystemService                : Mock(WorkflowSystemService),
                samplePairDeciderService             : Mock(SamplePairDeciderService),
        ])
    }
}
