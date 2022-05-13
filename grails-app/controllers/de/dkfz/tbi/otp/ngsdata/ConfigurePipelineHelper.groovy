/*
 * Copyright 2011-2019 The OTP authors
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
package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.ProjectSelectionService
import de.dkfz.tbi.otp.dataprocessing.Pipeline
import de.dkfz.tbi.otp.dataprocessing.WorkflowConfigService
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfigService
import de.dkfz.tbi.otp.project.Project

trait ConfigurePipelineHelper {

    ProjectSelectionService projectSelectionService
    RoddyWorkflowConfigService roddyWorkflowConfigService
    WorkflowConfigService workflowConfigService

    boolean validateUniqueness(ConfigurePipelineSubmitCommand cmd, Project project, Pipeline pipeline) {
        return roddyWorkflowConfigService.findAllByProjectAndSeqTypeAndPipelineAndProgramVersionAndConfigVersion(
                project,
                cmd.seqType,
                pipeline,
                "${cmd.pluginName}:${cmd.programVersion}",
                cmd.config,
        ).empty
    }

    Map getValues(Project project, SeqType seqType, Pipeline pipeline) {
        RoddyWorkflowConfig latestConfig = roddyWorkflowConfigService.findLatestConfigByProjectAndSeqTypeAndPipeline(project, seqType, pipeline)
        String nextConfigVersion = workflowConfigService.getNextConfigVersion(latestConfig?.configVersion)

        RoddyWorkflowConfigService.ConfigState configState = roddyWorkflowConfigService.getCurrentFilesystemState(project, seqType, pipeline)

        return [
                project             : project,
                seqType             : seqType,
                pipeline            : pipeline,
                nextConfigVersion   : nextConfigVersion,
                configState         : configState,
        ]
    }
}
