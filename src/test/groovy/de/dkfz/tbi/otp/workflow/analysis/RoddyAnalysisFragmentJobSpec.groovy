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
package de.dkfz.tbi.otp.workflow.analysis

import grails.testing.gorm.DataTest
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.dataprocessing.BamFilePairAnalysis
import de.dkfz.tbi.otp.dataprocessing.BamFilePairAnalysisSpec
import de.dkfz.tbi.otp.dataprocessing.MergingWorkPackage
import de.dkfz.tbi.otp.dataprocessing.Pipeline
import de.dkfz.tbi.otp.dataprocessing.RoddyBamFile
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair
import de.dkfz.tbi.otp.domainFactory.pipelines.IsRoddy
import de.dkfz.tbi.otp.domainFactory.workflowSystem.AceseqWorkflowDomainFactory
import de.dkfz.tbi.otp.ngsdata.FastqFile
import de.dkfz.tbi.otp.ngsdata.FastqImportInstance
import de.dkfz.tbi.otp.ngsdata.FileType
import de.dkfz.tbi.otp.ngsdata.LibraryPreparationKit
import de.dkfz.tbi.otp.ngsdata.ReferenceGenomeProjectSeqType
import de.dkfz.tbi.otp.ngsdata.Sample
import de.dkfz.tbi.otp.ngsdata.SampleType
import de.dkfz.tbi.otp.ngsdata.SampleTypePerProject
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.workflow.ConcreteArtefactService
import de.dkfz.tbi.otp.workflowExecution.ProcessingPriority
import de.dkfz.tbi.otp.workflowExecution.SingleSelectSelectorExtendedCriteria
import de.dkfz.tbi.otp.workflowExecution.Workflow
import de.dkfz.tbi.otp.workflowExecution.WorkflowRun
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

class RoddyAnalysisFragmentJobSpec extends Specification implements DataTest, AceseqWorkflowDomainFactory, IsRoddy {

    private static final String ANALYSIS_OUTPUT = AbstractAnalysisWorkflow.ANALYSIS_OUTPUT

    @Override
    Class[] getDomainClassesToMock() {
        return [
                Workflow,
                ProcessingPriority,
                Project,
                WorkflowRun,
                Pipeline,
                LibraryPreparationKit,
                SampleType,
                Sample,
                MergingWorkPackage,
                ReferenceGenomeProjectSeqType,
                FileType,
                FastqImportInstance,
                FastqFile,
                RoddyWorkflowConfig,
                RoddyBamFile,
                SampleTypePerProject,
                SamplePair,
        ]
    }

    @Unroll
    void "fetchSelectors, when #name, then return only one criteria"() {
        given:
        WorkflowStep workflowStep = createWorkflowStep([
                workflowRun: createWorkflowRun([
                        workflow: findOrCreateAceseqWorkflow(),
                ]),
        ])
        BamFilePairAnalysis outputArtefact = BamFilePairAnalysisSpec.createMockBamFilePairAnalysis()

        RoddyAnalysisFragmentJob job = new RoddyAnalysisFragmentJob()
        job.concreteArtefactService = Mock(ConcreteArtefactService)

        when:
        List<SingleSelectSelectorExtendedCriteria> criteriaList = job.fetchSelectors(workflowStep)

        then:
        criteriaList.size() == 1
        SingleSelectSelectorExtendedCriteria criteria = criteriaList.first()

        criteria.workflow == workflowStep.workflowRun.workflow
        criteria.workflowVersion == workflowStep.workflowRun.workflowVersion
        criteria.project == workflowStep.workflowRun.project
        criteria.seqType == outputArtefact.seqType
        criteria.referenceGenome == outputArtefact.referenceGenome
        criteria.libraryPreparationKit == null

        and:
        1 * job.concreteArtefactService.getOutputArtefact(workflowStep, ANALYSIS_OUTPUT) >> outputArtefact
        0 * _
    }
}
