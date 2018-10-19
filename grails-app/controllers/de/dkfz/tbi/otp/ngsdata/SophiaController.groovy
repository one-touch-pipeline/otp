package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.dataprocessing.sophia.*
import grails.converters.*

class SophiaController extends AbstractAnalysisController {

    SophiaResultsService sophiaResultsService

    JSON dataTableResults(ResultTableCommand cmd) {
        Map dataToRender = cmd.dataToRender()
        List data = sophiaResultsService.getCallingInstancesForProject(cmd.project?.name)
        dataToRender.iTotalRecords = data.size()
        dataToRender.iTotalDisplayRecords = dataToRender.iTotalRecords
        dataToRender.aaData = data

        render dataToRender as JSON
    }

    Map plots(BamFilePairAnalysisCommand cmd) {
        if (cmd.hasErrors()) {
            render status: 404
            return
        }
        if (sophiaResultsService.getFiles(cmd.bamFilePairAnalysis, cmd.plotType)) {
            return [
                    id: cmd.bamFilePairAnalysis.id,
                    pid: cmd.bamFilePairAnalysis.individual.pid,
                    plotType: cmd.plotType,
                    error: null,
            ]
        }
        return [
                error: "File not found",
                pid: "no File",
        ]
    }

    def renderPDF(BamFilePairAnalysisCommand cmd) {
        if (cmd.hasErrors()) {
            response.sendError(404)
            return
        }
        List<File> stream = sophiaResultsService.getFiles(cmd.bamFilePairAnalysis, cmd.plotType)
        if (stream) {
            render file: stream.first(), contentType: "application/pdf"
        } else {
            render status: 404
        }
    }
}
