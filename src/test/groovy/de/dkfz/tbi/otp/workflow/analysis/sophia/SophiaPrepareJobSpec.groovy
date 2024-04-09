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

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile
import de.dkfz.tbi.otp.dataprocessing.ExternalMergingWorkPackage
import de.dkfz.tbi.otp.dataprocessing.ExternallyProcessedBamFile
import de.dkfz.tbi.otp.dataprocessing.MergingWorkPackage
import de.dkfz.tbi.otp.dataprocessing.Pipeline
import de.dkfz.tbi.otp.dataprocessing.ProcessingThresholds
import de.dkfz.tbi.otp.dataprocessing.RoddyBamFile
import de.dkfz.tbi.otp.dataprocessing.bamfiles.AbstractBamFileServiceFactoryService
import de.dkfz.tbi.otp.dataprocessing.bamfiles.RoddyBamFileService
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.dataprocessing.sophia.SophiaInstance
import de.dkfz.tbi.otp.dataprocessing.sophia.SophiaWorkFileService
import de.dkfz.tbi.otp.domainFactory.pipelines.IsRoddy
import de.dkfz.tbi.otp.domainFactory.pipelines.analysis.SophiaDomainFactory
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.ngsdata.FastqFile
import de.dkfz.tbi.otp.ngsdata.FastqImportInstance
import de.dkfz.tbi.otp.ngsdata.FileType
import de.dkfz.tbi.otp.ngsdata.ReferenceGenomeProjectSeqType
import de.dkfz.tbi.otp.ngsdata.Sample
import de.dkfz.tbi.otp.ngsdata.SampleType
import de.dkfz.tbi.otp.ngsdata.SampleTypePerProject
import de.dkfz.tbi.otp.utils.LinkEntry
import de.dkfz.tbi.otp.workflow.ConcreteArtefactService
import de.dkfz.tbi.otp.workflow.analysis.AbstractAnalysisWorkflow
import de.dkfz.tbi.otp.workflowExecution.WorkflowRun
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

import java.nio.file.Path
import java.nio.file.Paths

class SophiaPrepareJobSpec extends Specification implements DataTest, WorkflowSystemDomainFactory, IsRoddy {

    RoddyBamFileService roddyBamFileService
    SophiaPrepareJob job

    SophiaInstance sophiaInstance
    WorkflowStep workflowStep

    String inputControlBam = SophiaPrepareJob.de_dkfz_tbi_otp_workflow_analysis_AnalysisWorkflowShared__INPUT_CONTROL_BAM
    String inputTumorBam = SophiaPrepareJob.de_dkfz_tbi_otp_workflow_analysis_AnalysisWorkflowShared__INPUT_TUMOR_BAM
    String analysisOutput = AbstractAnalysisWorkflow.ANALYSIS_OUTPUT

    @Override
    Class[] getDomainClassesToMock() {
        return [
                WorkflowStep,
                WorkflowRun,
                Pipeline,
                SampleType,
                Sample,
                MergingWorkPackage,
                ReferenceGenomeProjectSeqType,
                FileType,
                FastqImportInstance,
                FastqFile,
                RoddyWorkflowConfig,
                SophiaInstance,
                SampleTypePerProject,
                RoddyBamFile,
                ProcessingThresholds,
                ExternalMergingWorkPackage,
                ExternallyProcessedBamFile,
        ]
    }

    void setup() {
        SophiaDomainFactory sophiaDomainFactory = SophiaDomainFactory.INSTANCE
        sophiaInstance = sophiaDomainFactory.createInstance(sophiaDomainFactory.createSamplePairWithExternallyProcessedBamFiles())
        workflowStep = createWorkflowStep([workflowRun: createWorkflowRun([workflow: findOrCreateWorkflow(SophiaWorkflow.WORKFLOW)])])
        job = new SophiaPrepareJob([
                sophiaWorkFileService               : Mock(SophiaWorkFileService),
                concreteArtefactService             : Mock(ConcreteArtefactService),
                abstractBamFileServiceFactoryService: Mock(AbstractBamFileServiceFactoryService),
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
        1 * job.sophiaWorkFileService.getDirectoryPath(sophiaInstance) >> workDirectoryPath
        1 * job.concreteArtefactService.getOutputArtefact(workflowStep, analysisOutput) >> sophiaInstance
    }

    void "generateMapForLinking, should return empty list"() {
        given:
        AbstractBamFile tumorBamFile = createBamFile()
        AbstractBamFile controlBamFile = createBamFile()

        Path workDirectoryPath = Paths.get('/workDirectory')
        Path furtherProcessingTumorPath = Paths.get('/furtherProcessingTumor').resolve(tumorBamFile.bamFileName)
        Path furtherProcessingControlPath = Paths.get('/furtherProcessingControl').resolve(controlBamFile.bamFileName)

        when:
        Collection<LinkEntry> linkResults = job.generateMapForLinking(workflowStep)

        then:
        TestCase.assertContainSame(linkResults, [
                new LinkEntry([link: workDirectoryPath.resolve(tumorBamFile.bamFileName), target: furtherProcessingTumorPath]),
                new LinkEntry([link: workDirectoryPath.resolve(controlBamFile.bamFileName), target: furtherProcessingControlPath]),
                new LinkEntry([link  : workDirectoryPath.resolve(controlBamFile.baiFileName),
                               target: furtherProcessingControlPath.resolveSibling(controlBamFile.baiFileName)]),
                new LinkEntry([link  : workDirectoryPath.resolve(tumorBamFile.baiFileName),
                               target: furtherProcessingTumorPath.resolveSibling(tumorBamFile.baiFileName)]),
        ])

        and:
        1 * job.sophiaWorkFileService.getDirectoryPath(sophiaInstance) >> workDirectoryPath
        1 * job.concreteArtefactService.getOutputArtefact(workflowStep, analysisOutput) >> sophiaInstance
        1 * job.concreteArtefactService.getInputArtefact(workflowStep, inputControlBam) >> controlBamFile
        1 * job.concreteArtefactService.getInputArtefact(workflowStep, inputTumorBam) >> tumorBamFile
        1 * job.abstractBamFileServiceFactoryService.getService(tumorBamFile) >> roddyBamFileService
        1 * job.abstractBamFileServiceFactoryService.getService(controlBamFile) >> roddyBamFileService
        1 * roddyBamFileService.getPathForFurtherProcessing(tumorBamFile) >> furtherProcessingTumorPath
        1 * roddyBamFileService.getPathForFurtherProcessing(controlBamFile) >> furtherProcessingControlPath
    }

    void "doFurtherPreparation, should do nothing"() {
        when:
        job.doFurtherPreparation(workflowStep)

        then:
        0 * _
    }
}
