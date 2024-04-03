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
package de.dkfz.tbi.otp.workflow.analysis.aceseq

import grails.testing.gorm.DataTest
import spock.lang.Specification
import spock.lang.TempDir

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.aceseq.AceseqInstance
import de.dkfz.tbi.otp.dataprocessing.aceseq.AceseqQc
import de.dkfz.tbi.otp.dataprocessing.aceseq.AceseqWorkFileService
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair
import de.dkfz.tbi.otp.domainFactory.pipelines.analysis.AceseqDomainFactory
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.job.processing.ProcessingStep
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.qcTrafficLight.QcThreshold
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.workflow.ConcreteArtefactService
import de.dkfz.tbi.otp.workflowExecution.WorkflowStateChangeService
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

import java.nio.file.Files
import java.nio.file.Path

class AceseqParseJobSpec extends Specification implements DataTest, WorkflowSystemDomainFactory {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                AbstractBamFile,
                RawSequenceFile,
                FastqFile,
                FileType,
                AceseqInstance,
                AceseqQc,
                Individual,
                MergingCriteria,
                MergingWorkPackage,
                Pipeline,
                ProcessingOption,
                ProcessingStep,
                Project,
                QcThreshold,
                Sample,
                SamplePair,
                SampleType,
                SampleTypePerProject,
                SeqCenter,
                SeqPlatform,
                SeqPlatformGroup,
                SequencingKitLabel,
                SeqTrack,
                SeqType,
                ReferenceGenome,
                ReferenceGenomeEntry,
                ReferenceGenomeProjectSeqType,
                RoddyBamFile,
                FastqImportInstance,
                ExternalMergingWorkPackage,
                ExternallyProcessedBamFile,
                ProcessingThresholds,
        ]
    }

    @TempDir
    Path tempDir

    AceseqParseJob job

    AceseqInstance aceseqInstance
    Path aceseqQcJsonFile

    void setup() {
        aceseqInstance = AceseqDomainFactory.INSTANCE.createInstance(AceseqDomainFactory.INSTANCE.createSamplePairWithExternallyProcessedBamFiles())
        job = new AceseqParseJob([
                aceseqWorkFileService: Mock(AceseqWorkFileService),
                concreteArtefactService: Mock(ConcreteArtefactService),
                workflowStateChangeService: Mock(WorkflowStateChangeService),
        ])

        aceseqQcJsonFile = tempDir.resolve("aceseqQcJsonFile")
    }

    void "parseOutputs, should define sampleSwapDetection and quality control when both files available"() {
        given:
        WorkflowStep workflowStep = createWorkflowStep([workflowRun: createWorkflowRun([workflow: findOrCreateWorkflow(AceseqWorkflow.WORKFLOW)])])
        Files.createFile(aceseqQcJsonFile)
        Files.write(aceseqQcJsonFile, [AceseqDomainFactory.INSTANCE.qcFileContent])

        when:
        job.parseOutputs(workflowStep)

        then:
        AceseqQc qualityControl1 = CollectionUtils.exactlyOneElement(AceseqQc.findAllByNumber(1))
        AceseqQc qualityControl2 = CollectionUtils.exactlyOneElement(AceseqQc.findAllByNumber(2))
        qualityControl1.aceseqInstance == aceseqInstance
        qualityControl1.gender != null
        qualityControl1.goodnessOfFit != null
        qualityControl2.aceseqInstance == aceseqInstance
        qualityControl2.gender != null
        qualityControl2.goodnessOfFit != null

        and:
        1 * job.aceseqWorkFileService.getQcJsonFile(aceseqInstance) >> aceseqQcJsonFile
        1 * job.concreteArtefactService.getOutputArtefact(workflowStep, _) >> aceseqInstance
    }

    void "parseOutputs, should be idempotent"() {
        given:
        WorkflowStep workflowStep = createWorkflowStep([workflowRun: createWorkflowRun([workflow: findOrCreateWorkflow(AceseqWorkflow.WORKFLOW)])])
        Files.createFile(aceseqQcJsonFile)
        Files.write(aceseqQcJsonFile, [AceseqDomainFactory.INSTANCE.qcFileContent])

        when:
        job.parseOutputs(workflowStep)
        job.parseOutputs(workflowStep)

        then:
        AceseqQc qualityControl1 = CollectionUtils.exactlyOneElement(AceseqQc.findAllByNumber(1))
        AceseqQc qualityControl2 = CollectionUtils.exactlyOneElement(AceseqQc.findAllByNumber(2))
        qualityControl1.aceseqInstance == aceseqInstance
        qualityControl1.gender != null
        qualityControl1.goodnessOfFit != null
        qualityControl2.aceseqInstance == aceseqInstance
        qualityControl2.gender != null
        qualityControl2.goodnessOfFit != null

        and:
        2 * job.aceseqWorkFileService.getQcJsonFile(aceseqInstance) >> aceseqQcJsonFile
        2 * job.concreteArtefactService.getOutputArtefact(workflowStep, _) >> aceseqInstance
    }
}
