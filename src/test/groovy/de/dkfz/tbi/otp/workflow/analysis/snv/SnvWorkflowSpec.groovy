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
package de.dkfz.tbi.otp.workflow.analysis.snv

import grails.testing.gorm.DataTest
import spock.lang.Specification

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.domainFactory.pipelines.analysis.SnvDomainFactory
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.workflow.analysis.*
import de.dkfz.tbi.otp.workflow.jobs.*
import de.dkfz.tbi.otp.workflowExecution.*

class SnvWorkflowSpec extends Specification implements DataTest, WorkflowSystemDomainFactory {

    private SnvWorkflow workflow

    @Override
    Class[] getDomainClassesToMock() {
        return [
                AbstractMergingWorkPackage,
                BamFilePairAnalysis,
                FastqFile,
                FastqImportInstance,
                FileType,
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
                SnvCallingInstance,
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
        workflow = new SnvWorkflow()
        workflow.snvWorkFileService = Mock(SnvWorkFileService)
    }

    void "getJobList for api version 1 should return all SNV V1 job bean names in correct order"() {
        expect:
        workflow.getJobList(1) == [
                AnalysisConditionalSkipJob,
                RoddyAnalysisFragmentJob,
                SnvCheckFragmentKeysV1Job,
                SnvConditionalFailJob,
                AttachUuidJob,
                SnvPrepareJob,
                SnvExecuteJob,
                SnvValidationJob,
                RoddyCleanupJob,
                SetCorrectPermissionJob,
                CalculateSizeJob,
                AnalysisLinkJob,
                AnalysisFinishJob,
        ]*.simpleName*.uncapitalize()
    }

    void "getJobList for api version 2 should return all SNV V2 job bean names in correct order"() {
        expect:
        workflow.getJobList(2) == [
                AnalysisConditionalSkipJob,
                RoddyAnalysisFragmentJob,
                SnvCheckFragmentKeysV2Job,
                SnvConditionalFailJob,
                AttachUuidJob,
                SnvPrepareJob,
                SnvExecuteJob,
                SnvValidationJob,
                RoddyCleanupJob,
                SetCorrectPermissionJob,
                CalculateSizeJob,
                AnalysisLinkJob,
                AnalysisFinishJob,
        ]*.simpleName*.uncapitalize()
    }

    void "createCopyOfArtefact should create new SnvCallingInstance with same properties and mark old as withdrawn"() {
        given:
        Map processablePair = SnvDomainFactory.INSTANCE.createProcessableSamplePair()
        SamplePair samplePair = processablePair.samplePair

        WorkflowVersion workflowVersion = createWorkflowVersion()
        WorkflowRun producedBy = createWorkflowRun(workflowVersion: workflowVersion)
        WorkflowArtefact workflowArtefact = createWorkflowArtefact(producedBy: producedBy, outputRole: "ANALYSIS_OUTPUT")

        SnvCallingInstance snvInstance = SnvDomainFactory.INSTANCE.createInstance(samplePair)
        snvInstance.workflowArtefact = workflowArtefact
        snvInstance.save(flush: true)

        String newInstanceName = "new-instance-${nextId}"
        workflow.snvWorkFileService = Mock(SnvWorkFileService) {
            1 * constructInstanceName(workflowVersion) >> newInstanceName
        }

        when:
        SnvCallingInstance result = workflow.createCopyOfArtefact(snvInstance) as SnvCallingInstance

        then:
        result.id != snvInstance.id
        result.samplePair == samplePair
        result.instanceName == newInstanceName
        result.config == snvInstance.config
        result.sampleType1BamFile == samplePair.mergingWorkPackage1.bamFileInProjectFolder
        result.sampleType2BamFile == samplePair.mergingWorkPackage2.bamFileInProjectFolder

        and:
        snvInstance.withdrawn
    }
}
