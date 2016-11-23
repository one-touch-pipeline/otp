package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.roddy.*
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.*
import de.dkfz.tbi.otp.utils.*
import org.springframework.validation.*

class ConfigurePipelineController {

    ProjectService projectService

    Map index(ConfigurePipelineSubmitCommand cmd) {
        Project project
        SeqType seqType
        if (params.projectName && params.seqTypeName && params.libraryLayout) {
            project = projectService.getProjectByName(params.projectName)
            seqType = CollectionUtils.atMostOneElement(SeqType.findAllByNameAndLibraryLayout(params.seqTypeName, params.libraryLayout))
        } else {
            response.sendError(404)
            return
        }
        if (!project || !seqType) {
            response.sendError(404)
            return
        }

        Pipeline pipeline = CollectionUtils.exactlyOneElement(Pipeline.findAllByTypeAndName(
                Pipeline.Type.SNV,
                Pipeline.Name.RODDY_SNV,
        ))

        boolean hasErrors
        String message = ""

        if (cmd.submit == "Submit") {
            hasErrors = cmd.hasErrors()
            boolean duplicateConfigVersion = false
            RoddyWorkflowConfig.findAllWhere([
                    project      : project,
                    seqType      : seqType,
                    pipeline     : pipeline,
                    pluginVersion: "${cmd.pluginName}:${cmd.pluginVersion}"

            ]).each({
                if (it.configVersion == cmd.config) {
                    duplicateConfigVersion = true
                }
            })
            if (hasErrors) {
                FieldError errors = cmd.errors.getFieldError()
                message = "'" + errors.getRejectedValue() + "' is not a valid value for '" + errors.getField() + "'. Error code: '" + errors.code + "'"
            } else if (duplicateConfigVersion) {
                hasErrors = true
                message = "'${cmd.config}' is not a valid value for 'Config Version'. Error code: 'duplicate'"
            } else {
                SnvPipelineConfiguration snvPipelineConfiguration = new SnvPipelineConfiguration([
                        project          : project,
                        seqType          : seqType,
                        pluginName       : cmd.pluginName,
                        pluginVersion    : cmd.pluginVersion,
                        baseProjectConfig: cmd.baseProjectConfig,
                        configVersion    : cmd.config,
                ])

                projectService.configureSnvPipelineProject(snvPipelineConfiguration)
                message = "Successfully changed SNV Pipeline Configuration"
                redirect(controller: "projectOverview", action: "specificOverview", params: [project: project.name])
            }
        }

        String defaultPluginName = ProcessingOptionService.findOption(RoddyConstants.OPTION_KEY_SNV_PIPELINE_PLUGIN_NAME, seqType.roddyName, null)
        String defaultPluginVersion = ProcessingOptionService.findOption(RoddyConstants.OPTION_KEY_SNV_PIPELINE_PLUGIN_VERSION, seqType.roddyName, null)
        String defaultBaseProjectConfig = ProcessingOptionService.findOption(RoddyConstants.OPTION_KEY_SNV_BASE_PROJECT_CONFIG, seqType.roddyName, null)

        RoddyWorkflowConfig roddyWorkflowConfig = RoddyWorkflowConfig.getLatestForProject(project, seqType, pipeline)
        String configVersion = roddyWorkflowConfig?.configVersion
        if (configVersion) {
            Set<String> versions = configVersion.split("_")
            final int MAIN_CONFIG_VERSION_INDEX = 0
            final int SUB_CONFIG_VERSION_INDEX = 1
            configVersion = versions[MAIN_CONFIG_VERSION_INDEX] + "_" + (versions[SUB_CONFIG_VERSION_INDEX].toInteger() + 1)
        } else {
            configVersion = "v1_0"
        }

        String lastRoddyConfig = (roddyWorkflowConfig?.configFilePath as File)?.text

        assert project.getProjectDirectory().exists()

        Map ret = [
                projectName             : project.name,
                seqType                 : seqType,

                pluginName              : defaultPluginName,
                defaultPluginName       : defaultPluginName,

                pluginVersion           : defaultPluginVersion,
                defaultPluginVersion    : defaultPluginVersion,

                baseProjectConfig       : defaultBaseProjectConfig,
                defaultBaseProjectConfig: defaultBaseProjectConfig,

                config                  : configVersion,
                lastRoddyConfig         : lastRoddyConfig,

                message                 : message,
                hasErrors               : hasErrors,
        ]

        if (cmd.submit == "Submit") {
            ret += [
                    pluginName       : cmd.pluginName,
                    pluginVersion    : cmd.pluginVersion,
                    baseProjectConfig: cmd.baseProjectConfig,
                    config           : cmd.config,
            ]
        }
        return ret
    }
}


class ConfigurePipelineSubmitCommand implements Serializable {
    String pluginName
    String pluginVersion
    String baseProjectConfig
    String config
    String submit

    static constraints = {
        pluginName(nullable: true, validator: { val, obj ->
            if (!val) {
                return 'Empty'
            }
            if (!(OtpPath.isValidPathComponent(val))) {
                return 'Invalid path component'
            }
        })
        pluginVersion(nullable: true, validator: { val, obj ->
            if (!val) {
                return 'Empty'
            }
            if (!(OtpPath.isValidPathComponent(val))) {
                return 'Invalid path component'
            }
        })
        config(nullable: true, validator: { val, obj ->
            if (!val) {
                return 'Empty'
            }
            if (!(val ==~ /^v\d+_\d+$/)) {
                return "Not a valid config version. Must look like 'v1_0'"
            }
        })
        baseProjectConfig(nullable: false, blank: false, validator: { val, obj ->
            if (val && !OtpPath.isValidPathComponent(val)) {
                return "Invalid path component"
            }
        })
    }

    void setConfig(String config) {
        this.config = config?.trim()?.replaceAll(" +", " ")
    }
}
