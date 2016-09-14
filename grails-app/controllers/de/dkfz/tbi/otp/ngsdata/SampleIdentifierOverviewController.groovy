package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.utils.DataTableCommand
import grails.converters.JSON

class SampleIdentifierOverviewController {

    ProjectService projectService
    ProjectOverviewService projectOverviewService

    Map index() {
        String projectName = params.projectName
        return [projects: projectService.getAllProjects()*.name, project: projectName]
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
