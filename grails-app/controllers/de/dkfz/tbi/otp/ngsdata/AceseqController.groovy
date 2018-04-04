package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.utils.*
import grails.converters.*

import java.text.*


class AceseqController extends AbstractAnalysisController {

    JSON dataTableResults(ResultTableCommand cmd) {
        Map dataToRender = cmd.dataToRender()
        SimpleDateFormat sdf = new SimpleDateFormat('yyyy-MM-dd HH:mm')
        List results = analysisService.getCallingInstancesForProject(AceseqInstance, cmd.project?.name)
        List data = results.collect { Map properties ->
            AceseqQc qc = AceseqQc.findByAceseqInstanceAndNumber(AceseqInstance.get(properties.instanceId as long), 1)
            properties.putAll([
                    tcc: FormatHelper.formatNumber(qc?.tcc),
                    ploidy: FormatHelper.formatNumber(qc?.ploidy),
                    ploidyFactor: qc?.ploidyFactor,
                    goodnessOfFit: FormatHelper.formatNumber(qc?.goodnessOfFit),
                    gender: qc?.gender,
                    solutionPossible: qc?.solutionPossible,
            ])
            properties.remove('libPrepKit1')
            properties.remove('libPrepKit2')
            properties.dateCreated = sdf.format(properties.dateCreated)
            if (properties.processingState != AnalysisProcessingStates.FINISHED) {
                properties.remove('instanceId')
            }
            return properties
        }

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

        if (analysisService.getFiles(cmd.bamFilePairAnalysis, PlotType.ACESEQ_EXTRA)) {
            int count = analysisService.getFiles(cmd.bamFilePairAnalysis, PlotType.ACESEQ_EXTRA).size()
            plotNumber.put(PlotType.ACESEQ_EXTRA, count ? (0..count - 1) : [])
        }

        if (analysisService.getFiles(cmd.bamFilePairAnalysis, PlotType.ACESEQ_ALL)) {
            int count = analysisService.getFiles(cmd.bamFilePairAnalysis, PlotType.ACESEQ_ALL).size()
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
        List<File> files = analysisService.getFiles(cmd.bamFilePairAnalysis, cmd.plotType)

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

class ResultTableCommand extends DataTableCommand {
    Project project
}
