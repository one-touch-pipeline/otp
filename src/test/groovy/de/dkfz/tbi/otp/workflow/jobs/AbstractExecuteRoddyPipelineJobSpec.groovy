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
package de.dkfz.tbi.otp.workflow.jobs

import grails.testing.gorm.DataTest
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.config.OtpProperty
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.domainFactory.pipelines.IsRoddy
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.infrastructure.ClusterJob
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.ProcessOutput
import de.dkfz.tbi.otp.workflowExecution.*
import de.dkfz.tbi.otp.workflowExecution.cluster.ClusterJobHandlingService

import java.nio.file.Paths

class AbstractExecuteRoddyPipelineJobSpec extends Specification implements DataTest, WorkflowSystemDomainFactory, IsRoddy {

    @Rule
    TemporaryFolder temporaryFolder

    TestConfigService configService

    @Override
    Class[] getDomainClassesToMock() {
        return [
                ActiveProjectWorkflow,
                FastqImportInstance,
                FileType,
                LibraryPreparationKit,
                MergingWorkPackage,
                Pipeline,
                ProcessingOption,
                ReferenceGenomeProjectSeqType,
                RoddyBamFile,
                RoddyWorkflowConfig,
                Sample,
                SampleType,
                WorkflowStep,
        ]
    }

    void cleanup() {
        configService.clean()
    }

    void "test execute, successfully"() {
        given:
        configService = new TestConfigService([(OtpProperty.PATH_PROJECT_ROOT): temporaryFolder.newFolder().path])
        WorkflowStep workflowStep = createWorkflowStep()

        String configText = "<config/>"
        String cmd = "roddy.sh"
        RoddyBamFile bamFile = createBamFile()
        WorkflowVersion workflowVersion = createWorkflowVersion(workflow: workflowStep.workflowRun.workflow)
        ActiveProjectWorkflow activeWorkflow = createActiveProjectWorkflow(workflowVersion: workflowVersion, project: bamFile.project, seqType: bamFile.seqType)
        ProcessOutput processOutput = new ProcessOutput("out", "err", 0)
        List<ClusterJob> clusterJobs = [createClusterJob(), createClusterJob()]

        AbstractExecuteRoddyPipelineJob job = Spy(AbstractExecuteRoddyPipelineJob) {
            1 * getRoddyResult(_) >> { bamFile }
            1 * getRoddyWorkflowName() >> { "workflow-name" }
            1 * getAnalysisConfiguration(_) >> { "analysis-id" }
            1 * getFilenameSectionKillSwitch() >> { true }
            1 * getConfigurationValues(_, _) >> { [a: "b"] }
            1 * getAdditionalParameters(_) >> { ["c", "d"] }
        }
        job.clusterJobHandlingService = Mock(ClusterJobHandlingService)
        job.fileService = Mock(FileService)
        job.fileSystemService = new TestFileSystemService()
        job.roddyCommandService = Mock(RoddyCommandService)
        job.roddyConfigService = Mock(RoddyConfigService)
        job.roddyConfigValueService = Mock(RoddyConfigValueService)
        job.roddyExecutionService = Mock(RoddyExecutionService)
        job.workflowStateChangeService = Mock(WorkflowStateChangeService)
        job.workflowRunService = Mock(WorkflowRunService)

        when:
        job.execute(workflowStep)

        then:
        1 * job.roddyConfigValueService.getDefaultValues() >> { [e: "f"] }
        1 * job.roddyConfigService.createRoddyXmlConfig(_, [e: "f", a: "b"], "workflow-name", activeWorkflow.workflowVersion, "analysis-id", _, _, _,
                true) >> { configText }
        1 * job.fileService.createFileWithContent(Paths.get(bamFile.workDirectory.absolutePath).resolve("config.xml"), configText, _)
        1 * job.roddyCommandService.createRoddyCommand(_, _, ["c", "d"]) >> { cmd }
        1 * job.roddyExecutionService.clearRoddyExecutionStoreDirectory(bamFile)
        1 * job.workflowRunService.markJobAsNotRestartableInSeparateTransaction(workflowStep.workflowRun)
        1 * job.roddyExecutionService.execute(cmd, _) >> { processOutput }
        1 * job.roddyExecutionService.createClusterJobObjects(bamFile, processOutput, workflowStep) >> { clusterJobs }
        1 * job.roddyExecutionService.saveRoddyExecutionStoreDirectory(_, processOutput.stderr, _)
        1 * job.clusterJobHandlingService.collectJobStatistics(workflowStep, clusterJobs)
        1 * job.clusterJobHandlingService.startMonitorClusterJob(workflowStep, clusterJobs)
        1 * job.workflowStateChangeService.changeStateToWaitingOnSystem(workflowStep)
    }

