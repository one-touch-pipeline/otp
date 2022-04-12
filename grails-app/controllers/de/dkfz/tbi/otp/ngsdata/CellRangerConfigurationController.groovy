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

import grails.plugin.springsecurity.annotation.Secured
import org.springframework.validation.Errors

import de.dkfz.tbi.otp.FlashMessage
import de.dkfz.tbi.otp.dataprocessing.Pipeline
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.dataprocessing.cellRanger.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.CollectionUtils

@Secured('isFullyAuthenticated()')
class CellRangerConfigurationController extends AbstractConfigureNonRoddyPipelineController {

    CellRangerConfigurationService cellRangerConfigurationService
    SeqTypeService seqTypeService

    static allowedMethods = [
            index        : "GET",
            updateVersion: "POST",
            createMwp    : "POST",
    ]

    static final List<String> ALLOWED_CELL_TYPE = ["neither", "expected", "enforced"]

    def index(CellRangerSelectionCommand cmd) {
        Project project = projectSelectionService.selectedProject

        SeqType seqType = cellRangerConfigurationService.seqType
        List<SeqType> seqTypes = cellRangerConfigurationService.seqTypes
        Pipeline pipeline = cellRangerConfigurationService.pipeline

        boolean configExists = cellRangerConfigurationService.getWorkflowConfig(project) && cellRangerConfigurationService.getMergingCriteria(project)

        CellRangerConfigurationService.Samples samples = cellRangerConfigurationService.getSamples(project, cmd.individual, cmd.sampleType)

        List<Individual> allIndividuals = samples.allSamples*.individual.unique()
        List<SampleType> allSampleTypes = samples.allSamples*.sampleType.unique()

        List<Individual> selectedIndividuals = samples.selectedSamples*.individual.unique()
        List<SampleType> selectedSampleTypes = samples.selectedSamples*.sampleType.unique()

        List<CellRangerMergingWorkPackage> mwps = samples.selectedSamples ?
                CellRangerMergingWorkPackage.findAllBySampleInListAndPipeline(samples.selectedSamples, pipeline) : []

        mwps.sort { a, b ->
            a.individual.pid <=> b.individual.pid ?: a.sampleType.name <=> b.sampleType.name
        }

        ToolName toolName = CollectionUtils.atMostOneElement(ToolName.findAllByNameAndType('CELL_RANGER', ToolName.Type.SINGLE_CELL))

        return getModelValues(seqType) + [
                cmd                   : flash.cmd as CellRangerConfigurationCommand,
                configExists          : configExists,
                allIndividuals        : allIndividuals,
                individual            : cmd.individual,
                allSampleTypes        : allSampleTypes,
                sampleType            : cmd.sampleType,
                seqType               : seqType,
                seqTypes              : seqTypes,
                samples               : samples.selectedSamples,
                referenceGenomeIndex  : cmd.reference,
                referenceGenomeIndexes: toolName?.referenceGenomeIndexes,
                selectedIndividuals   : selectedIndividuals,
                selectedSampleTypes   : selectedSampleTypes,
                mwps                  : mwps,
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

    def createMwp(CellRangerConfigurationCommand cmd) {
        Map<String, Object> params = [:]
        params.put("individual.id", cmd.individual?.id ?: "")
        params.put("sampleType.id", cmd.sampleType?.id ?: "")

        if (!cmd.validate()) {
            flash.cmd = cmd
            flash.message = new FlashMessage(g.message(code: "cellRanger.store.failure") as String, cmd.errors)
            redirect(action: "index", params: params)
            return
        }
        Integer expectedCells, enforcedCells
        switch (cmd.expectedOrEnforcedCells) {
            case "expected":
                expectedCells = cmd.expectedOrEnforcedCellsValue as Integer
                break
            case "enforced":
                enforcedCells = cmd.expectedOrEnforcedCellsValue as Integer
                break
            case "neither":
                break
            default:
                throw new UnsupportedOperationException("expectedOrEnforcedCells must be one of ${ALLOWED_CELL_TYPE}")
        }
        Errors errors = cellRangerConfigurationService.prepareCellRangerExecution(
                expectedCells,
                enforcedCells,
                cmd.referenceGenomeIndex,
                projectSelectionService.requestedProject,
                cmd.individual,
                cmd.sampleType,
                cmd.seqType,
        )
        if (errors) {
            flash.cmd = cmd
            flash.message = new FlashMessage(g.message(code: "cellRanger.store.failure") as String, errors)
        } else {
            flash.message = new FlashMessage(g.message(code: "cellRanger.store.success") as String)
        }
        redirect(action: "index", params: params)
    }

    @Override
    protected Pipeline getPipeline() {
        Pipeline.Name.CELL_RANGER.pipeline
    }

    @SuppressWarnings('MissingOverrideAnnotation') //for an unknown reason the groovy compiler doesnt work with @Override in this case
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

class CellRangerConfigurationCommand extends CellRangerSelectionCommand {
    String expectedOrEnforcedCells
    String expectedOrEnforcedCellsValue
    ReferenceGenomeIndex referenceGenomeIndex

    static constraints = {
        expectedOrEnforcedCellsValue nullable: true, validator: { val, obj ->
            if (val != null) {
                if (!val.isInteger()) {
                    return "validator.not.an.integer"
                } else if ((val as int) < 0) {
                    return 'validator.greater.or.equal.zero'
                }
            }
        }
    }
}
