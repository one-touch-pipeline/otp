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

import org.springframework.validation.FieldError

import de.dkfz.tbi.otp.ProjectSelectionService
import de.dkfz.tbi.otp.dataprocessing.Pipeline
import de.dkfz.tbi.otp.dataprocessing.WorkflowConfigService
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.job.processing.FileSystemService
import de.dkfz.tbi.otp.utils.CollectionUtils

import java.nio.file.FileSystem

trait ConfigurePipelineHelper {

    ProjectSelectionService projectSelectionService
    FileSystemService fileSystemService
    WorkflowConfigService workflowConfigService

    Map checkErrorsIfSubmitted(ConfigurePipelineSubmitCommand cmd, Pipeline pipeline) {
        boolean hasErrors = false
        String message = ""
        if (cmd.submit) {
            hasErrors = cmd.hasErrors()
            boolean duplicateConfigVersion = false
            RoddyWorkflowConfig.findAllWhere([
                    project      : cmd.project,
                    seqType      : cmd.seqType,
                    pipeline     : pipeline,
                    pluginVersion: "${cmd.pluginName}:${cmd.pluginVersion}",
            ]).each {
                if (it.configVersion == cmd.config) {
                    duplicateConfigVersion = true
                }
            }
            if (hasErrors) {
                FieldError errors = cmd.errors.getFieldError()
                message = "'${errors.getRejectedValue()}' is not a valid value for '${errors.getField()}'." +
                        "Error code: '${errors.code}"
            } else if (duplicateConfigVersion) {
                hasErrors = true
                message = "'${cmd.config}' is not a valid value for 'Config Version'. Error code: 'duplicate'"
            } else {
                return [:]
            }
            return [
                    message          : message,
                    hasErrors        : hasErrors,
                    pluginName       : cmd.pluginName,
                    pluginVersion    : cmd.pluginVersion,
                    baseProjectConfig: cmd.baseProjectConfig,
                    config           : cmd.config,
            ]
        }
        return [
                message  : message,
                hasErrors: hasErrors,
        ]
    }

    Map getValues(Project project, SeqType seqType, Pipeline pipeline) {
        List<RoddyWorkflowConfig> latestConfig = RoddyWorkflowConfig.findAllByProjectAndSeqTypeAndPipelineAndIndividualIsNull(
                project, seqType, pipeline, [sort: 'id', order: 'desc', max: 1]
        )
        String configVersion = workflowConfigService.getNextConfigVersion(CollectionUtils.atMostOneElement(latestConfig)?.configVersion)

        String latestRoddyConfig = ""
        RoddyWorkflowConfig config = RoddyWorkflowConfig.getLatestForProject(project, seqType, pipeline)
        if (config) {
            FileSystem fs = fileSystemService.getFilesystemForConfigFileChecksForRealm(project.realm)
            latestRoddyConfig = fs.getPath(config.configFilePath).text
        }
        return [
                project          : project,
                seqType          : seqType,
                pipeline         : pipeline,
                nextConfigVersion: configVersion,
                lastRoddyConfig  : latestRoddyConfig,
        ]
    }
}
