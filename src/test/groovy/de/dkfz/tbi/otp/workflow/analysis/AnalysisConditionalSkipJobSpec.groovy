/*
 * Copyright 2011-2025 The OTP authors
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

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.domainFactory.pipelines.IsRoddy
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.workflow.ConcreteArtefactService
import de.dkfz.tbi.otp.workflow.analysis.snv.SnvWorkflow
import de.dkfz.tbi.otp.workflow.shared.SkipWorkflowStepException
import de.dkfz.tbi.otp.workflowExecution.*

class AnalysisConditionalSkipJobSpec extends Specification implements DataTest, WorkflowSystemDomainFactory, IsRoddy {

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
                ExternalMergingWorkPackage,
                ExternallyProcessedBamFile,
        ]
    }

    WorkflowStep workflowStep
    AbstractBamFile tumorBamFile
    AbstractBamFile controlBamFile
    AnalysisConditionalSkipJob job

    static final String INPUT_TUMOR_BAM = AnalysisConditionalSkipJob.de_dkfz_tbi_otp_workflow_analysis_AnalysisWorkflowShared__INPUT_TUMOR_BAM
    static final String INPUT_CONTROL_BAM = AnalysisConditionalSkipJob.de_dkfz_tbi_otp_workflow_analysis_AnalysisWorkflowShared__INPUT_CONTROL_BAM

    void setup() {
        workflowStep = createWorkflowStep([workflowRun: createWorkflowRun([
                workflowVersion: null,
                workflow       : findOrCreateWorkflow(SnvWorkflow.WORKFLOW, [beanName: SnvWorkflow.simpleName.uncapitalize()]),
        ])])

        job = new AnalysisConditionalSkipJob()
        job.concreteArtefactService = Mock(ConcreteArtefactService)
    }

    void "checkRequirements should throw skip exception when workflow threshold is not reached by #bamFileType bam file"() {
        given:
        tumorBamFile = createCustomBamFile([coverage: tumorCoverage])
        controlBamFile = createCustomBamFile([coverage: controlCoverage])

        when:
        job.checkRequirements(workflowStep)

        then:
        SkipWorkflowStepException skipException = thrown(SkipWorkflowStepException)
        skipException.message.contains(expectedMessage)
        skipException.skipMessage.category == WorkflowStepSkipMessage.Category.WORKFLOW_COVERAGE_REJECTION

        and:
        1 * job.concreteArtefactService.getInputArtefact(workflowStep, INPUT_TUMOR_BAM) >> tumorBamFile
        1 * job.concreteArtefactService.getInputArtefact(workflowStep, INPUT_CONTROL_BAM) >> controlBamFile
        0 * _

        where:
        bamFileType         | tumorCoverage | controlCoverage | expectedMessage
        'tumor'             | 3             | 20              | '3.0'
        'control'           | 40            | 4               | '4.0'
        'control and tumor' | 5             | 2               | '5.0'
    }

    void "checkRequirements should run through successfully when all threshold are reached"() {
        given:
        tumorBamFile = createCustomBamFile([coverage: 26, numberOfMergedLanes: 10])
        controlBamFile = createCustomBamFile([coverage: 21, numberOfMergedLanes: 21])

        when:
        job.checkRequirements(workflowStep)

        then:
        1 * job.concreteArtefactService.getInputArtefact(workflowStep, INPUT_TUMOR_BAM) >> tumorBamFile
        1 * job.concreteArtefactService.getInputArtefact(workflowStep, INPUT_CONTROL_BAM) >> controlBamFile
        0 * _
    }

    void "checkRequirements should run through successfully when all threshold are reached and #description"() {
        given:
        tumorBamFile = createCustomBamFile([coverage: tumorCoverage, numberOfMergedLanes: tumorLanes])
        controlBamFile = createCustomBamFile([coverage: controlCoverage, numberOfMergedLanes: controlLanes])

        when:
        job.checkRequirements(workflowStep)

        then:
        1 * job.concreteArtefactService.getInputArtefact(workflowStep, INPUT_TUMOR_BAM) >> tumorBamFile
        1 * job.concreteArtefactService.getInputArtefact(workflowStep, INPUT_CONTROL_BAM) >> controlBamFile
        0 * _

        where:
        description                                | tumorCoverage | tumorLanes | controlCoverage | controlLanes
        'tumor coverage is null'                   | null          | 10         | 22              | 20
        'control and tumor coverage is null'       | null          | 8          | null            | 12
        'control lanes and coverage is null'       | 25            | 1          | null            | null
        'tumor lanes and control coverage is null' | 25            | null       | null            | 10
    }

    AbstractBamFile createCustomBamFile(Map properties) {
        if (properties.numberOfMergedLanes == null) {
            return DomainFactory.createExternallyProcessedBamFile([coverage: properties.coverage])
        }
        return createBamFile([coverage: properties.coverage, numberOfMergedLanes: properties.numberOfMergedLanes])
    }
}
