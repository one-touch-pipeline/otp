package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.cellRanger.*

class ConfigureCellRangerPipelineController extends AbstractConfigureNonRoddyPipelineController {

    def update(ConfigureCellRangerSubmitCommand cmd) {
        updatePipeline(projectService.createOrUpdateCellRangerConfig(cmd.project, cmd.seqType, cmd.programVersion, cmd.referenceGenomeIndex),
                cmd.project, cmd.seqType)
    }

    @Override
    Pipeline getPipeline() {
        Pipeline.Name.CELL_RANGER.pipeline
    }

    @SuppressWarnings('MissingOverrideAnnotation') //for an unknown reason the groovy compiler doesnt work with @Override in this case
    CellRangerConfig getLatestConfig(Project project, SeqType seqType) {
        return projectService.getLatestCellRangerConfig(project, seqType)
    }

    @Override
    String getDefaultVersion() {
        return processingOptionService.findOptionAsString(ProcessingOption.OptionName.PIPELINE_CELLRANGER_DEFAULT_VERSION)
    }

    @Override
    List<String> getAvailableVersions() {
        return processingOptionService.findOptionAsList(ProcessingOption.OptionName.PIPELINE_CELLRANGER_AVAILABLE_VERSIONS)
    }

    @Override
    Map getAdditionalProperties(Project project, SeqType seqType) {
        CellRangerConfig config = getLatestConfig(project, seqType)
        ToolName toolName = ToolName.findByNameAndType('CELL_RANGER', ToolName.Type.SINGLE_CELL)

        return [
                referenceGenomeIndex  : config?.referenceGenomeIndex,
                referenceGenomeIndexes: toolName.referenceGenomeIndexes,
        ]
    }
}
