/*
 * Copyright 2011-2023 The OTP authors
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
package de.dkfz.tbi.otp.workflow.alignment.rna

import grails.testing.gorm.DataTest
import spock.lang.Specification
import spock.lang.TempDir

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.bamfiles.RnaRoddyBamFileService
import de.dkfz.tbi.otp.dataprocessing.rnaAlignment.RnaRoddyBamFile
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.domainFactory.pipelines.roddyRna.RoddyRnaFactory
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.workflow.jobs.JobStage
import de.dkfz.tbi.otp.workflowExecution.*

import java.nio.file.Path

class RnaAlignmentParseJobSpec extends Specification implements WorkflowSystemDomainFactory, DataTest, RoddyRnaFactory {

    private RnaAlignmentParseJob job

    @Override
    Class[] getDomainClassesToMock() {
        return [
                FastqFile,
                FastqImportInstance,
                FileType,
                LibraryPreparationKit,
                MergingWorkPackage,
                Pipeline,
                ProcessingPriority,
                Project,
                Realm,
                ReferenceGenomeEntry,
                ReferenceGenomeProjectSeqType,
                RnaQualityAssessment,
                RnaRoddyBamFile,
                RoddyMergedBamQa,
                RoddyWorkflowConfig,
                Sample,
                SampleType,
                Workflow,
                WorkflowRun,
        ]
    }

    @TempDir
    Path tempDir

    void "test execute"() {
        given:
        WorkflowStep workflowStep = createWorkflowStep()
        RnaRoddyBamFile bamFile = createBamFile()

        Path mergedQAJsonFile = tempDir.resolve("qa.json")
        mergedQAJsonFile.text = qaFileContent

        job = Spy(RnaAlignmentParseJob)
        job.abstractQualityAssessmentService = new RoddyQualityAssessmentService()
        job.abstractQualityAssessmentService.rnaRoddyBamFileService = Mock(RnaRoddyBamFileService) {
            getWorkMergedQAJsonFile(_) >> mergedQAJsonFile
        }
        job.workflowStateChangeService = Mock(WorkflowStateChangeService)
        job.getRoddyBamFile(workflowStep) >> bamFile

        when:
        job.execute(workflowStep)

        then:
        RnaQualityAssessment qa = CollectionUtils.exactlyOneElement(RnaQualityAssessment.findAllByAbstractBamFile(bamFile))
        qaValuesProperties.each { k, v ->
            assert qa."${k}" == v
        }

        1 * job.workflowStateChangeService.changeStateToSuccess(workflowStep)
        bamFile.qualityAssessmentStatus == AbstractBamFile.QaProcessingStatus.FINISHED
    }

    void "test execute, when QA exists"() {
        given:
        WorkflowStep workflowStep = createWorkflowStep()
        RnaRoddyBamFile bamFile = createBamFile()
        RnaQualityAssessment existingQa = createQa(bamFile, [
                insertSizeMean                   : 999,
                intergenicRate                   : 777.123,
        ]) as RnaQualityAssessment

        Path mergedQAJsonFile = tempDir.resolve("qa.json")
        mergedQAJsonFile.text = qaFileContent

        job = Spy(RnaAlignmentParseJob)
        job.abstractQualityAssessmentService = new RoddyQualityAssessmentService()
        job.abstractQualityAssessmentService.rnaRoddyBamFileService = Mock(RnaRoddyBamFileService) {
            getWorkMergedQAJsonFile(_) >> mergedQAJsonFile
        }
        job.workflowStateChangeService = Mock(WorkflowStateChangeService)
        job.getRoddyBamFile(workflowStep) >> bamFile

        when:
        job.execute(workflowStep)

        then:
        RnaQualityAssessment qa = CollectionUtils.exactlyOneElement(RnaQualityAssessment.findAllByAbstractBamFile(bamFile))
        qaValuesProperties.each { k, v ->
            assert qa."${k}" == v
        }
        // check that existing object is reused
        qa == existingQa

        1 * job.workflowStateChangeService.changeStateToSuccess(workflowStep)
        bamFile.qualityAssessmentStatus == AbstractBamFile.QaProcessingStatus.FINISHED
    }

    void "test getJobStage"() {
        given:
        job = new RnaAlignmentParseJob()

        expect:
        job.jobStage == JobStage.PARSE
    }
}
