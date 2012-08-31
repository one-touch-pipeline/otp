package de.dkfz.tbi.otp.ngsdata

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

    def dataTableSource = {
        int start = 0
        int length = 10
        if (params.iDisplayStart) {
            start = params.iDisplayStart as int
        }
        if (params.iDisplayLength) {
            length = Math.min(100, params.iDisplayLength as int)
        }
        int column = 0
        if (params.iSortCol_0) {
            column = params.iSortCol_0 as int
        }
        def dataToRender = [:]
        dataToRender.sEcho = params.sEcho
        dataToRender.aaData = []

        dataToRender.offset = start
        dataToRender.iSortCol_0 = params.iSortCol_0
        dataToRender.sSortDir_0 = params.sSortDir_0
        dataToRender.iTotalRecords = runService.countRun(params.sSearch)
        dataToRender.iTotalDisplayRecords = dataToRender.iTotalRecords

        runService.listRuns(start, length, params.sSortDir_0 == "asc", column, params.sSearch).each { run ->
            dataToRender.aaData << [
                [id: run.id, text: run.name],
                run.seqCenter.name.toLowerCase(),
                run.storageRealm?.toString()?.toLowerCase(),
                run.dateCreated?.format("yyyy-MM-dd hh:mm:ss"),
                run.dateExecuted?.format("yyyy-MM-dd"),
                run.blacklisted,
                run.multipleSource,
                fastqcResultsService.isFastqcAvailable(run)
            ]
        }
        render dataToRender as JSON
    }
}
