/*
 * Copyright 2011-2023 The OTP authors
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

import grails.converters.JSON
import grails.databinding.BindUsing
import grails.databinding.SimpleMapDataBindingSource
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.Errors

import de.dkfz.tbi.otp.CheckAndCall
import de.dkfz.tbi.otp.FlashMessage
import de.dkfz.tbi.otp.dataprocessing.Pipeline
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.dataprocessing.cellRanger.*
import de.dkfz.tbi.otp.ngsdata.referencegenome.ReferenceGenomeIndexService
import de.dkfz.tbi.otp.ngsdata.referencegenome.ToolNameService
import de.dkfz.tbi.otp.project.Project

@PreAuthorize('isFullyAuthenticated()')
class CellRangerConfigurationController extends AbstractConfigureNonRoddyPipelineController implements CheckAndCall {

    CellRangerConfigurationService cellRangerConfigurationService
    ReferenceGenomeIndexService referenceGenomeIndexService
    SeqTypeService seqTypeService
    ToolNameService toolNameService

    static allowedMethods = [
            index                                : "GET",
            updateVersion                        : "POST",
            updateAutomaticExecution             : "POST",
            createMwp                            : "POST",
            getIndividualsAndSampleTypesBySeqType: "POST",
    ]

    static final List<String> ALLOWED_CELL_TYPE = ["neither", "expected", "enforced"]

    def index() {
        Project project = projectSelectionService.selectedProject

        SeqType seqType = cellRangerConfigurationService.seqType
        List<SeqType> seqTypes = cellRangerConfigurationService.seqTypes

        CellRangerConfig config = cellRangerConfigurationService.getWorkflowConfig(project)
        boolean configExists = config && cellRangerConfigurationService.getMergingCriteria(project)

        List<Sample> samples = cellRangerConfigurationService.getAllSamples(project, [], [])

        List<Individual> allIndividuals = samples*.individual.unique()
        List<SampleType> allSampleTypes = samples*.sampleType.unique()

        ToolName toolName = toolNameService.findToolNameByNameAndType('CELL_RANGER', ToolName.Type.SINGLE_CELL)

        return [
                createMwpCmd               : flash.createMwpCmd as CreateMwpCommand,
                updateAutomaticExecutionCmd: flash.updateAutomaticExecutionCmd as UpdateAutomaticExecutionCommand,
                configExists               : configExists,
                config                     : config,
                allIndividuals             : allIndividuals,
                allSampleTypes             : allSampleTypes,
                seqTypes                   : seqTypes,
                referenceGenomeIndexes     : toolName?.referenceGenomeIndexes,
        ] + getModelValues(seqType)
    }

    JSON getIndividualsAndSampleTypesBySeqType(CellRangerSelectionCommand cmd) {
        Pipeline pipeline = cellRangerConfigurationService.pipeline
        List<Sample> samples = cellRangerConfigurationService.getAllSamples(cmd.selectedProject, cmd.individuals, cmd.sampleTypes)

        List<CellRangerMergingWorkPackage> mwps = cellRangerConfigurationService.findMergingWorkPackage(samples, pipeline)

        mwps.sort { a, b ->
            a.individual.pid <=> b.individual.pid ?: a.sampleType.name <=> b.sampleType.name
        }

        return render([
                samples: samples.collect { sample ->
                    [
                            sampleId  : sample.id,
                            individual: sample.individual.toString(),
                            sampleType: sample.sampleType.toString(),
                            seqType   : cellRangerConfigurationService.seqType.displayNameWithLibraryLayout,
                    ]
                },
                mwps   : mwps.collect { mwp ->
                    [
                            individual            : mwp.individual.toString(),
                            sampleType            : mwp.sampleType.toString(),
                            seqType               : mwp.seqType.displayNameWithLibraryLayout,
                            config                : mwp.config.programVersion,
                            referenceGenome       : mwp.referenceGenome.toString(),
                            referenceGenomeIndex  : mwp.referenceGenomeIndex.toolWithVersion,
                            expectedCells         : mwp.expectedCells,
                            enforcedCells         : mwp.enforcedCells,
                            bamFileInProjectFolder: mwp.bamFileInProjectFolder?.fileOperationStatus?.toString() ?: "N/A",
                    ]
                },
        ] as JSON)
    }

    def updateVersion(ConfigureCellRangerSubmitCommand cmd) {
        updatePipeline(
                projectService.createOrUpdateCellRangerConfig(projectSelectionService.requestedProject,
                        cmd.seqType,
                        cmd.programVersion,
                        cmd.referenceGenomeIndex),
                cmd.seqType,
                cmd.overviewController,
        )
    }

    def updateAutomaticExecution(UpdateAutomaticExecutionCommand cmd) {
        Project project = projectSelectionService.requestedProject

        if (!cmd.validate()) {
            flash.updateAutomaticExecutionCmd = cmd
            flash.message = new FlashMessage(g.message(code: "cellRanger.store.failure") as String, cmd.errors)
            return defaultIndexRedirect()
        }

        Errors errors = cellRangerConfigurationService.configureAutoRun(project, cmd.enableAutoExec, cmd.expectedCellsValue, cmd.enforcedCellsValue,
                cmd.referenceGenomeIndex)
        if (errors) {
            flash.updateAutomaticExecutionCmd = cmd
            flash.message = new FlashMessage(g.message(code: "cellRanger.store.failure") as String, errors)
        } else {
            flash.message = new FlashMessage(g.message(code: "cellRanger.store.success") as String)
        }
        return defaultIndexRedirect()
    }

    JSON createMwp(CreateMwpCommand cmd) {
        return checkErrorAndCallMethodReturns(cmd) {
            cellRangerConfigurationService.prepareCellRangerExecution(
                    cmd.samples,
                    cmd.expectedCellsValue,
                    cmd.enforcedCellsValue,
                    cmd.referenceGenomeIndex,
            )
            return [:] as JSON
        } as JSON
    }

    private void defaultIndexRedirect() {
        redirect(action: "index", params: [
                "individuals"            : params.individuals,
                "sampleTypes"            : params.sampleTypes,
                "referenceGenomeIndex.id": params["referenceGenomeIndex.id"],
        ])
    }

    @Override
    protected Pipeline getPipeline() {
        return Pipeline.Name.CELL_RANGER.pipeline
    }

    @SuppressWarnings('MissingOverrideAnnotation')
    // for an unknown reason the groovy compiler doesnt work with @Override in this case
    protected CellRangerConfig getLatestConfig(Project project, SeqType seqType) {
        return projectService.getLatestCellRangerConfig(project, seqType)
    }

    @Override
    protected String getDefaultVersion() {
        return processingOptionService.findOptionAsString(ProcessingOption.OptionName.PIPELINE_CELLRANGER_DEFAULT_VERSION)
    }

    @Override
    protected List<String> getAvailableVersions() {
        return processingOptionService.findOptionAsList(ProcessingOption.OptionName.PIPELINE_CELLRANGER_AVAILABLE_VERSIONS)
    }
}

class UpdateAutomaticExecutionCommand {
    boolean enableAutoExec
    Integer expectedCellsValue
    Integer enforcedCellsValue
    ReferenceGenomeIndex referenceGenomeIndex

    static constraints = {
        expectedCellsValue nullable: true
        enforcedCellsValue nullable: true
    }
}

class CellRangerSelectionCommand extends BaseConfigurePipelineSubmitCommand {
    Project selectedProject
    // BindUsing is needed, because one value is not send as array but multiple values are sent as array from frontend.
    @BindUsing({ CellRangerSelectionCommand obj, SimpleMapDataBindingSource source ->
        return Individual.getAll(source['individuals']) as List<Individual>
    })
    List<Individual> individuals

    // BindUsing is needed, because one value is not send as array but multiple values are sent as array from frontend.
    @BindUsing({ CellRangerSelectionCommand obj, SimpleMapDataBindingSource source ->
        return SampleType.getAll(source['sampleTypes']) as List<SampleType>
    })
    List<SampleType> sampleTypes

    static constraints = {
        individuals nullable: true
        sampleTypes nullable: true
        seqType nullable: true
        selectedProject nullable: false
    }
}

class CreateMwpCommand extends BaseConfigurePipelineSubmitCommand {
    Integer expectedCellsValue
    Integer enforcedCellsValue

    // BindUsing is needed, because one value is not send as array but multiple values are sent as array from frontend.
    @BindUsing({ CreateMwpCommand obj, SimpleMapDataBindingSource source ->
        return Sample.getAll(source['samples']) as List<Sample>
    })
    List<Sample> samples
    ReferenceGenomeIndex referenceGenomeIndex

    static constraints = {
        samples nullable: false
        expectedCellsValue nullable: true
        enforcedCellsValue nullable: true
        referenceGenomeIndex nullable: false
    }
}
