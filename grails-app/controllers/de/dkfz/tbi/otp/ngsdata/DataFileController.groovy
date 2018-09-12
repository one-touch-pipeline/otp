package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.CommentService
import de.dkfz.tbi.otp.ngsqc.FastqcResultsService
import de.dkfz.tbi.otp.utils.CommentCommand
import grails.converters.JSON

class DataFileController {

    LsdfFilesService lsdfFilesService
    MetaDataService metaDataService
    CommentService commentService
    FastqcResultsService fastqcResultsService

    def showDetails(ShowDetailsCommand cmd) {
        if (cmd.hasErrors()) {
            response.sendError(404)
            return
        }

        DataFile dataFile = metaDataService.getDataFile(cmd.id)
        if (!dataFile) {
            response.sendError(404)
            return
        }
        List<MetaDataEntry> entries = MetaDataEntry.findAllByDataFile(dataFile, [sort:"key.id"])
        Map<MetaDataEntry, Boolean> changelogs = metaDataService.checkForChangelog(entries)

        List<String> keys = []
        List<String> values = []

        keys <<  "fullPath"
        values << lsdfFilesService.getFileFinalPath(dataFile)

        keys << "view-by-pid path"
        values << lsdfFilesService.getFileViewByPidPath(dataFile)

        [
                dataFile: dataFile,
                entries: entries,
                values: values,
                changelogs: changelogs,
                comment: dataFile.comment,
                fastqcAvailable: fastqcResultsService.isFastqcAvailable(dataFile),
        ]
    }

    def saveDataFileComment(CommentCommand cmd) {
        DataFile dataFile = metaDataService.getDataFile(cmd.id)
        commentService.saveComment(dataFile, cmd.comment)
        def dataToRender = [date: dataFile.comment.modificationDate.format('EEE, d MMM yyyy HH:mm'), author: dataFile.comment.author]
        render dataToRender as JSON
    }

    def updateMetaData = {
        MetaDataEntry entry = metaDataService.getMetaDataEntryById(params.id as Long)
        if (!entry) {
            Map data = [error: g.message(code: "datafile.metadata.update.notFound", args: [params.id])]
            render data as JSON
            return
        }
        Map data = [:]
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

class ShowDetailsCommand {
    Long id
}
