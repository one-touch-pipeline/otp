/*
 * Copyright 2011-2026 The OTP authors
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
import spock.lang.Specification

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.indelcalling.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair
import de.dkfz.tbi.otp.domainFactory.pipelines.analysis.IndelDomainFactory
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.workflow.analysis.*
import de.dkfz.tbi.otp.workflow.jobs.*
import de.dkfz.tbi.otp.workflowExecution.*

class IndelWorkflowSpec extends Specification implements DataTest, WorkflowSystemDomainFactory {

    private IndelWorkflow workflow

    @Override
    Class[] getDomainClassesToMock() {
        return [
                AbstractMergingWorkPackage,
                BamFilePairAnalysis,
                FastqFile,
                FastqImportInstance,
                FileType,
                IndelCallingInstance,
                IndelQualityControl,
                LibraryPreparationKit,
                MergingWorkPackage,
                Pipeline,
                ProcessingPriority,
                Project,
                RawSequenceFile,
                ReferenceGenomeProjectSeqType,
                RoddyBamFile,
                SamplePair,
                SampleType,
                SampleTypePerProject,
                SeqTrack,
                Workflow,
                WorkflowApiVersion,
                WorkflowArtefact,
                WorkflowRun,
                WorkflowStep,
                WorkflowVersion,
        ]
    }

    void setup() {
        workflow = new IndelWorkflow()
        workflow.indelWorkFileService = Mock(IndelWorkFileService)
    }

    void "getJobList for api version 1 should return all Indel V1 job bean names in correct order"() {
        expect:
        workflow.getJobList(1) == [
                AnalysisConditionalSkipJob,
                RoddyAnalysisFragmentJob,
                IndelCheckFragmentKeysV1Job,
                IndelConditionalFailJob,
                AttachUuidJob,
                IndelPrepareJob,
                IndelExecuteJob,
                IndelValidationJob,
                IndelParseJob,
                RoddyCleanupJob,
                SetCorrectPermissionJob,
                CalculateSizeJob,
                AnalysisLinkJob,
                AnalysisFinishJob,
        ]*.simpleName*.uncapitalize()
    }

    void "getJobList for api version 2 should return all Indel V2 job bean names in correct order"() {
        expect:
        workflow.getJobList(2) == [
                AnalysisConditionalSkipJob,
                RoddyAnalysisFragmentJob,
                IndelCheckFragmentKeysV2Job,
                IndelConditionalFailJob,
                AttachUuidJob,
                IndelPrepareJob,
                IndelExecuteJob,
                IndelValidationJob,
                IndelParseJob,
                RoddyCleanupJob,
                SetCorrectPermissionJob,
                CalculateSizeJob,
                AnalysisLinkJob,
                AnalysisFinishJob,
        ]*.simpleName*.uncapitalize()
    }

    void "createCopyOfArtefact should create new IndelCallingInstance with same properties and mark old as withdrawn"() {
        given:
        Map processablePair = IndelDomainFactory.INSTANCE.createProcessableSamplePair()
        SamplePair samplePair = processablePair.samplePair

        WorkflowVersion workflowVersion = createWorkflowVersion()
        WorkflowRun producedBy = createWorkflowRun(workflowVersion: workflowVersion)
        WorkflowArtefact workflowArtefact = createWorkflowArtefact(producedBy: producedBy, outputRole: "ANALYSIS_OUTPUT")

        IndelCallingInstance indelInstance = IndelDomainFactory.INSTANCE.createInstance(samplePair)
        indelInstance.workflowArtefact = workflowArtefact
        indelInstance.save(flush: true)

        String newInstanceName = "new-instance-${nextId}"
        workflow.indelWorkFileService = Mock(IndelWorkFileService) {
            1 * constructInstanceName(workflowVersion) >> newInstanceName
        }

        when:
        IndelCallingInstance result = workflow.createCopyOfArtefact(indelInstance) as IndelCallingInstance

        then:
        result.id != indelInstance.id
        result.samplePair == samplePair
        result.instanceName == newInstanceName
        result.config == indelInstance.config
        result.sampleType1BamFile == samplePair.mergingWorkPackage1.bamFileInProjectFolder
        result.sampleType2BamFile == samplePair.mergingWorkPackage2.bamFileInProjectFolder

        and:
        indelInstance.withdrawn
    }
}
