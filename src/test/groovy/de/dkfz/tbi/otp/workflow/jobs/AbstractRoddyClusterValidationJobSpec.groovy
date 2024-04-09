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
package de.dkfz.tbi.otp.workflow.jobs

import grails.testing.gorm.DataTest
import spock.lang.Specification
import spock.lang.TempDir

import de.dkfz.tbi.otp.dataprocessing.MergingWorkPackage
import de.dkfz.tbi.otp.dataprocessing.Pipeline
import de.dkfz.tbi.otp.dataprocessing.RoddyBamFile
import de.dkfz.tbi.otp.dataprocessing.roddy.JobStateLogFile
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyResult
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.domainFactory.pipelines.IsRoddy
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.infrastructure.ClusterJob
import de.dkfz.tbi.otp.ngsdata.FastqFile
import de.dkfz.tbi.otp.ngsdata.FastqImportInstance
import de.dkfz.tbi.otp.ngsdata.FileType
import de.dkfz.tbi.otp.ngsdata.LibraryPreparationKit
import de.dkfz.tbi.otp.ngsdata.ReferenceGenomeProjectSeqType
import de.dkfz.tbi.otp.ngsdata.Sample
import de.dkfz.tbi.otp.ngsdata.SampleType
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.CreateJobStateLogFileHelper
import de.dkfz.tbi.otp.workflow.RoddyService
import de.dkfz.tbi.otp.workflow.analysis.aceseq.AceseqWorkflow
import de.dkfz.tbi.otp.workflow.shared.ValidationJobFailedException
import de.dkfz.tbi.otp.workflowExecution.LogService
import de.dkfz.tbi.otp.workflowExecution.ProcessingPriority
import de.dkfz.tbi.otp.workflowExecution.Workflow
import de.dkfz.tbi.otp.workflowExecution.WorkflowRun
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep
import de.dkfz.tbi.otp.workflowExecution.WorkflowStepService

import java.nio.file.Path

