package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.utils.*
import grails.converters.*

import java.text.*

class IndelController extends AbstractAnalysisController {

    Map plots(BamFilePairAnalysisCommand cmd) {
        if (cmd.hasErrors()) {
            render status: 404
            return
        }
        if (analysisService.getFiles(cmd.bamFilePairAnalysis, cmd.plotType)) {
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
        List<File> stream = analysisService.getFiles(cmd.bamFilePairAnalysis, cmd.plotType)
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
        SimpleDateFormat sdf = new SimpleDateFormat('yyyy-MM-dd HH:mm')
        List results = analysisService.getCallingInstancesForProject(IndelCallingInstance, cmd.project?.name)
        List data = results.collect { Map properties ->
            IndelQualityControl qc = IndelQualityControl.findByIndelCallingInstance(IndelCallingInstance.get(properties.instanceId as long))
            IndelSampleSwapDetection sampleSwap = IndelSampleSwapDetection.findByIndelCallingInstance(IndelCallingInstance.get(properties.instanceId as long))
            properties.putAll([
                    numIndels: qc?.numIndels ?: "",
                    numIns: qc?.numIns ?: "",
                    numDels: qc?.numDels ?: "",
                    numSize1_3: qc?.numSize1_3 ?: "",
                    numSize4_10: qc?.numDelsSize4_10 ?: "",
                    somaticSmallVarsInTumor: sampleSwap?.somaticSmallVarsInTumor ?: "",
                    somaticSmallVarsInControl: sampleSwap?.somaticSmallVarsInControl ?: "",
                    somaticSmallVarsInTumorCommonInGnomad: sampleSwap?.somaticSmallVarsInTumorCommonInGnomad ?: "",
                    somaticSmallVarsInControlCommonInGnomad: sampleSwap?.somaticSmallVarsInControlCommonInGnomad ?: "",
                    somaticSmallVarsInTumorPass: sampleSwap?.somaticSmallVarsInTumorPass ?: "",
                    somaticSmallVarsInControlPass: sampleSwap?.somaticSmallVarsInControlPass ?: "",
                    tindaSomaticAfterRescue: sampleSwap?.tindaSomaticAfterRescue ?: "",
                    tindaSomaticAfterRescueMedianAlleleFreqInControl: sampleSwap ? FormatHelper.formatNumber(sampleSwap.tindaSomaticAfterRescueMedianAlleleFreqInControl) : "",
            ])
            Collection<String> libPrepKitShortNames
            if (SeqTypeNames.fromSeqTypeName(properties.seqTypeName)?.isWgbs()) {
                assert properties.libPrepKit1 == null && properties.libPrepKit2 == null
                libPrepKitShortNames = IndelCallingInstance.get(properties.instanceId).containedSeqTracks*.
                        libraryPreparationKit*.shortDisplayName
            } else {
                libPrepKitShortNames = [(String) properties.libPrepKit1, (String) properties.libPrepKit2]
            }
            properties.libPrepKits = libPrepKitShortNames.unique().collect { it ?: 'unknown' }.join(", <br>")
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
}
