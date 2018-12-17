package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.dataprocessing.Pipeline
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption

class ConfigureSnvPipelineController extends AbstractConfigureRoddyPipelineController {

    @Override
    Pipeline getPipeline() {
        Pipeline.Name.RODDY_SNV.pipeline
    }

    @SuppressWarnings('MissingOverrideAnnotation') //for an unknown reason the groovy compiler doesnt work with @Override in this case
    String getDefaultPluginName(String roddyName) {
        return processingOptionService.findOptionAsString(ProcessingOption.OptionName.PIPELINE_RODDY_SNV_DEFAULT_PLUGIN_NAME, roddyName)
    }

    @SuppressWarnings('MissingOverrideAnnotation') //for an unknown reason the groovy compiler doesnt work with @Override in this case
    String getDefaultPluginVersion(String roddyName) {
        return processingOptionService.findOptionAsString(ProcessingOption.OptionName.PIPELINE_RODDY_SNV_DEFAULT_PLUGIN_VERSION, roddyName)
    }

    @SuppressWarnings('MissingOverrideAnnotation') //for an unknown reason the groovy compiler doesnt work with @Override in this case
    String getDefaultBaseProjectConfig(String roddyName) {
        return processingOptionService.findOptionAsString(ProcessingOption.OptionName.PIPELINE_RODDY_SNV_DEFAULT_BASE_PROJECT_CONFIG, roddyName)
    }

    @Override
    void configure(RoddyConfiguration configuration) {
        projectService.configureSnvPipelineProject(configuration)
    }
}