class AbstractRoddyClusterValidationJobSpec extends Specification implements DataTest, WorkflowSystemDomainFactory, IsRoddy {
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
        ]
    }

    @TempDir
    Path tempDir

    AbstractRoddyClusterValidationJob job

    WorkflowStep workflowStep
    RoddyResult roddyResult

    void setup() {
        job = Spy(AbstractRoddyClusterValidationJob)
        job.workflowStepService = Mock(WorkflowStepService)
        job.logService = Mock(LogService)
        job.roddyService = Mock(RoddyService)

        workflowStep = createWorkflowStep()
        roddyResult = createBamFile()
    }

    void "ensureExternalJobsRunThrough, should run through successfully, when no cluster jobs are found"() {
        given:
        WorkflowStep previousWorkflowStep = createWorkflowStep([clusterJobs: []])

        when:
        job.ensureExternalJobsRunThrough(workflowStep)

        then:
        1 * job.workflowStepService.getPreviousRunningWorkflowStep(workflowStep) >> previousWorkflowStep
        1 * job.logService.addSimpleLogEntry(workflowStep) { it.contains("No cluster job found") }
    }

    void "ensureExternalJobsRunThrough, should throw exception, when cluster job did not finish successfully"() {
        given:
        ClusterJob successfulClusterJob = createClusterJob()
        JobStateLogFile.LogFileEntry successfulLogFileEntry = CreateJobStateLogFileHelper.createJobStateLogFileEntry([
                clusterJobId: successfulClusterJob.clusterJobId,
        ])
        ClusterJob failedClusterJob = createClusterJob()
        JobStateLogFile.LogFileEntry failedLogFileEntry = CreateJobStateLogFileHelper.createJobStateLogFileEntry([
                statusCode  : '12',
                clusterJobId: failedClusterJob.clusterJobId,
        ])
        JobStateLogFile jobStateLogFile = CreateJobStateLogFileHelper.createJobStateLogFile(tempDir.toFile(), [successfulLogFileEntry, failedLogFileEntry])
        WorkflowStep previousWorkflowStep = createWorkflowStep([clusterJobs: [successfulClusterJob, failedClusterJob]])

        when:
        job.ensureExternalJobsRunThrough(workflowStep)

        then:
        ValidationJobFailedException e = thrown(ValidationJobFailedException)
        e.message.contains("Status code of cluster job")
        e.message.contains(failedClusterJob.clusterJobId)
        !e.message.contains(successfulClusterJob.clusterJobId)

        and:
        1 * job.getRoddyResult(workflowStep) >> roddyResult
        1 * job.workflowStepService.getPreviousRunningWorkflowStep(workflowStep) >> previousWorkflowStep
        1 * job.roddyService.getJobStateLogFile(roddyResult) >> jobStateLogFile
        1 * job.logService.addSimpleLogEntry(workflowStep) { it.contains("Status code of cluster job") }
    }

    void "ensureExternalJobsRunThrough, should throw exception, when cluster job is not contained in logs"() {
        given:
        ClusterJob notLoggedClusterJob = createClusterJob()
        JobStateLogFile.LogFileEntry logFileEntry1 = CreateJobStateLogFileHelper.createJobStateLogFileEntry()
        ClusterJob clusterJob2 = createClusterJob()
        JobStateLogFile.LogFileEntry logFileEntry2 = CreateJobStateLogFileHelper.createJobStateLogFileEntry([clusterJobId: clusterJob2.clusterJobId])
        JobStateLogFile jobStateLogFile = CreateJobStateLogFileHelper.createJobStateLogFile(tempDir.toFile(), [logFileEntry1, logFileEntry2])
        WorkflowStep previousWorkflowStep = createWorkflowStep([clusterJobs: [notLoggedClusterJob, clusterJob2]])

        when:
        job.ensureExternalJobsRunThrough(workflowStep)

        then:
        ValidationJobFailedException e = thrown(ValidationJobFailedException)
        e.message.contains("contains no information for cluster job")
        e.message.contains(notLoggedClusterJob.clusterJobId)
        !e.message.contains(clusterJob2.clusterJobId)

        and:
        1 * job.getRoddyResult(workflowStep) >> roddyResult
        1 * job.workflowStepService.getPreviousRunningWorkflowStep(workflowStep) >> previousWorkflowStep
        1 * job.roddyService.getJobStateLogFile(roddyResult) >> jobStateLogFile
        1 * job.logService.addSimpleLogEntry(workflowStep) { it.contains("no information for cluster job") }
    }

    void "ensureExternalJobsRunThrough, should run through successfully, when no problems"() {
        given:
        ClusterJob successfulClusterJob1 = createClusterJob()
        JobStateLogFile.LogFileEntry successfulLogFileEntry1 = CreateJobStateLogFileHelper.createJobStateLogFileEntry([
                clusterJobId: successfulClusterJob1.clusterJobId])
        ClusterJob successfulClusterJob2 = createClusterJob()
        JobStateLogFile.LogFileEntry successfulLogFileEntry2 = CreateJobStateLogFileHelper.createJobStateLogFileEntry([
                clusterJobId: successfulClusterJob2.clusterJobId])
        JobStateLogFile jobStateLogFile = CreateJobStateLogFileHelper.createJobStateLogFile(tempDir.toFile(),
                [successfulLogFileEntry1, successfulLogFileEntry2])
        WorkflowStep previousWorkflowStep = createWorkflowStep([
                workflowRun: createWorkflowRun([workflow: findOrCreateWorkflow(AceseqWorkflow.WORKFLOW)]),
                clusterJobs: [successfulClusterJob1, successfulClusterJob2],
        ])

        when:
        job.ensureExternalJobsRunThrough(workflowStep)

        then:
        1 * job.getRoddyResult(workflowStep) >> roddyResult
        1 * job.workflowStepService.getPreviousRunningWorkflowStep(workflowStep) >> previousWorkflowStep
        1 * job.roddyService.getJobStateLogFile(roddyResult) >> jobStateLogFile
        1 * job.logService.addSimpleLogEntry(workflowStep) { it.contains("finished successfully") }
    }
}
