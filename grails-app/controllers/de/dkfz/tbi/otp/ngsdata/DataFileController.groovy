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
import org.springframework.security.access.annotation.Secured

import de.dkfz.tbi.otp.CommentService
import de.dkfz.tbi.otp.ngsqc.FastqcResultsService
import de.dkfz.tbi.otp.utils.CommentCommand
import de.dkfz.tbi.util.TimeFormats

@Secured('isFullyAuthenticated()')
class DataFileController {

    LsdfFilesService lsdfFilesService
    MetaDataService metaDataService
    CommentService commentService
    FastqcResultsService fastqcResultsService

    static allowedMethods = [
            showDetails        : "GET",
            saveDataFileComment: "POST",
    ]

    def showDetails(ShowDetailsCommand cmd) {
        if (cmd.hasErrors()) {
            return response.sendError(404)
        }

        DataFile dataFile = metaDataService.getDataFile(cmd.id)
        if (!dataFile) {
            return response.sendError(404)
        }

        return [
                dataFile       : dataFile,
                dateExecuted   : TimeFormats.DATE.getFormattedDate(dataFile.dateExecuted),
                dateFileSystem : TimeFormats.DATE_TIME_WITHOUT_SECONDS.getFormattedDate(dataFile.dateFileSystem),
                dateCreated    : TimeFormats.DATE_TIME_WITHOUT_SECONDS.getFormattedDate(dataFile.dateCreated),
                withdrawnDate  : TimeFormats.DATE_TIME_WITHOUT_SECONDS.getFormattedDate(dataFile.withdrawnDate),
                entries        : MetaDataEntry.findAllByDataFile(dataFile, [sort: "key.id"]),
                fullPath       : lsdfFilesService.getFileFinalPath(dataFile),
                vbpPath        : lsdfFilesService.getFileViewByPidPath(dataFile),
                fastqcAvailable: fastqcResultsService.isFastqcAvailable(dataFile),
        ]
    }

    JSON saveDataFileComment(CommentCommand cmd) {
        DataFile dataFile = metaDataService.getDataFile(cmd.id)
        commentService.saveComment(dataFile, cmd.comment)
        Map dataToRender = [date: TimeFormats.WEEKDAY_DATE_TIME.getFormattedDate(dataFile.comment.modificationDate), author: dataFile.comment.author]
        render dataToRender as JSON
    }
}

class ShowDetailsCommand {
    Long id
}
