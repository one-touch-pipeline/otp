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
package de.dkfz.tbi.otp.workflow.analysis.sophia

import grails.testing.gorm.DataTest
import spock.lang.Specification
import spock.lang.TempDir

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.sophia.SophiaInstance
import de.dkfz.tbi.otp.dataprocessing.sophia.SophiaQc
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair
import de.dkfz.tbi.otp.dataprocessing.sophia.SophiaWorkFileService
import de.dkfz.tbi.otp.domainFactory.pipelines.analysis.SophiaDomainFactory
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

class SophiaParseJobSpec extends Specification implements DataTest, WorkflowSystemDomainFactory {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                AbstractBamFile,
                RawSequenceFile,
                FastqFile,
                FileType,
                SophiaInstance,
                SophiaQc,
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

    SophiaParseJob job

    SophiaInstance sophiaInstance
    Path sophiaQcJsonFile

    void setup() {
        sophiaInstance = SophiaDomainFactory.INSTANCE.createInstance(SophiaDomainFactory.INSTANCE.createSamplePairWithExternallyProcessedBamFiles())
        job = new SophiaParseJob([
                sophiaWorkFileService: Mock(SophiaWorkFileService),
                concreteArtefactService: Mock(ConcreteArtefactService),
                workflowStateChangeService: Mock(WorkflowStateChangeService),
        ])

        sophiaQcJsonFile = tempDir.resolve("sophiaQcJsonFile")
    }

    void "parseOutputs, should define sampleSwapDetection and quality control when both files available"() {
        given:
        WorkflowStep workflowStep = createWorkflowStep([workflowRun: createWorkflowRun([workflow: findOrCreateWorkflow(SophiaWorkflow.WORKFLOW)])])
        Files.createFile(sophiaQcJsonFile)
        Files.write(sophiaQcJsonFile, [SophiaDomainFactory.INSTANCE.qcFileContent])

        when:
        job.parseOutputs(workflowStep)

        then:
        SophiaQc qualityControl = CollectionUtils.exactlyOneElement(SophiaQc.list())
        qualityControl.sophiaInstance == sophiaInstance
        qualityControl.rnaContaminatedGenesMoreThanTwoIntron != null
        !qualityControl.rnaDecontaminationApplied
        qualityControl.rnaContaminatedGenesCount != null

        and:
        1 * job.sophiaWorkFileService.getQcJsonFile(sophiaInstance) >> sophiaQcJsonFile
        1 * job.concreteArtefactService.getOutputArtefact(workflowStep, _) >> sophiaInstance
    }

    void "parseOutputs, should be idempotent"() {
        given:
        WorkflowStep workflowStep = createWorkflowStep([workflowRun: createWorkflowRun([workflow: findOrCreateWorkflow(SophiaWorkflow.WORKFLOW)])])
        Files.createFile(sophiaQcJsonFile)
        Files.write(sophiaQcJsonFile, [SophiaDomainFactory.INSTANCE.qcFileContent])

        when:
        job.parseOutputs(workflowStep)
        job.parseOutputs(workflowStep)

        then:
        SophiaQc qualityControl = CollectionUtils.exactlyOneElement(SophiaQc.list())
        qualityControl.sophiaInstance == sophiaInstance
        qualityControl.rnaContaminatedGenesMoreThanTwoIntron != null
        !qualityControl.rnaDecontaminationApplied
        qualityControl.rnaContaminatedGenesCount != null

        and:
        2 * job.sophiaWorkFileService.getQcJsonFile(sophiaInstance) >> sophiaQcJsonFile
        2 * job.concreteArtefactService.getOutputArtefact(workflowStep, _) >> sophiaInstance
    }
}
