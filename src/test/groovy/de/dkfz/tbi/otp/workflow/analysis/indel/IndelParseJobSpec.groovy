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
package de.dkfz.tbi.otp.workflow.analysis.indel

import grails.testing.gorm.DataTest
import spock.lang.*

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.indelcalling.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair
import de.dkfz.tbi.otp.domainFactory.pipelines.analysis.IndelDomainFactory
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

class IndelParseJobSpec extends Specification implements DataTest, WorkflowSystemDomainFactory {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                AbstractBamFile,
                RawSequenceFile,
                FastqFile,
                FileType,
                IndelCallingInstance,
                IndelSampleSwapDetection,
                IndelQualityControl,
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

    IndelParseJob job

    IndelCallingInstance indelCallingInstance
    Path sampleSwapJsonFile
    Path indelQcJsonFile

    void setup() {
        indelCallingInstance = IndelDomainFactory.INSTANCE.createInstance(IndelDomainFactory.INSTANCE.createSamplePairWithExternallyProcessedBamFiles())
        job = new IndelParseJob([
                concreteArtefactService: Mock(ConcreteArtefactService),
                indelWorkFileService: Mock(IndelWorkFileService),
                workflowStateChangeService: Mock(WorkflowStateChangeService),
        ])

        sampleSwapJsonFile = tempDir.resolve("sampleSwapJsonFile")
        indelQcJsonFile = tempDir.resolve("indelQcJsonFile")
    }

    void "parseOutputs should define sampleSwapDetection and quality control when both files available"() {
        given:
        WorkflowStep workflowStep = createWorkflowStep([workflowRun: createWorkflowRun([workflow: findOrCreateWorkflow(IndelWorkflow.WORKFLOW)])])
        Files.createFile(indelQcJsonFile)
        Files.write(indelQcJsonFile, [IndelDomainFactory.INSTANCE.qcFileContent])
        Files.createFile(sampleSwapJsonFile)
        Files.write(sampleSwapJsonFile, [IndelDomainFactory.INSTANCE.sampleSwapDetectionFileContent])

        when:
        job.parseOutputs(workflowStep)

        then:
        IndelSampleSwapDetection swapDetection = CollectionUtils.exactlyOneElement(IndelSampleSwapDetection.list())
        IndelQualityControl qualityControl = CollectionUtils.exactlyOneElement(IndelQualityControl.list())
        swapDetection.indelCallingInstance == indelCallingInstance
        qualityControl.indelCallingInstance == indelCallingInstance
        qualityControl.file != null
        qualityControl.numDels != null
        qualityControl.percentInsSize4_10 != null
        swapDetection.germlineSNVsHeterozygousInBothRare != null
        swapDetection.pid != null

        and:
        1 * job.indelWorkFileService.getIndelQcJsonFile(indelCallingInstance) >> indelQcJsonFile
        1 * job.indelWorkFileService.getSampleSwapJsonFile(indelCallingInstance) >> sampleSwapJsonFile
        1 * job.concreteArtefactService.getOutputArtefact(workflowStep, _) >> indelCallingInstance
    }

    void "parseOutputs should be idempotent"() {
        given:
        WorkflowStep workflowStep = createWorkflowStep([workflowRun: createWorkflowRun([workflow: findOrCreateWorkflow(IndelWorkflow.WORKFLOW)])])
        Files.createFile(indelQcJsonFile)
        Files.write(indelQcJsonFile, [IndelDomainFactory.INSTANCE.qcFileContent])
        Files.createFile(sampleSwapJsonFile)
        Files.write(sampleSwapJsonFile, [IndelDomainFactory.INSTANCE.sampleSwapDetectionFileContent])

        when:
        job.parseOutputs(workflowStep)
        job.parseOutputs(workflowStep)

        then:
        IndelSampleSwapDetection swapDetection = CollectionUtils.exactlyOneElement(IndelSampleSwapDetection.list())
        IndelQualityControl qualityControl = CollectionUtils.exactlyOneElement(IndelQualityControl.list())
        swapDetection.indelCallingInstance == indelCallingInstance
        qualityControl.indelCallingInstance == indelCallingInstance
        qualityControl.file != null
        qualityControl.numDels != null
        qualityControl.percentInsSize4_10 != null
        swapDetection.germlineSNVsHeterozygousInBothRare != null
        swapDetection.pid != null

        and:
        2 * job.indelWorkFileService.getIndelQcJsonFile(indelCallingInstance) >> indelQcJsonFile
        2 * job.indelWorkFileService.getSampleSwapJsonFile(indelCallingInstance) >> sampleSwapJsonFile
        2 * job.concreteArtefactService.getOutputArtefact(workflowStep, _) >> indelCallingInstance
    }
}
