package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.*
import de.dkfz.tbi.otp.utils.*
import grails.converters.*


class SampleIdentifierOverviewController {

    ProjectService projectService
    ProjectOverviewService projectOverviewService
    ProjectSelectionService projectSelectionService

    Map index() {
        List<Project> projects = projectService.getAllProjects()
        ProjectSelection selection = projectSelectionService.getSelectedProject()

        Project project = projectSelectionService.getProjectFromProjectSelectionOrAllProjects(selection)

        return [
                projects: projects,
                project: project,
        ]
    }

    JSON dataTableSourceListSampleIdentifierByProject(DataTableCommand cmd) {
        Map dataToRender = cmd.dataToRender()
        Project project = projectService.getProjectByName(params.project)
        Map data = projectOverviewService.listSampleIdentifierByProject(project)

        dataToRender.iTotalRecords = data.size()
        dataToRender.iTotalDisplayRecords = dataToRender.iTotalRecords
        dataToRender.aaData = []

        data.each { individualMap ->
            individualMap.value.each { sampleMap ->
                List line = [
                        individualMap.key,
                        sampleMap.key,
                        sampleMap.value.collect { it[2] }.join(", "),
                ]
                dataToRender.aaData << line
            }
        }
        render dataToRender as JSON
    }

}
