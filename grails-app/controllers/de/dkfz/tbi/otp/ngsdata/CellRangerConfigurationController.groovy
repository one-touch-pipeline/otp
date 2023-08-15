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

import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.Errors

import de.dkfz.tbi.otp.FlashMessage
import de.dkfz.tbi.otp.dataprocessing.Pipeline
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.dataprocessing.cellRanger.*
import de.dkfz.tbi.otp.ngsdata.referencegenome.ReferenceGenomeIndexService
import de.dkfz.tbi.otp.ngsdata.referencegenome.ToolNameService
import de.dkfz.tbi.otp.project.Project

@PreAuthorize('isFullyAuthenticated()')
class CellRangerConfigurationController extends AbstractConfigureNonRoddyPipelineController {

    CellRangerConfigurationService cellRangerConfigurationService
    ReferenceGenomeIndexService referenceGenomeIndexService
    SeqTypeService seqTypeService
    ToolNameService toolNameService

    static allowedMethods = [
            index                   : "GET",
            updateVersion           : "POST",
            updateAutomaticExecution: "POST",
            createMwp               : "POST",
    ]

    static final List<String> ALLOWED_CELL_TYPE = ["neither", "expected", "enforced"]

    def index(CellRangerSelectionCommand cmd) {
        Project project = projectSelectionService.selectedProject

        SeqType seqType = cellRangerConfigurationService.seqType
        List<SeqType> seqTypes = cellRangerConfigurationService.seqTypes
        Pipeline pipeline = cellRangerConfigurationService.pipeline

        CellRangerConfig config = cellRangerConfigurationService.getWorkflowConfig(project)
        boolean configExists = config && cellRangerConfigurationService.getMergingCriteria(project)

        CellRangerConfigurationService.Samples samples = cellRangerConfigurationService.getSamples(project, cmd.individual, cmd.sampleType)

        List<Individual> allIndividuals = samples.allSamples*.individual.unique()
        List<SampleType> allSampleTypes = samples.allSamples*.sampleType.unique()

        List<Individual> selectedIndividuals = samples.selectedSamples*.individual.unique()
        List<SampleType> selectedSampleTypes = samples.selectedSamples*.sampleType.unique()

        List<CellRangerMergingWorkPackage> mwps = cellRangerConfigurationService.findMergingWorkPackage(samples.selectedSamples, pipeline)

        mwps.sort { a, b ->
            a.individual.pid <=> b.individual.pid ?: a.sampleType.name <=> b.sampleType.name
        }

        ToolName toolName = toolNameService.findToolNameByNameAndType('CELL_RANGER', ToolName.Type.SINGLE_CELL)

        return getModelValues(seqType) + [
                createMwpCmd               : flash.createMwpCmd as CreateMwpCommand,
                updateAutomaticExecutionCmd: flash.updateAutomaticExecutionCmd as UpdateAutomaticExecutionCommand,
                configExists               : configExists,
                config                     : config,
                allIndividuals             : allIndividuals,
                individual                 : cmd.individual,
                allSampleTypes             : allSampleTypes,
                sampleType                 : cmd.sampleType,
                seqType                    : seqType,
                seqTypes                   : seqTypes,
                samples                    : samples.selectedSamples,
                referenceGenomeIndex       : cmd.reference,
                referenceGenomeIndexes     : toolName?.referenceGenomeIndexes,
                selectedIndividuals        : selectedIndividuals,
                selectedSampleTypes        : selectedSampleTypes,
                mwps                       : mwps,
        ]
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
            return redirect(action: "index", params: params)
        }

        Errors errors = cellRangerConfigurationService.configureAutoRun(project, cmd.enableAutoExec, cmd.expectedCellsValue, cmd.enforcedCellsValue,
                cmd.referenceGenomeIndex)
        if (errors) {
            flash.updateAutomaticExecutionCmd = cmd
            flash.message = new FlashMessage(g.message(code: "cellRanger.store.failure") as String, errors)
        } else {
            flash.message = new FlashMessage(g.message(code: "cellRanger.store.success") as String)
        }
        redirect(action: "index", params: params)
    }

    def createMwp(CreateMwpCommand cmd) {
        Map<String, Object> params = [:]
        params.put("individual.id", cmd.individual?.id ?: "")
        params.put("sampleType.id", cmd.sampleType?.id ?: "")

        if (!cmd.validate()) {
            flash.createMwpCmd = cmd
            flash.message = new FlashMessage(g.message(code: "cellRanger.store.failure") as String, cmd.errors)
            return redirect(action: "index", params: params)
        }
        Errors errors = cellRangerConfigurationService.prepareCellRangerExecution(
                cmd.expectedCellsValue,
                cmd.enforcedCellsValue,
                cmd.referenceGenomeIndex,
                projectSelectionService.requestedProject,
                cmd.individual,
                cmd.sampleType,
                cmd.seqType,
        )
        if (errors) {
            flash.createMwpCmd = cmd
            flash.message = new FlashMessage(g.message(code: "cellRanger.store.failure") as String, errors)
        } else {
            flash.message = new FlashMessage(g.message(code: "cellRanger.store.success") as String)
        }
        redirect(action: "index", params: params)
    }

    @Override
    protected Pipeline getPipeline() {
        return Pipeline.Name.CELL_RANGER.pipeline
    }

    @SuppressWarnings('MissingOverrideAnnotation') // for an unknown reason the groovy compiler doesnt work with @Override in this case
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
    Individual individual
    SampleType sampleType
    ReferenceGenomeIndex reference

    static constraints = {
        individual nullable: true
        sampleType nullable: true
        seqType nullable: true
        reference nullable: true
    }
}

class CreateMwpCommand extends CellRangerSelectionCommand {
    Integer expectedCellsValue
    Integer enforcedCellsValue
    ReferenceGenomeIndex referenceGenomeIndex

    static constraints = {
        expectedCellsValue nullable: true
        enforcedCellsValue nullable: true
    }
}
