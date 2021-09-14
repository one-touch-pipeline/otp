/*
 * Copyright 2011-2021 The OTP authors
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
package de.dkfz.tbi.otp.workflow.panCancer

import grails.testing.gorm.DataTest
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.config.OtpProperty
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.domainFactory.pipelines.IsRoddy
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.infrastructure.ClusterJob
import de.dkfz.tbi.otp.job.processing.TestFileSystemService
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.CreateFileHelper
import de.dkfz.tbi.otp.utils.CreateJobStateLogFileHelper
import de.dkfz.tbi.otp.workflow.ConcreteArtefactService
import de.dkfz.tbi.otp.workflow.RoddyService
import de.dkfz.tbi.otp.workflow.shared.ValidationJobFailedException
import de.dkfz.tbi.otp.workflowExecution.LogService
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep
import de.dkfz.tbi.otp.workflowExecution.WorkflowStepService

import java.nio.file.Path

class PanCancerValidationJobSpec extends Specification implements WorkflowSystemDomainFactory, DataTest, IsRoddy {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                WorkflowStep,
                RoddyBamFile,
                RoddyMergedBamQa,
                MergingWorkPackage,
                ReferenceGenomeProjectSeqType,
                FileType,
                FastqImportInstance,
                ClusterJob,
        ]
    }

    @Rule
    TemporaryFolder temporaryFolder

    final WorkflowStep workflowStep = createWorkflowStep()

    TestConfigService configService

    static final String STATUS_CODE_FAILED = "1"
    static final String STATUS_CODE_FINISHED = "0"

    void setup() {
        configService = new TestConfigService([(OtpProperty.PATH_CLUSTER_LOGS_OTP): temporaryFolder.newFolder().path])
    }

    void cleanup() {
        configService.clean()
    }

    @Unroll
    void "test doFurtherValidationAndReturnProblems, when called the readGroups are checked for errors"() {
        given:
        final PanCancerValidationJob job = new PanCancerValidationJob()
        final RoddyBamFile bamFile = createRoddyBamFile(RoddyBamFile)

        job.fileSystemService = new TestFileSystemService()
        job.roddyService = Mock(RoddyService) {
            1 * getReadGroupsInBam(_) >> readGroupsInBam
            1 * getReadGroupsExpected(_) >> readGroupsExpected
        }
        job.concreteArtefactService = Mock(ConcreteArtefactService) {
            _ * getOutputArtefact(_, _, _) >> bamFile
        }

        when:
        List<String> errors = job.doFurtherValidationAndReturnProblems(workflowStep)

        then:
        errors.size() == nErrors

        where:
        nErrors | readGroupsInBam      | readGroupsExpected
        0       | ["group1", "group2"] | ["group1", "group2"]
        1       | ["group1", "group2"] | ["group1"]
        1       | []                   | ["group1", "group2"]
    }

    @Unroll
    void "test getExpectedXxx(), when called the correct paths (files or directories) should be returned"() {
        given:
        final PanCancerValidationJob job = new PanCancerValidationJob()
        final RoddyBamFile bamFile = createRoddyBamFile(RoddyBamFile)

        // an extra file is added to the return list if the bamFile's seqType is exome
        bamFile.seqType.name = isExome ? SeqTypeNames.EXOME.seqTypeName : ''

        List<Path> expectedFiles = [
                bamFile.workBamFile.toPath(),
                bamFile.workBaiFile.toPath(),
                bamFile.workMd5sumFile.toPath(),
                bamFile.workMergedQAJsonFile.toPath(),
        ]
        if (isExome) {
            expectedFiles.add(bamFile.workMergedQATargetExtractJsonFile.toPath())
        }

        List<Path> expectedDirectories = [
                bamFile.workDirectory.toPath(),
                bamFile.workExecutionStoreDirectory.toPath(),
        ]

        job.concreteArtefactService = Mock(ConcreteArtefactService) {
            _ * getOutputArtefact(_, _, _) >> bamFile
        }
        job.fileSystemService = new TestFileSystemService()

        when:
        List<Path> files = job.getExpectedFiles(workflowStep)
        List<Path> directories = job.getExpectedDirectories(workflowStep)

        then:
        files.sort()       == expectedFiles.sort()
        directories.sort() == expectedDirectories.sort()

        where:
        nExpectedFiles | isExome
        4              | false
        5              | true
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
                jobLog: CreateFileHelper.createFile(temporaryFolder.newFile()).absolutePath,
        ])

        job.roddyService = Mock(RoddyService) {
            1 * getJobStateLogFile(_) >> CreateJobStateLogFileHelper.createJobStateLogFile(temporaryFolder.root, [
                    CreateJobStateLogFileHelper.createJobStateLogFileEntry([clusterJobId: testJobId, statusCode: STATUS_CODE_FINISHED]),
            ])
        }
        job.logService = Mock(LogService)
        job.workflowStepService = Mock(WorkflowStepService) {
            1 * getPreviousRunningWorkflowStep(workflowStepCurrent) >> workflowStep
        }

        when:
        job.ensureExternalJobsRunThrough(workflowStepCurrent)

        then:
        noExceptionThrown()
    }

    void "test ensureExternalJobsRunThrough, when not all jobs run through, then throw a ValidationJobFailedException"() {
        given:
        final PanCancerValidationJob job = new PanCancerValidationJob()
        final WorkflowStep workflowStepCurrent = createWorkflowStep([previous: workflowStep])

        final String testJobId = "cluster_job_id"
        createClusterJob([
                workflowStep: workflowStep,
                clusterJobId: testJobId,
                checkStatus : ClusterJob.CheckStatus.FINISHED,
                jobLog: CreateFileHelper.createFile(temporaryFolder.newFile()).absolutePath,
        ])

        job.roddyService = Mock(RoddyService) {
            1 * getJobStateLogFile(_) >> CreateJobStateLogFileHelper.createJobStateLogFile(temporaryFolder.root, [
                    CreateJobStateLogFileHelper.createJobStateLogFileEntry([clusterJobId: testJobId, statusCode: STATUS_CODE_FAILED]),
            ])
        }
        job.logService = Mock(LogService)
        job.workflowStepService = Mock(WorkflowStepService) {
            1 * getPreviousRunningWorkflowStep(workflowStepCurrent) >> workflowStep
        }

        when:
        job.ensureExternalJobsRunThrough(workflowStepCurrent)

        then:
        final ValidationJobFailedException e = thrown()
        e.message.contains("Status code of cluster job ${testJobId}: " + STATUS_CODE_FAILED)
    }
}
