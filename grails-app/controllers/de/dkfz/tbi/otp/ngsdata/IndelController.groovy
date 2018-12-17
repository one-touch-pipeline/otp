package de.dkfz.tbi.otp.ngsdata

import grails.converters.JSON

import de.dkfz.tbi.otp.dataprocessing.indelcalling.IndelResultsService

class IndelController extends AbstractAnalysisController {

    IndelResultsService indelResultsService

    Map plots(BamFilePairAnalysisCommand cmd) {
        if (cmd.hasErrors()) {
            render status: 404
            return
        }
        if (indelResultsService.getFiles(cmd.bamFilePairAnalysis, cmd.plotType)) {
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

    def renderPlot(BamFilePairAnalysisCommand cmd) {
        if (cmd.hasErrors()) {
            response.sendError(404)
            return
        }
        List<File> stream = indelResultsService.getFiles(cmd.bamFilePairAnalysis, cmd.plotType)
        if (stream) {
            if (cmd.plotType == PlotType.INDEL) {
                render file: stream.first(), contentType: "application/pdf"
            } else {
                render file: stream.first(), contentType: "image/png"
            }
        } else {
            render status: 404
        }
    }

    JSON dataTableResults(ResultTableCommand cmd) {
        Map dataToRender = cmd.dataToRender()
        List data = indelResultsService.getCallingInstancesForProject(cmd.project?.name)
        dataToRender.iTotalRecords = data.size()
        dataToRender.iTotalDisplayRecords = dataToRender.iTotalRecords
        dataToRender.aaData = data

        render dataToRender as JSON
    }
}
