package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.utils.DataTableCommand;
import grails.converters.JSON
import javax.xml.transform.Transformer
import javax.xml.transform.TransformerFactory
import javax.xml.transform.stream.StreamResult
import javax.xml.transform.stream.StreamSource

class ProjectOverviewController {

    def projectService

    def projectOverviewService

    Map index() {
        return [projects: projectService.getAllProjects()]
    }

    JSON dataTableSource(DataTableCommand cmd) {
        Map dataToRender = cmd.dataToRender()
        List data = projectOverviewService.overviewProjectQuery(params.project)
        dataToRender.iTotalRecords = data.size()
        dataToRender.iTotalDisplayRecords = dataToRender.iTotalRecords
        dataToRender.aaData = data
        render dataToRender as JSON
    }
}
