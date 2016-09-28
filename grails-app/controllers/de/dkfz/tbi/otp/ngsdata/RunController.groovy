package de.dkfz.tbi.otp.ngsdata


import grails.converters.JSON
import groovy.json.JsonSlurper
import de.dkfz.tbi.otp.ngsdata.Run.StorageRealm
import de.dkfz.tbi.otp.utils.DataTableCommand

class RunController {

    def lsdfFilesService
    def runService
    def fastqcResultsService
    def seqCenterService

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
        keys[0] = MetaDataKey.findByName(MetaDataColumn.SAMPLE_ID.name())
        keys[1] = MetaDataKey.findByName(MetaDataColumn.WITHDRAWN.name())

        return [
            run: run,
            finalPaths: lsdfFilesService.getAllPathsForRun(run, true),
            keys: keys,
            processParameters: runService.retrieveProcessParameters(run),
            metaDataFiles: runService.retrieveMetaDataFiles(run),
            seqTracks: runService.retrieveSequenceTrackInformation(run),
            errorFiles: runService.dataFilesWithError(run),
            nextRun: runService.nextRun(run),
            previousRun: runService.previousRun(run),
            fastqcLinks: fastqcResultsService.fastqcLinkMap(run),
        ]
    }

    def list = {
        Map retValue = [
            seqCenters: seqCenterService.allSeqCenters(),
            storageRealm: StorageRealm.values()
        ]
        return retValue
    }
    def dataTableSource(DataTableCommand cmd) {
        Map dataToRender = cmd.dataToRender()

        RunFiltering filtering = RunFiltering.fromJSON(params.filtering)

        dataToRender.iTotalRecords = runService.countRun(filtering, cmd.sSearch)
        dataToRender.iTotalDisplayRecords = dataToRender.iTotalRecords

        runService.listRuns(cmd.sortOrder, RunSortColumn.fromDataTable(cmd.iSortCol_0), filtering, cmd.sSearch).each { run ->
            dataToRender.aaData << [
                id: run.id,
                name: run.name,
                seqCenters: run.seqCenter?.toString()?.toLowerCase(),
                storageRealm: run.storageRealm?.toString()?.toLowerCase(),
                dateCreated: run.dateCreated?.format("yyyy-MM-dd"),
                dateExecuted: run.dateExecuted?.format("yyyy-MM-dd"),
            ]
        }
        render dataToRender as JSON
    }
}

enum RunSortColumn {
    RUN("name"),
    SEQCENTER("seqCenter"),
    STORAGEREALM("storageRealm"),
    DATECREATED("dateCreated"),
    DATEEXECUTED("dateExecuted"),

    private final String columnName

    RunSortColumn(String column) {
        this.columnName = column
    }

    static RunSortColumn fromDataTable(int column) {
        switch (column) {
            case 0:
                return RunSortColumn.RUN
            case 1:
                return RunSortColumn.SEQCENTER
            case 2:
                return RunSortColumn.STORAGEREALM
            case 3:
                return RunSortColumn.DATECREATED
            case 4:
                return RunSortColumn.DATEEXECUTED
            default:
                return RunSortColumn.RUN
        }
    }
}

/**
 * Class describing the various filtering to do on WorkPackage.
 *
 */
class RunFiltering {
    List<String> name = []
    List<Long> seqCenter = []
    List<StorageRealm> storageRealm = []
    List<List<Date>> dateCreated = []
    List<List<Date>> dateExecuted = []

    boolean enabled = false

    static RunFiltering fromJSON(String json) {
        RunFiltering filtering = new RunFiltering()
        if (!json) {
            return filtering
        }
        def slurper = new JsonSlurper()
        def each = slurper.parseText(json).each {
            switch (it.type) {
                case "runSearch":
                    if (it.value && it.value.length() >= 3) {
                        filtering.name << it.value
                        filtering.enabled = true
                    }
                    break
                case "seqCenterSelection":
                    if (it.value.isLong()) {
                        filtering.seqCenter << (it.value as Long)
                        filtering.enabled = true
                    }
                    break
                case "storageRealmSelection":
                    if (it.value) {
                        filtering.storageRealm << (it.value as StorageRealm)
                        filtering.enabled = true
                    }
                    break
                case "dateCreatedSelection":
                    if (it.value) {
                        int start_day = it.value.start_day as int
                        int start_month = it.value.start_month as int
                        int start_year = it.value.start_year as int
                        Date dateFrom = new Date(start_year - 1900, start_month - 1, start_day)
                        int end_day = it.value.end_day as int
                        int end_month = it.value.end_month as int
                        int end_year = it.value.end_year as int
                        Date dateTo = new Date(end_year - 1900, end_month - 1, end_day + 1)
                        filtering.dateCreated << [dateFrom, dateTo]
                        filtering.enabled = true
                    }
                    break
                case "dateExecutedSelection":
                    if (it.value) {
                        int start_day = it.value.start_day as int
                        int start_month = it.value.start_month as int
                        int start_year = it.value.start_year as int
                        Date dateFrom = new Date(start_year - 1900, start_month - 1, start_day)
                        int end_day = it.value.end_day as int
                        int end_month = it.value.end_month as int
                        int end_year = it.value.end_year as int
                        Date dateTo = new Date(end_year - 1900, end_month - 1, end_day + 1)
                        filtering.dateExecuted << [dateFrom, dateTo]
                        filtering.enabled = true
                    }
                    break
            }
        }
        return filtering
    }
}
