/*
 * Copyright 2011-2020 The OTP authors
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

import groovy.json.JsonOutput
import org.springframework.beans.factory.annotation.Autowired

import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyResult
import de.dkfz.tbi.otp.infrastructure.ClusterJob
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.ProcessOutput
import de.dkfz.tbi.otp.workflowExecution.WorkflowRunService
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep
import de.dkfz.tbi.otp.workflowExecution.cluster.ClusterJobHandlingService

import java.nio.file.FileSystem
import java.nio.file.Path

abstract class AbstractExecuteRoddyPipelineJob extends AbstractExecutePipelineJob {

    @Autowired
    ClusterJobHandlingService clusterJobHandlingService

    @Autowired
    IndividualService individualService

    @Autowired
    RoddyCommandService roddyCommandService

    @Autowired
    RoddyConfigService roddyConfigService

    @Autowired
    RoddyConfigValueService roddyConfigValueService

    @Autowired
    RoddyExecutionService roddyExecutionService

    @Autowired
    WorkflowRunService workflowRunService

    @Override
    final void execute(WorkflowStep workflowStep) {
        RoddyResult roddyResult = getRoddyResult(workflowStep)
        Realm realm = workflowStep.workflowRun.realm
        FileSystem fs = fileSystemService.getRemoteFileSystem(realm)
        Path outputDir = fs.getPath(roddyResult.workDirectory.absolutePath)
        Path confDir = outputDir.resolve(RoddyConfigService.CONFIGURATION_DIRECTORY)

        logService.addSimpleLogEntry(workflowStep,
                "The json config (without run specific values):\n${JsonOutput.prettyPrint(workflowStep.workflowRun.combinedConfig)}")

        String xmlConfig = roddyConfigService.createRoddyXmlConfig(
                workflowStep.workflowRun.combinedConfig,
                roddyConfigValueService.defaultValues + getConfigurationValues(workflowStep, workflowStep.workflowRun.combinedConfig),
                roddyWorkflowName,
                workflowStep.workflowRun.workflowVersion,
                getAnalysisConfiguration(roddyResult.seqType),
                individualService.getViewByPidPathBase(roddyResult.individual, roddyResult.seqType),
                outputDir,
                workflowStep.workflowRun.priority.queue,
                filenameSectionKillSwitch,
        )
        logService.addSimpleLogEntry(workflowStep, "The final xml:\n${xmlConfig}")

        Path xmlPath = confDir.resolve("${RoddyConfigService.CONFIGURATION_NAME}.xml")
        fileService.createFileWithContent(xmlPath, xmlConfig, realm, FileService.DEFAULT_FILE_PERMISSION, true)

        String command = roddyCommandService.createRoddyCommand(
                roddyResult.individual,
                confDir,
                getAdditionalParameters(workflowStep),
        )
        roddyExecutionService.clearRoddyExecutionStoreDirectory(roddyResult)

        // begin of non-restartable area
        workflowRunService.markJobAsNotRestartableInSeparateTransaction(workflowStep.workflowRun)
        ProcessOutput output = roddyExecutionService.execute(command, realm)
        // end of non-restartable area
        workflowStep.workflowRun.with {
            // done in same transaction in which cluster jobs are saved
            jobCanBeRestarted = true
            save(flush: true)
        }

        Collection<ClusterJob> clusterJobs = roddyExecutionService.createClusterJobObjects(roddyResult, output, workflowStep)
        if (clusterJobs) {
            roddyExecutionService.saveRoddyExecutionStoreDirectory(roddyResult, output.stderr, fs)
            clusterJobHandlingService.collectJobStatistics(workflowStep, clusterJobs)
            clusterJobHandlingService.startMonitorClusterJob(workflowStep, clusterJobs)
            workflowStateChangeService.changeStateToWaitingOnSystem(workflowStep)
        } else {
            workflowStateChangeService.changeStateToSuccess(workflowStep)
        }
    }

    protected abstract RoddyResult getRoddyResult(WorkflowStep workflowStep)

    protected abstract String getRoddyWorkflowName()

    protected abstract String getAnalysisConfiguration(SeqType seqType)

    protected abstract boolean getFilenameSectionKillSwitch()

    protected abstract Map<String, String> getConfigurationValues(WorkflowStep workflowStep, String combinedConfig)

    protected abstract List<String> getAdditionalParameters(WorkflowStep workflowStep)
}
