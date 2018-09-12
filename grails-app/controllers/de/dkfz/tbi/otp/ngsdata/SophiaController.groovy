package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.sophia.*
import grails.converters.*

import java.text.*

class SophiaController extends AbstractAnalysisController {

    JSON dataTableResults(ResultTableCommand cmd) {
        Map dataToRender = cmd.dataToRender()
        SimpleDateFormat sdf = new SimpleDateFormat('yyyy-MM-dd HH:mm')
        List results = analysisService.getCallingInstancesForProject(SophiaInstance, cmd.project?.name)
        List data = results.collect { Map properties ->
            SophiaQc qc = SophiaQc.findBySophiaInstance(SophiaInstance.get(properties.instanceId as long))
            properties.putAll([
                    controlMassiveInvPrefilteringLevel: qc?.controlMassiveInvPrefilteringLevel,
                    tumorMassiveInvFilteringLevel: qc?.tumorMassiveInvFilteringLevel,
                    rnaContaminatedGenesMoreThanTwoIntron: qc?.rnaContaminatedGenesMoreThanTwoIntron,
                    rnaContaminatedGenesCount: qc?.rnaContaminatedGenesCount,
                    rnaDecontaminationApplied: qc?.rnaDecontaminationApplied,
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

    def renderPDF(BamFilePairAnalysisCommand cmd) {
        if (cmd.hasErrors()) {
            response.sendError(404)
            return
        }
        List<File> stream = analysisService.getFiles(cmd.bamFilePairAnalysis, cmd.plotType)
        if (stream) {
            render file: stream.first(), contentType: "application/pdf"
        } else {
            render status: 404
        }
    }
}