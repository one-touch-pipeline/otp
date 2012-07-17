package de.dkfz.tbi.otp.ngsdata

import grails.converters.JSON

class DataFileController {

    def lsdfFilesService
    def metaDataService

    def showDetails = {

        DataFile dataFile = DataFile.get(params.id)
        List<MetaDataEntry> entries = MetaDataEntry.findAllByDataFile(dataFile, [sort:"key.id"])
        Map<MetaDataEntry, Boolean> changelogs = metaDataService.checkForChangelog(entries)

        List<String> keys = new Vector<String>()
        List<String> values = new Vector<String>()

        keys <<  "fullPath"
        values << lsdfFilesService.getFileFinalPath(dataFile)

        keys << "view-by-pid path"
        values << lsdfFilesService.getFileViewByPidPath(dataFile)

        return [dataFile: dataFile, entries: entries, values: values, changelogs: changelogs]
    }

    def updateMetaData = {
        MetaDataEntry entry = metaDataService.getMetaDataEntryById(params.id as Long)
        if (!entry) {
            def data = [error: g.message(code: "datafile.metadata.update.notFound", args: [params.id])]
            render data as JSON
            return
        }
        def data = [:]
        try {
            metaDataService.updateMetaDataEntry(entry, params.value)
            data.put("success", true)
        } catch (MetaDataEntryUpdateException e) {
            data.put("error", g.message(code: "datafile.metadata.update.error"))
        } catch (ChangelogException e) {
            data.put("error", g.message(code: "datafile.metadata.update.changelog.error"))
        }
        render data as JSON
    }

    def metaDataChangelog = {
        MetaDataEntry entry = metaDataService.getMetaDataEntryById(params.id as Long)
        if (!entry) {
            List data = []
            render data as JSON
            return
        }
        List data = []
        metaDataService.retrieveChangeLog(entry).each { ChangeLog log ->
            data << [comment: log.comment, from: log.fromValue, to: log.toValue, source: log.source.toString(), timestamp: log.dateCreated]
        }
        render data as JSON
    }
}
