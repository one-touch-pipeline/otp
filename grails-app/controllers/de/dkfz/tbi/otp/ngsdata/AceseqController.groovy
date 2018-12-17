package de.dkfz.tbi.otp.ngsdata

import grails.converters.JSON

import de.dkfz.tbi.otp.dataprocessing.aceseq.AceseqResultsService

class AceseqController extends AbstractAnalysisController {

    AceseqResultsService aceseqResultsService

    JSON dataTableResults(ResultTableCommand cmd) {
        Map dataToRender = cmd.dataToRender()
        List data = aceseqResultsService.getCallingInstancesForProject(cmd.project?.name)
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

        Map<PlotType, List<Integer>> plotNumber = [:]

        if (aceseqResultsService.getFiles(cmd.bamFilePairAnalysis, PlotType.ACESEQ_EXTRA)) {
            int count = aceseqResultsService.getFiles(cmd.bamFilePairAnalysis, PlotType.ACESEQ_EXTRA).size()
            plotNumber.put(PlotType.ACESEQ_EXTRA, count ? (0..count - 1) : [])
        }

        if (aceseqResultsService.getFiles(cmd.bamFilePairAnalysis, PlotType.ACESEQ_ALL)) {
            int count = aceseqResultsService.getFiles(cmd.bamFilePairAnalysis, PlotType.ACESEQ_ALL).size()
            plotNumber.put(PlotType.ACESEQ_ALL, count ? (0..count - 1) : [])
        }

        return [
                bamFilePairAnalysis: cmd.bamFilePairAnalysis,
                plotType: [PlotType.ACESEQ_WG_COVERAGE, PlotType.ACESEQ_TCN_DISTANCE_COMBINED_STAR, PlotType.ACESEQ_QC_GC_CORRECTED, PlotType.ACESEQ_GC_CORRECTED],
                plotNumber: plotNumber,
                error: null,
        ]
    }

    Map plotImages(BamFilePairAnalysisCommand cmd) {
        if (cmd.hasErrors()) {
            render status: 404
            return
        }
        List<File> files = aceseqResultsService.getFiles(cmd.bamFilePairAnalysis, cmd.plotType)

        if (files) {
            if (cmd.plotType in [PlotType.ACESEQ_EXTRA, PlotType.ACESEQ_ALL]) {
                render file: files[cmd.index], contentType: "image/png"
            } else {
                render file: files.first(), contentType: "image/png"
            }
        }

        return [
                error: "File not found",
                pid: "no File",
        ]
    }
}
