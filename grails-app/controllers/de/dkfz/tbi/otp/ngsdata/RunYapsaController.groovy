package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.dataprocessing.runYapsa.*
import grails.converters.*

class RunYapsaController extends AbstractAnalysisController {

    RunYapsaResultsService runYapsaResultsService

    JSON dataTableResults(ResultTableCommand cmd) {
        Map dataToRender = cmd.dataToRender()
        List data = runYapsaResultsService.getCallingInstancesForProject(cmd.project?.name)
        dataToRender.iTotalRecords = data.size()
        dataToRender.iTotalDisplayRecords = dataToRender.iTotalRecords
        dataToRender.aaData = data

        render dataToRender as JSON
    }
}
