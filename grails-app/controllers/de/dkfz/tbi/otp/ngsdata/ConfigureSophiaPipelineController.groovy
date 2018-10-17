package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.dataprocessing.*

class ConfigureSophiaPipelineController extends AbstractConfigureRoddyPipelineController {

    @Override
    Pipeline getPipeline() {
        Pipeline.Name.RODDY_SOPHIA.pipeline
    }

    @SuppressWarnings('MissingOverrideAnnotation') //for an unknown reason the groovy compiler doesnt work with @Override in this case
    String getDefaultPluginName(String roddyName) {
        return processingOptionService.findOptionAsString(ProcessingOption.OptionName.PIPELINE_SOPHIA_DEFAULT_PLUGIN_NAME, roddyName)
    }

    @SuppressWarnings('MissingOverrideAnnotation') //for an unknown reason the groovy compiler doesnt work with @Override in this case
    String getDefaultPluginVersion(String roddyName) {
        return processingOptionService.findOptionAsString(ProcessingOption.OptionName.PIPELINE_SOPHIA_DEFAULT_PLUGIN_VERSIONS, roddyName)
    }

    @SuppressWarnings('MissingOverrideAnnotation') //for an unknown reason the groovy compiler doesnt work with @Override in this case
    String getDefaultBaseProjectConfig(String roddyName) {
        return processingOptionService.findOptionAsString(ProcessingOption.OptionName.PIPELINE_SOPHIA_DEFAULT_BASE_PROJECT_CONFIG, roddyName)
    }

    @Override
    void configure(RoddyConfiguration configuration) {
        projectService.configureSophiaPipelineProject(configuration)
    }
}