    void "test execute, successfully, without cluster jobs"() {
        given:
        configService = new TestConfigService([(OtpProperty.PATH_PROJECT_ROOT): temporaryFolder.newFolder().path])
        WorkflowStep workflowStep = createWorkflowStep()

        String configText = "<config/>"
        String cmd = "roddy.sh"
        RoddyBamFile bamFile = createBamFile()
        WorkflowVersion workflowVersion = createWorkflowVersion(workflow: workflowStep.workflowRun.workflow)
        ActiveProjectWorkflow activeWorkflow = createActiveProjectWorkflow(workflowVersion: workflowVersion, project: bamFile.project, seqType: bamFile.seqType)
        ProcessOutput processOutput = new ProcessOutput("out", "err", 0)

        AbstractExecuteRoddyPipelineJob job = Spy(AbstractExecuteRoddyPipelineJob) {
            1 * getRoddyResult(_) >> { bamFile }
            1 * getRoddyWorkflowName() >> { "workflow-name" }
            1 * getAnalysisConfiguration(_) >> { "analysis-id" }
            1 * getFilenameSectionKillSwitch() >> { true }
            1 * getConfigurationValues(_, _) >> { [a: "b"] }
            1 * getAdditionalParameters(_) >> { ["c", "d"] }
        }
        job.clusterJobHandlingService = Mock(ClusterJobHandlingService)
        job.fileService = Mock(FileService)
        job.fileSystemService = new TestFileSystemService()
        job.roddyCommandService = Mock(RoddyCommandService)
        job.roddyConfigService = Mock(RoddyConfigService)
        job.roddyConfigValueService = Mock(RoddyConfigValueService)
        job.roddyExecutionService = Mock(RoddyExecutionService)
        job.workflowStateChangeService = Mock(WorkflowStateChangeService)
        job.workflowRunService = Mock(WorkflowRunService)

        when:
        job.execute(workflowStep)

        then:
        1 * job.roddyConfigValueService.getDefaultValues() >> { [e: "f"] }
        1 * job.roddyConfigService.createRoddyXmlConfig(_, [e: "f", a: "b"], "workflow-name", activeWorkflow.workflowVersion, "analysis-id", _, _, _,
                true) >> { configText }
        1 * job.fileService.createFileWithContent(Paths.get(bamFile.workDirectory.absolutePath).resolve("config.xml"), configText, _)
        1 * job.roddyCommandService.createRoddyCommand(_, _, ["c", "d"]) >> { cmd }
        1 * job.roddyExecutionService.clearRoddyExecutionStoreDirectory(bamFile)
        1 * job.workflowRunService.markJobAsNotRestartableInSeparateTransaction(workflowStep.workflowRun)
        1 * job.roddyExecutionService.execute(cmd, _) >> { processOutput }
        1 * job.roddyExecutionService.createClusterJobObjects(bamFile, processOutput, workflowStep) >> { [] }
        0 * job.roddyExecutionService.saveRoddyExecutionStoreDirectory(_, processOutput.stderr, _)
        0 * job.clusterJobHandlingService.collectJobStatistics(workflowStep, _)
        0 * job.clusterJobHandlingService.startMonitorClusterJob(workflowStep, _)
        1 * job.workflowStateChangeService.changeStateToSuccess(workflowStep)
    }

    void "test execute, execution fails"() {
        given:
        configService = new TestConfigService([(OtpProperty.PATH_PROJECT_ROOT): temporaryFolder.newFolder().path])
        WorkflowStep workflowStep = createWorkflowStep()

        String configText = "<config/>"
        String cmd = "roddy.sh"
        RoddyBamFile bamFile = createBamFile()
        WorkflowVersion workflowVersion = createWorkflowVersion(workflow: workflowStep.workflowRun.workflow)
        ActiveProjectWorkflow activeWorkflow = createActiveProjectWorkflow(workflowVersion: workflowVersion, project: bamFile.project, seqType: bamFile.seqType)
        ProcessOutput processOutput = new ProcessOutput("out", "err", 0)
        List<ClusterJob> clusterJobs = [createClusterJob(), createClusterJob()]

        AbstractExecuteRoddyPipelineJob job = Spy(AbstractExecuteRoddyPipelineJob) {
            1 * getRoddyResult(_) >> { bamFile }
            1 * getRoddyWorkflowName() >> { "workflow-name" }
            1 * getAnalysisConfiguration(_) >> { "analysis-id" }
            1 * getFilenameSectionKillSwitch() >> { true }
            1 * getConfigurationValues(_, _) >> { [a: "b"] }
            1 * getAdditionalParameters(_) >> { ["c", "d"] }
        }
        job.clusterJobHandlingService = Mock(ClusterJobHandlingService)
        job.fileService = Mock(FileService)
        job.fileSystemService = new TestFileSystemService()
        job.roddyCommandService = Mock(RoddyCommandService)
        job.roddyConfigService = Mock(RoddyConfigService)
        job.roddyConfigValueService = Mock(RoddyConfigValueService)
        job.roddyExecutionService = Mock(RoddyExecutionService)
        job.workflowStateChangeService = Mock(WorkflowStateChangeService)
        job.workflowRunService = Mock(WorkflowRunService)

        when:
        job.execute(workflowStep)

        then:
        1 * job.roddyConfigValueService.getDefaultValues() >> { [e: "f"] }
        1 * job.roddyConfigService.createRoddyXmlConfig(_, [e: "f", a: "b"], "workflow-name", activeWorkflow.workflowVersion, "analysis-id", _, _, _,
                true) >> { configText }
        1 * job.fileService.createFileWithContent(Paths.get(bamFile.workDirectory.absolutePath).resolve("config.xml"), configText, _)
        1 * job.roddyCommandService.createRoddyCommand(_, _, ["c", "d"]) >> { cmd }
        1 * job.roddyExecutionService.clearRoddyExecutionStoreDirectory(bamFile)
        1 * job.workflowRunService.markJobAsNotRestartableInSeparateTransaction(workflowStep.workflowRun)
        1 * job.roddyExecutionService.execute(cmd, _) >> { throw new RoddyException() }
        0 * job.roddyExecutionService.createClusterJobObjects(bamFile, processOutput, workflowStep) >> { clusterJobs }
        0 * job.roddyExecutionService.saveRoddyExecutionStoreDirectory(_, processOutput.stderr, _)
        0 * job.clusterJobHandlingService.collectJobStatistics(workflowStep, clusterJobs)
        0 * job.clusterJobHandlingService.startMonitorClusterJob(workflowStep, clusterJobs)
        0 * job.workflowStateChangeService.changeStateToWaitingOnSystem(workflowStep)
        0 * job.workflowStateChangeService.changeStateToSuccess(workflowStep)
        thrown(RoddyException)
    }
}
