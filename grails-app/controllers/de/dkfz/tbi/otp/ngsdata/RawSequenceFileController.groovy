/*
 * Copyright 2011-2024 The OTP authors
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
import org.springframework.security.access.prepost.PreAuthorize

import de.dkfz.tbi.otp.CommentService
import de.dkfz.tbi.otp.infrastructure.RawSequenceDataViewFileService
import de.dkfz.tbi.otp.infrastructure.RawSequenceDataWorkFileService
import de.dkfz.tbi.otp.ngsqc.FastqcResultsService
import de.dkfz.tbi.otp.utils.CommentCommand
import de.dkfz.tbi.otp.utils.TimeFormats

@PreAuthorize('isFullyAuthenticated()')
class RawSequenceFileController {

    MetaDataService metaDataService
    CommentService commentService
    FastqcResultsService fastqcResultsService
    RawSequenceDataWorkFileService rawSequenceDataWorkFileService
    RawSequenceDataViewFileService rawSequenceDataViewFileService

    static allowedMethods = [
            showDetails        : "GET",
            saveDataFileComment: "POST",
    ]

    def showDetails(ShowDetailsCommand cmd) {
        if (cmd.hasErrors()) {
            return response.sendError(404)
        }

        RawSequenceFile rawSequenceFile = metaDataService.getRawSequenceFile(cmd.id)
        if (!rawSequenceFile) {
            return response.sendError(404)
        }

        return [
                rawSequenceFile: rawSequenceFile,
                fileSize       : formatFileSize(rawSequenceFile.fileSize),
                dateExecuted   : TimeFormats.DATE.getFormattedDate(rawSequenceFile.dateExecuted),
                dateFileSystem : TimeFormats.DATE_TIME_WITHOUT_SECONDS.getFormattedDate(rawSequenceFile.dateFileSystem),
                dateCreated    : TimeFormats.DATE_TIME_WITHOUT_SECONDS.getFormattedDate(rawSequenceFile.dateCreated),
                withdrawnDate  : TimeFormats.DATE_TIME_WITHOUT_SECONDS.getFormattedDate(rawSequenceFile.withdrawnDate),
                entries        : MetaDataEntry.findAllBySequenceFile(rawSequenceFile, [sort: "key.id"]),
                fullPath       : rawSequenceDataWorkFileService.getFilePath(rawSequenceFile).toString(),
                vbpPath        : rawSequenceDataViewFileService.getFilePath(rawSequenceFile).toString(),
                fastqcAvailable: fastqcResultsService.isFastqcAvailable(rawSequenceFile),
        ]
    }

    JSON saveDataFileComment(CommentCommand cmd) {
        RawSequenceFile rawSequenceFile = metaDataService.getRawSequenceFile(cmd.id)
        commentService.saveComment(rawSequenceFile, cmd.comment)
        Map dataToRender = [
                date: TimeFormats.WEEKDAY_DATE_TIME.getFormattedDate(rawSequenceFile.comment.modificationDate),
                author: rawSequenceFile.comment.author,
        ]
        return render(dataToRender as JSON)
    }

    private String formatFileSize(long fileSize) {
        if (fileSize > 1e9) {
            return String.format("%.2f GB", fileSize / 1e9)
        } else if (fileSize > 1e6) {
            return String.format("%.2f MB", fileSize / 1e6)
        } else if (fileSize > 1e3) {
            return String.format("%.2f kB", fileSize / 1e3)
        }
        return fileSize
    }
}

class ShowDetailsCommand {
    Long id
}
