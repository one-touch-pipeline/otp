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
package de.dkfz.tbi.otp.workflow.alignment.alignment

import grails.testing.gorm.DataTest
import spock.lang.*

import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.config.OtpProperty
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.bamfiles.RoddyBamFileService
import de.dkfz.tbi.otp.domainFactory.pipelines.IsRoddy
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.infrastructure.ClusterJob
import de.dkfz.tbi.otp.job.processing.TestFileSystemService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.CreateFileHelper
import de.dkfz.tbi.otp.utils.CreateJobStateLogFileHelper
import de.dkfz.tbi.otp.workflow.ConcreteArtefactService
import de.dkfz.tbi.otp.workflow.RoddyService
import de.dkfz.tbi.otp.workflow.alignment.AbstractRoddyAlignmentValidationJob
import de.dkfz.tbi.otp.workflow.alignment.panCancer.PanCancerValidationJob
import de.dkfz.tbi.otp.workflow.shared.ValidationJobFailedException
import de.dkfz.tbi.otp.workflowExecution.*

import java.nio.file.Path

abstract class AbstractRoddyAlignmentValidationJobSpec extends Specification implements WorkflowSystemDomainFactory, DataTest, IsRoddy {

    private static final String STATUS_CODE_FINISHED = "0"
    private static final String STATUS_CODE_FAILED = "1"

    @TempDir
    Path tempDir

    protected WorkflowRun run

    protected WorkflowStep workflowStep

    protected AbstractBamFile abstractBamFile

    protected AbstractRoddyAlignmentValidationJob job

    protected TestConfigService configService

    @Override
    Class[] getDomainClassesToMock() {
        return [
                FastqFile,
                WorkflowStep,
                RoddyMergedBamQa,
                MergingWorkPackage,
                ReferenceGenomeProjectSeqType,
                FileType,
                FastqImportInstance,
                ClusterJob,
        ]
    }

    abstract protected String workflowName()

    abstract protected AbstractRoddyAlignmentValidationJob createJob()

    abstract protected AbstractBamFile createRoddyBamFile()

    void setup() {
        configService = new TestConfigService([(OtpProperty.PATH_CLUSTER_LOGS_OTP): tempDir.toString()])
        run = createWorkflowRun([
                workflow: createWorkflow([
                        name: workflowName(),
                ]),
        ])
        workflowStep = createWorkflowStep([workflowRun: run])
        job = createJob()
        abstractBamFile = createRoddyBamFile()
    }

    void cleanup() {
        configService?.clean()
    }

    @Unroll
    void "test doFurtherValidation, when the readGroups are identical, don't throw exception"() {
        given:
        job.fileSystemService = new TestFileSystemService()
        job.roddyService = Mock(RoddyService) {
            1 * getReadGroupsInBam(_) >> readGroupsInBam
            1 * getReadGroupsExpected(_) >> readGroupsExpected
        }
        job.concreteArtefactService = Mock(ConcreteArtefactService) {
            _ * getOutputArtefact(_, _) >> abstractBamFile
        }
        job.roddyBamFileService = Mock(RoddyBamFileService)

        when:
        job.doFurtherValidation(workflowStep)

        then:
        notThrown(ValidationJobFailedException)

        where:
        readGroupsInBam      | readGroupsExpected
        ["group1", "group2"] | ["group1", "group2"]
    }

    @Unroll
    void "test doFurtherValidation, when the readGroups are different, throw exception"() {
        given:
        final PanCancerValidationJob job = new PanCancerValidationJob()
        final AbstractBamFile bamFile = createRoddyBamFile(RoddyBamFile)

        job.fileSystemService = new TestFileSystemService()
        job.roddyService = Mock(RoddyService) {
            1 * getReadGroupsInBam(_) >> readGroupsInBam
            1 * getReadGroupsExpected(_) >> readGroupsExpected
        }
        job.concreteArtefactService = Mock(ConcreteArtefactService) {
            _ * getOutputArtefact(_, _) >> bamFile
        }
        job.roddyBamFileService = Mock(RoddyBamFileService)

        when:
        job.doFurtherValidation(workflowStep)

        then:
        thrown(ValidationJobFailedException)

        where:
        readGroupsInBam      | readGroupsExpected
        ["group1", "group2"] | ["group1"]
        []                   | ["group1", "group2"]
    }

    void "test ensureExternalJobsRunThrough, when all jobs run through successfully, then log that all run successfully"() {
        given:
        final PanCancerValidationJob job = new PanCancerValidationJob()
        final WorkflowStep workflowStepCurrent = createWorkflowStep([previous: workflowStep])

        final String testJobId = "cluster_job_id"
        createClusterJob([
                workflowStep: workflowStep,
                clusterJobId: testJobId,
                checkStatus : ClusterJob.CheckStatus.FINISHED,
                jobLog      : CreateFileHelper.createFile(tempDir.resolve("test.txt")).toString(),
        ])

        job.roddyService = Mock(RoddyService) {
            1 * getJobStateLogFile(_) >> CreateJobStateLogFileHelper.createJobStateLogFile(tempDir.toFile(), [
                    CreateJobStateLogFileHelper.createJobStateLogFileEntry([clusterJobId: testJobId, statusCode: STATUS_CODE_FINISHED]),
            ])
        }
        job.logService = Mock(LogService)
        job.workflowStepService = Mock(WorkflowStepService) {
            1 * getPreviousRunningWorkflowStep(workflowStepCurrent) >> workflowStep
        }
        job.roddyBamFileService = Mock(RoddyBamFileService)
        job.concreteArtefactService = Mock(ConcreteArtefactService) {
            1 * getOutputArtefact(workflowStepCurrent, _) >> abstractBamFile
        }

        when:
        job.ensureExternalJobsRunThrough(workflowStepCurrent)

        then:
        noExceptionThrown()
    }

    void "test ensureExternalJobsRunThrough, when not all jobs run through, then throw a ValidationJobFailedException"() {
        given:
        final WorkflowStep workflowStepCurrent = createWorkflowStep([previous: workflowStep])

        final String testJobId = "cluster_job_id"
        createClusterJob([
                workflowStep: workflowStep,
                clusterJobId: testJobId,
                checkStatus : ClusterJob.CheckStatus.FINISHED,
                jobLog      : CreateFileHelper.createFile(tempDir.resolve("test.txt")).toString(),
        ])

        job.roddyService = Mock(RoddyService) {
            1 * getJobStateLogFile(_) >> CreateJobStateLogFileHelper.createJobStateLogFile(tempDir.toFile(), [
                    CreateJobStateLogFileHelper.createJobStateLogFileEntry([clusterJobId: testJobId, statusCode: STATUS_CODE_FAILED]),
            ])
        }
        job.logService = Mock(LogService)
        job.workflowStepService = Mock(WorkflowStepService) {
            1 * getPreviousRunningWorkflowStep(workflowStepCurrent) >> workflowStep
        }
        job.roddyBamFileService = Mock(RoddyBamFileService)
        job.concreteArtefactService = Mock(ConcreteArtefactService) {
            1 * getOutputArtefact(workflowStepCurrent, _) >> abstractBamFile
        }

        when:
        job.ensureExternalJobsRunThrough(workflowStepCurrent)

        then:
        final ValidationJobFailedException e = thrown()
        e.message.contains("Status code of cluster job ${testJobId}: " + STATUS_CODE_FAILED)
    }
}
