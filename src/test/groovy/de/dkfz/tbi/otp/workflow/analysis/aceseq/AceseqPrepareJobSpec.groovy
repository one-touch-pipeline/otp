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

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.aceseq.AceseqInstance
import de.dkfz.tbi.otp.dataprocessing.aceseq.AceseqWorkFileService
import de.dkfz.tbi.otp.dataprocessing.bamfiles.RoddyBamFileService
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair
import de.dkfz.tbi.otp.dataprocessing.sophia.SophiaInstance
import de.dkfz.tbi.otp.dataprocessing.sophia.SophiaWorkFileService
import de.dkfz.tbi.otp.domainFactory.pipelines.IsRoddy
import de.dkfz.tbi.otp.domainFactory.pipelines.analysis.AceseqDomainFactory
import de.dkfz.tbi.otp.domainFactory.pipelines.analysis.SophiaDomainFactory
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.LinkEntry
import de.dkfz.tbi.otp.workflow.ConcreteArtefactService
import de.dkfz.tbi.otp.workflow.analysis.AbstractAnalysisWorkflow
import de.dkfz.tbi.otp.workflowExecution.WorkflowRun
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

import java.nio.file.Path
import java.nio.file.Paths

class AceseqPrepareJobSpec extends Specification implements DataTest, WorkflowSystemDomainFactory, IsRoddy {

    AceseqInstance aceseqInstance
    SophiaInstance sophiaInstance
    WorkflowStep workflowStep

    RoddyBamFileService roddyBamFileService
    AceseqPrepareJob job

    String analysisOutput = AbstractAnalysisWorkflow.ANALYSIS_OUTPUT
    String sophiaInput = "SOPHIA"

    @Override
    Class[] getDomainClassesToMock() {
        return [
                WorkflowStep,
                WorkflowRun,
                ExternalMergingWorkPackage,
                ExternallyProcessedBamFile,
                SampleTypePerProject,
                ProcessingThresholds,
                MergingWorkPackage,
                ReferenceGenomeProjectSeqType,
                FileType,
                FastqImportInstance,
                FastqFile,
                RoddyWorkflowConfig,
                RoddyBamFile,
                SamplePair,
                AceseqInstance,
                SophiaInstance,
        ]
    }

    void setup() {
        AceseqDomainFactory aceseqDomainFactory = AceseqDomainFactory.INSTANCE
        SophiaDomainFactory sophiaDomainFactory = SophiaDomainFactory.INSTANCE
        SamplePair samplePair = aceseqDomainFactory.createSamplePairWithExternallyProcessedBamFiles()
        aceseqInstance = aceseqDomainFactory.createInstance(samplePair)
        sophiaInstance = sophiaDomainFactory.createInstance(samplePair, [processingState: AnalysisProcessingStates.FINISHED])
        workflowStep = createWorkflowStep([workflowRun: createWorkflowRun([workflow: findOrCreateWorkflow(AceseqWorkflow.WORKFLOW)])])
        job = new AceseqPrepareJob([
                aceseqWorkFileService  : Mock(AceseqWorkFileService),
                sophiaWorkFileService  : Mock(SophiaWorkFileService),
                concreteArtefactService: Mock(ConcreteArtefactService),
        ])
        roddyBamFileService = Mock(RoddyBamFileService)
    }

    void "buildWorkDirectoryPath, should return work directory"() {
        given:
        Path workDirectoryPath = Paths.get('/path')

        when:
        Path resultPath = job.buildWorkDirectoryPath(workflowStep)

        then:
        resultPath == workDirectoryPath

        and:
        1 * job.aceseqWorkFileService.getDirectoryPath(_) >> workDirectoryPath
        1 * job.concreteArtefactService.getOutputArtefact(workflowStep, analysisOutput) >> aceseqInstance
    }

    void "generateMapForLinking, should return the expected link entries"() {
        given:
        Path workDirectoryPath = Paths.get('/workPath')
        String finalAceseqInputFileName = 'aceqInputFile'
        Path finalAceseqInputFile = Paths.get('/path/to').resolve(finalAceseqInputFileName)

        when:
        Collection<LinkEntry> linkResults = job.generateMapForLinking(workflowStep)

        then:
        linkResults == [new LinkEntry([link: workDirectoryPath.resolve(finalAceseqInputFileName), target: finalAceseqInputFile])]

        and:
        1 * job.aceseqWorkFileService.getDirectoryPath(aceseqInstance) >> workDirectoryPath
        1 * job.concreteArtefactService.getOutputArtefact(workflowStep, analysisOutput) >> aceseqInstance
        1 * job.concreteArtefactService.getInputArtefact(workflowStep, sophiaInput) >> sophiaInstance
        1 * job.sophiaWorkFileService.getFinalAceseqInputFile(sophiaInstance) >> finalAceseqInputFile
    }

    void "doFurtherPreparation, should do nothing"() {
        when:
        job.doFurtherPreparation(workflowStep)

        then:
        0 * _
    }
}
