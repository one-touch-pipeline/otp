package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.*
import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.sophia.*
import grails.converters.*

import java.text.*

class SophiaController {

    ProjectService projectService
    AnalysisService analysisService
    ProjectSelectionService projectSelectionService

    Map results() {
        if (params.projectName) {
            Project project
            if ((project =  projectService.getProjectByName(params.projectName))) {
                projectSelectionService.setSelectedProject([project], project.name)
                redirect(controller: controllerName, action: actionName)
                return
            }
        }

        List<Project> projects = projectService.getAllProjects()
        ProjectSelection selection = projectSelectionService.getSelectedProject()

        Project project
        if (selection.projects.size() == 1) {
            project = selection.projects.first()
        } else {
            project = projects.first()
        }

        return [
                projects: projects,
                project: project,
        ]
    }

    JSON dataTableResults(ResultTableCommand cmd) {
        Map dataToRender = cmd.dataToRender()
        SimpleDateFormat sdf = new SimpleDateFormat('yyyy-MM-dd hh:mm')
        List results = analysisService.getCallingInstancesForProject(SophiaInstance, cmd.project.name)
        List data = results.collect { Map properties ->
            properties.dateCreated = sdf.format(properties.dateCreated)
            if (properties.sophiaProcessingState != AnalysisProcessingStates.FINISHED) {
                properties.remove('sophiaInstanceId')
            }
            return properties
        }

        dataToRender.iTotalRecords = data.size()
        dataToRender.iTotalDisplayRecords = dataToRender.iTotalRecords
        dataToRender.aaData = data

        render dataToRender as JSON
    }
}
