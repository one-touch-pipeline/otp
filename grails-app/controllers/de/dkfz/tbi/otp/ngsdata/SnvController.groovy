package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import grails.converters.*

class SnvController extends AbstractAnalysisController {

    SnvResultsService snvResultsService

    Map plots(BamFilePairAnalysisCommand cmd) {
        if (cmd.hasErrors()) {
            render status: 404
            return
        }
        if (snvResultsService.getFiles(cmd.bamFilePairAnalysis, cmd.plotType)) {
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
        List<File> stream = snvResultsService.getFiles(cmd.bamFilePairAnalysis, cmd.plotType)
        if (stream) {
            render file: stream.first(), contentType: "application/pdf"
        } else {
            render status: 404
        }
    }

    JSON dataTableResults(ResultTableCommand cmd) {
        Map dataToRender = cmd.dataToRender()
        List data = snvResultsService.getCallingInstancesForProject(cmd.project?.name)
        dataToRender.iTotalRecords = data.size()
        dataToRender.iTotalDisplayRecords = dataToRender.iTotalRecords
        dataToRender.aaData = data

        render dataToRender as JSON
    }
}
