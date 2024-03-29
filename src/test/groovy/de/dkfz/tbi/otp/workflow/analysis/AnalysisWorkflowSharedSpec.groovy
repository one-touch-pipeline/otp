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

import grails.testing.gorm.DataTest
import spock.lang.Specification

import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile
import de.dkfz.tbi.otp.dataprocessing.BamFilePairAnalysis
import de.dkfz.tbi.otp.dataprocessing.BamFilePairAnalysisSpec
import de.dkfz.tbi.otp.dataprocessing.MergingWorkPackage
import de.dkfz.tbi.otp.dataprocessing.Pipeline
import de.dkfz.tbi.otp.dataprocessing.RoddyBamFile
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair
import de.dkfz.tbi.otp.domainFactory.pipelines.IsRoddy
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.ngsdata.FastqFile
import de.dkfz.tbi.otp.ngsdata.FastqImportInstance
import de.dkfz.tbi.otp.ngsdata.FileType
import de.dkfz.tbi.otp.ngsdata.ReferenceGenomeProjectSeqType
import de.dkfz.tbi.otp.ngsdata.SampleTypePerProject
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.workflow.ConcreteArtefactService
import de.dkfz.tbi.otp.workflow.analysis.AbstractAnalysisWorkflow
import de.dkfz.tbi.otp.workflow.analysis.AnalysisWorkflowShared
import de.dkfz.tbi.otp.workflow.analysis.aceseq.AceseqWorkflow
import de.dkfz.tbi.otp.workflowExecution.WorkflowRun
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

class AnalysisWorkflowSharedSpec extends Specification implements WorkflowSystemDomainFactory, DataTest, IsRoddy {

    private WorkflowStep workflowStep
    private AnalysisWorkflowSharedInstance analysisWorkflowSharedInstance
    private AbstractBamFile tumorBamFile
    private AbstractBamFile controlBamFile
    private BamFilePairAnalysis outputInstance
    private static final String INPUT_TUMOR_BAM = AnalysisWorkflowSharedInstance.de_dkfz_tbi_otp_workflow_analysis_AnalysisWorkflowShared__INPUT_TUMOR_BAM
    private static final String INPUT_CONTROL_BAM = AnalysisWorkflowSharedInstance.de_dkfz_tbi_otp_workflow_analysis_AnalysisWorkflowShared__INPUT_CONTROL_BAM
    private static final String ANALYSIS_OUTPUT = AbstractAnalysisWorkflow.ANALYSIS_OUTPUT

    @Override
    Class[] getDomainClassesToMock() {
        return [
                WorkflowRun,
                WorkflowStep,
                SeqTrack,
                Pipeline,
                MergingWorkPackage,
                ReferenceGenomeProjectSeqType,
                FileType,
                FastqImportInstance,
                FastqFile,
                RoddyWorkflowConfig,
                RoddyBamFile,
                SamplePair,
                SampleTypePerProject,
        ]
    }

    private void createData() {
        analysisWorkflowSharedInstance = Spy(AnalysisWorkflowSharedInstance)
        tumorBamFile = createBamFile()
        controlBamFile = createBamFile()
        outputInstance = BamFilePairAnalysisSpec.createMockBamFilePairAnalysis()
        analysisWorkflowSharedInstance.concreteArtefactService = Mock(ConcreteArtefactService)
        final WorkflowRun run = createWorkflowRun([
                workflow: createWorkflow([
                        name: AceseqWorkflow.WORKFLOW
                ]),
        ])
        workflowStep = createWorkflowStep([workflowRun: run])
    }

    void "getTumorBamFile, should call checkWorkflowName and getTumorBamFile"() {
        given:
        createData()

        when:
        AbstractBamFile result = analysisWorkflowSharedInstance.getTumorBamFile(workflowStep)

        then:
        result == tumorBamFile

        and:
        1 * analysisWorkflowSharedInstance.checkWorkflowName(workflowStep, _)
        1 * analysisWorkflowSharedInstance.concreteArtefactService.getInputArtefact(workflowStep, INPUT_TUMOR_BAM) >> tumorBamFile
    }

    void "getControlBamFile, should call checkWorkflowName and getControlBamFile"() {
        given:
        createData()

        when:
        AbstractBamFile result = analysisWorkflowSharedInstance.getControlBamFile(workflowStep)

        then:
        result == controlBamFile

        and:
        1 * analysisWorkflowSharedInstance.checkWorkflowName(workflowStep, _)
        1 * analysisWorkflowSharedInstance.concreteArtefactService.getInputArtefact(workflowStep, INPUT_CONTROL_BAM) >> controlBamFile
    }

    void "getOutputInstance, should return the analysis output instance"() {
        given:
        createData()

        when:
        BamFilePairAnalysis result = analysisWorkflowSharedInstance.getOutputInstance(workflowStep)

        then:
        result == outputInstance

        and:
        1 * analysisWorkflowSharedInstance.checkWorkflowName(workflowStep, _)
        1 * analysisWorkflowSharedInstance.concreteArtefactService.getOutputArtefact(workflowStep, ANALYSIS_OUTPUT) >> outputInstance
    }

    @SuppressWarnings('EmptyClass')
    static class AnalysisWorkflowSharedInstance implements AnalysisWorkflowShared {
    }
}
