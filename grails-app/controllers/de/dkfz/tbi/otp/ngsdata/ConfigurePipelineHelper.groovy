package de.dkfz.tbi.otp.ngsdata

import org.springframework.validation.FieldError

import de.dkfz.tbi.otp.ProjectSelectionService
import de.dkfz.tbi.otp.dataprocessing.Pipeline
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.job.processing.FileSystemService
import de.dkfz.tbi.otp.utils.CollectionUtils

import java.nio.file.FileSystem

trait ConfigurePipelineHelper {

    ProjectSelectionService projectSelectionService
    FileSystemService fileSystemService

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
        String configVersion = CollectionUtils.atMostOneElement(
                RoddyWorkflowConfig.findAllByProjectAndSeqTypeAndPipelineAndIndividualIsNull(project, seqType, pipeline, [sort: 'id', order: 'desc', max: 1]))?.configVersion
        if (configVersion) {
            Set<String> versions = configVersion.split("_")
            final int MAIN_CONFIG_VERSION_INDEX = 0
            final int SUB_CONFIG_VERSION_INDEX = 1
            configVersion = versions[MAIN_CONFIG_VERSION_INDEX] + "_" + (versions[SUB_CONFIG_VERSION_INDEX].toInteger() + 1)
        } else {
            configVersion = "v1_0"
        }

        String latestRoddyConfig = ""
        RoddyWorkflowConfig config = RoddyWorkflowConfig.getLatestForProject(project, seqType, pipeline)
        if (config) {
            FileSystem fs = fileSystemService.getFilesystemForConfigFileChecksForRealm(project.realm)
            latestRoddyConfig = fs.getPath(config.configFilePath).text
        }
        return [
                project        : project,
                seqType        : seqType,
                config         : configVersion,
                lastRoddyConfig: latestRoddyConfig,
        ]
    }
}
