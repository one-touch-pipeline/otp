package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.utils.DataTableCommand
import grails.converters.JSON
import javax.xml.transform.Transformer
import javax.xml.transform.TransformerFactory
import javax.xml.transform.stream.StreamResult
import javax.xml.transform.stream.StreamSource

class ProjectOverviewController {

    def projectService

    def projectOverviewService

    Map index() {
        String projectName = params.projectName
        return [projects: projectService.getAllProjects()*.name, project: projectName]
    }

    JSON individualCountByProject(String projectName) {
        Project project = projectService.getProjectByName(projectName)
        Map dataToRender = [individualCount: projectOverviewService.individualCountByProject(project)]
        render dataToRender as JSON
    }

    JSON dataTableSource(DataTableCommand cmd) {
        Map dataToRender = cmd.dataToRender()
        List data = projectOverviewService.overviewProjectQuery(params.project)
        dataToRender.iTotalRecords = data.size()
        dataToRender.iTotalDisplayRecords = dataToRender.iTotalRecords
        dataToRender.aaData = data
        render dataToRender as JSON
    }

    JSON dataTableSourcePatientsAndSamplesGBCountPerProject(DataTableCommand cmd) {
        Project project = projectService.getProjectByName(params.project)
        Map dataToRender = cmd.dataToRender()
        List data = projectOverviewService.patientsAndSamplesGBCountPerProject(project)
        dataToRender.iTotalRecords = data.size()
        dataToRender.iTotalDisplayRecords = dataToRender.iTotalRecords
        dataToRender.aaData = data
        render dataToRender as JSON
    }

    JSON dataTableSourceSampleTypeNameCountBySample(DataTableCommand cmd) {
        Project project = projectService.getProjectByName(params.project)
        Map dataToRender = cmd.dataToRender()
        List data = projectOverviewService.sampleTypeNameCountBySample(project)
        dataToRender.iTotalRecords = data.size()
        dataToRender.iTotalDisplayRecords = dataToRender.iTotalRecords
        dataToRender.aaData = data
        render dataToRender as JSON
    }

    JSON dataTableSourceCenterNameRunId(DataTableCommand cmd) {
       Project project = projectService.getProjectByName(params.project)
       Map dataToRender = cmd.dataToRender()
       List data = projectOverviewService.centerNameRunId(project)
       List dataLast = projectOverviewService.centerNameRunIdLastMonth(project)

       Map dataLastMap = [:]
       dataLast.each {
       dataLastMap.put(it[0], it[1])
                   }

       dataToRender.iTotalRecords = data.size()
       dataToRender.iTotalDisplayRecords = dataToRender.iTotalRecords
       dataToRender.aaData = []
       data.each {
           List line = []
           line << it[0]
           line << it[1]
           if (dataLastMap.containsKey(it[0])) {
               line << dataLastMap.get(it[0])
           } else {
                    line << "0"
           }
           dataToRender.aaData << line
       }
       render dataToRender as JSON
    }

}
