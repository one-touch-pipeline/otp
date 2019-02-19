package de.dkfz.tbi.otp.dataprocessing.cellRanger

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
        Pipeline pipeline = cellRangerConfigurationService.pipeline

        boolean configExists = cellRangerConfigurationService.getWorkflowConfig(project) && cellRangerConfigurationService.getMergingCriteria(project)

        CellRangerConfigurationService.Samples samples = cellRangerConfigurationService.getSamples(project, cmd.individual, cmd.sampleType)

        List<Individual> allIndividuals = samples.allSamples*.individual.unique()
        List<SampleType> allSampleTypes = samples.allSamples*.sampleType.unique()

        List<Individual> selectedIndividuals = samples.selectedSamples*.individual.unique()
        List<SampleType> selectedSampleTypes = samples.selectedSamples*.sampleType.unique()

        List<CellRangerMergingWorkPackage> mwps = samples.selectedSamples ?
                CellRangerMergingWorkPackage.findAllBySampleInListAndPipeline(samples.selectedSamples, pipeline) : []

        return [
                configExists       : configExists,
                projects           : projects,
                project            : project,
                allIndividuals     : allIndividuals,
                individual         : cmd.individual,
                allSampleTypes     : allSampleTypes,
                sampleType         : cmd.sampleType,
                seqType            : seqType,
                samples            : samples.selectedSamples,
                selectedIndividuals: selectedIndividuals,
                selectedSampleTypes: selectedSampleTypes,
                mwps               : mwps,
        ]
    }

    def create(CellRangerConfigurationCommand cmd) {
        if (!cmd.validate()) {
            flash.message = new FlashMessage(g.message(code: "cellRanger.store.failure") as String, cmd.errors)
            redirect(action: "index")
            return
        }

        Errors errors = cellRangerConfigurationService.createMergingWorkPackage(
                cmd.expectedCells,
                cmd.enforcedCells,
                cmd.project,
                cmd.individual,
                cmd.sampleType
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
    int expectedCells
    Integer enforcedCells
    Project project
}
