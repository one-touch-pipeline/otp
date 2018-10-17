package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.dataprocessing.*

class ConfigureRunYapsaPipelineController extends AbstractConfigureNonRoddyPipelineController {

    def update(ConfigureRunYapsaSubmitCommand cmd) {
        updatePipeline(projectService.createOrUpdateRunYapsaConfig(cmd.project, cmd.seqType, cmd.programVersion), cmd.project, cmd.seqType)
    }

    @Override
    Pipeline getPipeline() {
        Pipeline.Name.RUN_YAPSA.pipeline
    }

    @SuppressWarnings('MissingOverrideAnnotation') //for an unknown reason the groovy compiler doesnt work with @Override in this case
    WithProgramVersion getLatestConfig(Project project, SeqType seqType) {
        return projectService.getLatestRunYapsaConfig(project, seqType)
    }

    @Override
    String getDefaultVersion() {
        return processingOptionService.findOptionAsString(ProcessingOption.OptionName.PIPELINE_RUNYAPSA_DEFAULT_VERSION)
    }

    @Override
    List<String> getAvailableVersions() {
        return processingOptionService.findOptionAsList(ProcessingOption.OptionName.PIPELINE_RUNYAPSA_AVAILABLE_VERSIONS)
    }
}
