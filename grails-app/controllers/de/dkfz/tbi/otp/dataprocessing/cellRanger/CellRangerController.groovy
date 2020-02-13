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

import grails.validation.ValidationException
import groovy.transform.Canonical
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
            finalRunSelection: "GET",
            saveFinalRunSelection: "POST",
    ]

    static final List<String> ALLOWED_CELL_TYPE = ["neither", "expected", "enforced"]

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

        mwps.sort { a, b ->
            a.individual.pid <=> b.individual.pid ?: a.sampleType.name <=> b.sampleType.name
        }

        ToolName toolName = ToolName.findByNameAndType('CELL_RANGER', ToolName.Type.SINGLE_CELL)

        return [
                cmd                   : flash.cmd as CellRangerConfigurationCommand,
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
                referenceGenomeIndex  : cmd.reference,
                referenceGenomeIndexes: toolName.referenceGenomeIndexes,
                selectedIndividuals   : selectedIndividuals,
                selectedSampleTypes   : selectedSampleTypes,
                mwps                  : mwps,
        ]
    }

    def create(CellRangerConfigurationCommand cmd) {
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
            flash.cmd = cmd
            flash.message = new FlashMessage(g.message(code: "cellRanger.store.failure") as String, errors)
        } else {
            flash.message = new FlashMessage(g.message(code: "cellRanger.store.success") as String)
        }
        redirect(action: "index", params: params)
    }

    def finalRunSelection() {
        List<Project> projects = projectService.getAllProjects()
        if (!projects) {
            return [
                    projects: projects,
            ]
        }
        ProjectSelection selection = projectSelectionService.getSelectedProject()
        Project project = projectSelectionService.getProjectFromProjectSelectionOrAllProjects(selection)


        List<CellRangerMergingWorkPackage> mwps = CellRangerMergingWorkPackage.createCriteria().list {
            sample {
                individual {
                    eq("project", project)
                }
            }
        } as List<CellRangerMergingWorkPackage>

        Map<GroupedMwp, List<CellRangerMergingWorkPackage>> grouped = mwps.groupBy({
            new GroupedMwp(it.sample, it.seqType, it.config.programVersion, it.referenceGenomeIndex)
        })

        List<GroupedMwp> groupedMwps = grouped.collect { k, v ->
            k.mwps = v
            return k
        }

        groupedMwps.sort()
        groupedMwps.each { it.mwps.sort() }

        return [
                project    : project,
                projects   : projects,
                groupedMwps: groupedMwps,
        ]
    }

    def saveFinalRunSelection(MwpSelectionCommand cmd) {
        if (!cmd.validate()) {
            flash.message = new FlashMessage(g.message(code: "cellRanger.selection.failure") as String, cmd.errors)
            redirect(action: "finalRunSelection")
            return
        }

        try {
            if (cmd.mwp?.isLong()) {
                cellRangerConfigurationService.selectMwpAsFinal(CellRangerMergingWorkPackage.get(cmd.mwp.toLong()))
            } else if (cmd.mwp == "delete") {
                cellRangerConfigurationService.selectNoneAsFinal(cmd.sample, cmd.seqType, cmd.programVersion, cmd.reference)
            }
            flash.message = new FlashMessage(g.message(code: "cellRanger.selection.success") as String)
        } catch (ValidationException e) {
            flash.message = new FlashMessage(g.message(code: "cellRanger.selection.failure") as String, e.errors)
        } catch (Exception e) {
            flash.message = new FlashMessage(g.message(code: "cellRanger.selection.failure") as String, e.message)
        }

        redirect(action: "finalRunSelection")
    }
}

@Canonical
class GroupedMwp implements Comparable {
    Sample sample
    SeqType seqType
    String programVersion
    ReferenceGenomeIndex reference
    List<CellRangerMergingWorkPackage> mwps

    boolean isAtLeastOneInProgress() {
        mwps.any { !it.bamFileInProjectFolder }
    }

    boolean isAnyUnsetAndNoneFinal() {
        mwps.any { it.status == CellRangerMergingWorkPackage.Status.UNSET } && !mwps.any { it.status == CellRangerMergingWorkPackage.Status.FINAL }
    }

    @Override
    int compareTo(Object o) {
        this.sample.individual.displayName <=> o.sample.individual.displayName ?:
                this.sample.sampleType.displayName <=> o.sample.sampleType.displayName ?:
                        this.seqType.nameWithLibraryLayout <=> o.seqType.nameWithLibraryLayout ?:
                                this.programVersion <=> o.programVersion ?:
                                        this.reference.toString() <=> o.reference.toString()
    }
}

class CellRangerSelectionCommand {
    Individual individual
    SampleType sampleType
    ReferenceGenomeIndex reference

    static constraints = {
        individual nullable: true
        sampleType nullable: true
    }
}

class CellRangerConfigurationCommand extends CellRangerSelectionCommand {
    String expectedOrEnforcedCells
    String expectedOrEnforcedCellsValue
    ReferenceGenomeIndex referenceGenomeIndex
    Project project
    SeqType seqType

    static constraints = {
        expectedOrEnforcedCellsValue nullable: true, validator: { val, obj ->
            if (val != null && !(val ==~ /^[0-9]+$/)) {
                return "validator.not.a.number"
            }
        }
    }
}

class MwpSelectionCommand {
    Sample sample
    SeqType seqType
    String programVersion
    ReferenceGenomeIndex reference

    // we can't use a CellRangerMergingWorkPackage object here because we need to distinguish
    // between "delete all" and no radio button selected
    String mwp
}
