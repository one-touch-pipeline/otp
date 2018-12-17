package de.dkfz.tbi.otp.ngsdata

import groovy.transform.ToString

import de.dkfz.tbi.otp.ProjectSelection
import de.dkfz.tbi.otp.ProjectSelectionService
import de.dkfz.tbi.otp.dataprocessing.BamFilePairAnalysis
import de.dkfz.tbi.otp.utils.DataTableCommand

abstract class AbstractAnalysisController {

    ProjectService projectService
    ProjectSelectionService projectSelectionService

    Map results() {
        String projectName = params.project ?: params.projectName
        if (projectName) {
            Project project = projectService.getProjectByName(projectName)
            if (project) {
                projectSelectionService.setSelectedProject([project], project.name)
                redirect(controller: controllerName, action: actionName)
                return
            }
        }

        List<Project> projects = projectService.getAllProjects()
        ProjectSelection selection = projectSelectionService.getSelectedProject()

        Project project = projectSelectionService.getProjectFromProjectSelectionOrAllProjects(selection)

        return [
                projects: projects,
                project : project,
        ]
    }

}

@ToString
class BamFilePairAnalysisCommand {
    BamFilePairAnalysis bamFilePairAnalysis
    PlotType plotType
    int index

    static constraints = {
        bamFilePairAnalysis nullable: false
        plotType nullable: false
    }
}

class ResultTableCommand extends DataTableCommand {
    Project project
}
