package de.dkfz.tbi.otp.ngsdata

import org.springframework.validation.*

abstract class AbstractConfigureRoddyPipelineController extends AbstractConfigurePipelineController implements ConfigurePipelineHelper {

    Map index(ConfigurePipelineSubmitCommand cmd) {
        Map result = checkErrorsIfSubmitted(cmd, pipeline)
        if (!result) {
            RoddyConfiguration configuration = new RoddyConfiguration([
                    project          : cmd.project,
                    seqType          : cmd.seqType,
                    pluginName       : cmd.pluginName,
                    pluginVersion    : cmd.pluginVersion,
                    baseProjectConfig: cmd.baseProjectConfig,
                    configVersion    : cmd.config,
            ])
            configure(configuration)

            flash.message = g.message(code: "configurePipeline.store.success")
            redirect(controller: "projectConfig")
        }

        result << params
        result << getValues(cmd.project, cmd.seqType, getPipeline())

        String pluginName = getDefaultPluginName(cmd.seqType.roddyName)
        String pluginVersion = getDefaultPluginVersion(cmd.seqType.roddyName)
        String baseProjectConfig = getDefaultBaseProjectConfig(cmd.seqType.roddyName)
        result << [
                pipeline                : getPipeline(),

                pluginName              : pluginName,
                defaultPluginName       : pluginName,

                pluginVersion           : pluginVersion,
                defaultPluginVersion    : pluginVersion,

                baseProjectConfig       : baseProjectConfig,
                defaultBaseProjectConfig: baseProjectConfig,
        ]
        return result
    }

    abstract String getDefaultPluginName(String roddyName)
    abstract String getDefaultPluginVersion(String roddyName)
    abstract String getDefaultBaseProjectConfig(String roddyName)
    abstract void configure(RoddyConfiguration configuration)
}
