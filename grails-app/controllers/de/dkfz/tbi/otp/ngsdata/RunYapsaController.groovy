package de.dkfz.tbi.otp.ngsdata

import grails.converters.JSON

import de.dkfz.tbi.otp.dataprocessing.runYapsa.RunYapsaResultsService

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
