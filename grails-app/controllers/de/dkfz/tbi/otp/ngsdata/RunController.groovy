package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.utils.DataTableCommand
import grails.converters.JSON

class RunController {

    def lsdfFilesService
    def runService
    def fastqcResultsService

    def display = {
        redirect(action: "show", id: params.id)
    }

    def show = {
        if (!params.id) {
            params.id = "0"
        }
        Run run = runService.getRun(params.id)
        if (!run) {
            response.sendError(404)
            return
        }
        List<MetaDataKey> keys = []
        keys[0] = MetaDataKey.findByName("SAMPLE_ID")
        keys[1] = MetaDataKey.findByName("WITHDRAWN")

        return [
            run: run,
            finalPaths: lsdfFilesService.getAllPathsForRun(run),
            keys: keys,
            processParameters: runService.retrieveProcessParameters(run),
            metaDataFiles: runService.retrieveMetaDataFilesByInitialPath(run),
            seqTracks: runService.retrieveSequenceTrackInformation(run),
            errorFiles: runService.dataFilesWithError(run),
            nextRun: runService.nextRun(run),
            previousRun: runService.previousRun(run),
            fastqcLinks: fastqcResultsService.fastqcLinkMap(run),
            fastqcSummary: fastqcResultsService.fastqcSummaryMap(run)
        ]
    }

    def list = {
    }

    def dataTableSource(DataTableCommand cmd) {
        Map dataToRender = cmd.dataToRender()

        dataToRender.iTotalRecords = runService.countRun(cmd.sSearch)
        dataToRender.iTotalDisplayRecords = dataToRender.iTotalRecords

        runService.listRuns(cmd.iDisplayStart, cmd.iDisplayLength, cmd.sortOrder, cmd.iSortCol_0, cmd.sSearch).each { run ->
            dataToRender.aaData << [
                [id: run.id, text: run.name],
                run.seqCenter.name.toLowerCase(),
                run.storageRealm?.toString()?.toLowerCase(),
                run.dateCreated?.format("yyyy-MM-dd hh:mm:ss"),
                run.dateExecuted?.format("yyyy-MM-dd"),
                run.blacklisted,
                run.multipleSource,
                run.qualityEvaluated ? String.format("%.2f", run.dataQuality) : "NaN"
            ]
        }
        render dataToRender as JSON
    }

}
