/*
 * Copyright 2011-2019 The OTP authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package de.dkfz.tbi.otp.ngsdata

import grails.converters.JSON

import de.dkfz.tbi.otp.CommentService
import de.dkfz.tbi.otp.ngsqc.FastqcResultsService
import de.dkfz.tbi.otp.utils.CommentCommand

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
        Map<MetaDataEntry, Boolean> changelogs = metaDataService.checkForChangelog(entries.clone())
        List<String> keys = []
        List<String> values = []

        keys <<  "fullPath"
        values << lsdfFilesService.getFileFinalPath(dataFile)

        keys << "view-by-pid path"
        values << lsdfFilesService.getFileViewByPidPath(dataFile)

        [
                dataFile: dataFile,
                entries: entries,
                values: values*.replaceAll('//', '/'),
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
