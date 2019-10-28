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
package de.dkfz.tbi.otp.dataprocessing.cellRanger

import grails.databinding.BindUsing
import org.springframework.validation.Errors

import de.dkfz.tbi.otp.*
import de.dkfz.tbi.otp.dataprocessing.Pipeline
import de.dkfz.tbi.otp.ngsdata.*

class CellRangerController {
    CellRangerConfigurationService cellRangerConfigurationService
    ProjectSelectionService projectSelectionService
    ProjectService projectService
    SeqTypeService seqTypeService

    static allowedMethods = [
            index : "GET",
            create: "POST",
    ]

    static final List<String> ALLOWED_CELL_TYPE = ["expected", "enforced"]

    def index(CellRangerSelectionCommand cmd) {
        List<Project> projects = projectService.getAllProjects()
        if (!projects) {
            return [
                    projects: projects,
            ]
        }
        ProjectSelection selection = projectSelectionService.getSelectedProject()
        Project project = projectSelectionService.getProjectFromProjectSelectionOrAllProjects(selection)

        assert cmd.validate()

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

        ToolName toolName = ToolName.findByNameAndType('CELL_RANGER', ToolName.Type.SINGLE_CELL)

        return [
                configExists          : configExists,
                projects              : projects,
                project               : project,
                allIndividuals        : allIndividuals,
                individual            : cmd.individual,
                allSampleTypes        : allSampleTypes,
                sampleType            : cmd.sampleType,
                seqType               : seqType,
                seqTypes              : seqTypes,
                samples               : samples.selectedSamples,
                referenceGenomeIndexes: toolName.referenceGenomeIndexes,
                selectedIndividuals   : selectedIndividuals,
                selectedSampleTypes   : selectedSampleTypes,
                mwps                  : mwps,
        ]
    }

    def create(CellRangerConfigurationCommand cmd) {
        if (!cmd.validate()) {
            flash.message = new FlashMessage(g.message(code: "cellRanger.store.failure") as String, cmd.errors)
            redirect(action: "index")
            return
        }
        Integer expectedCells, enforcedCells
        switch (cmd.expectedOrEnforcedCells) {
            case "expected":
                expectedCells = cmd.expectedOrEnforcedCellsValue
                break
            case "enforced":
                enforcedCells = cmd.expectedOrEnforcedCellsValue
                break
            default:
                throw new UnsupportedOperationException("expectedOrEnforcedCells must be one of ${ALLOWED_CELL_TYPE}")
        }
        Errors errors = cellRangerConfigurationService.createMergingWorkPackage(
                expectedCells,
                enforcedCells,
                cmd.referenceGenomeIndex,
                cmd.project,
                cmd.individual,
                cmd.sampleType,
                cmd.seqType,
        )
        if (errors) {
            flash.message = new FlashMessage(g.message(code: "cellRanger.store.failure") as String, errors)
        } else {
            flash.message = new FlashMessage(g.message(code: "cellRanger.store.success") as String)
        }
        redirect(action: "index")
    }
}

class CellRangerSelectionCommand {
    Individual individual
    SampleType sampleType

    static constraints = {
        individual nullable: true
        sampleType nullable: true
    }
}

class CellRangerConfigurationCommand extends CellRangerSelectionCommand {
    String expectedOrEnforcedCells

    @BindUsing({ obj, source ->
        String value = source['expectedOrEnforcedCellsValue']
        value?.isInteger() ? value.toInteger() : null
    })
    Integer expectedOrEnforcedCellsValue

    ReferenceGenomeIndex referenceGenomeIndex
    Project project
    SeqType seqType
}
