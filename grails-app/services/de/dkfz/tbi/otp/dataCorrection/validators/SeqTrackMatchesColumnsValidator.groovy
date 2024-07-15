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
package de.dkfz.tbi.otp.dataCorrection.validators

import groovy.transform.CompileDynamic
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.dataCorrection.DataSwapService
import de.dkfz.tbi.otp.dataCorrection.DataSwapValidator
import de.dkfz.tbi.otp.dataswap.DataSwapColumn
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.utils.MessageSourceService
import de.dkfz.tbi.otp.utils.spreadsheet.Cell
import de.dkfz.tbi.otp.utils.spreadsheet.Column
import de.dkfz.tbi.otp.utils.spreadsheet.validation.LogLevel
import de.dkfz.tbi.otp.utils.spreadsheet.validation.ValidationContext

@Component
class SeqTrackMatchesColumnsValidator implements DataSwapValidator {

    @Autowired
    MessageSourceService messageSourceService

    @Autowired
    DataSwapService dataSwapService

    @Override
    @CompileDynamic
    void validate(ValidationContext context) {
        Column seqTrackColumn = context.spreadsheet.getColumn(dataSwapService.getHeaderName(DataSwapColumn.SEQ_TRACK, DataSwapService.HeaderType.RAW))
        if (!seqTrackColumn) {
            return
        }

        Map<Long, SeqTrack> seqTrackMap = SeqTrack.findAllByIdInList(context.spreadsheet.dataRows*.getCell(seqTrackColumn)*.text*.toLong())
                .collectEntries { seqTrack -> [(seqTrack.id): seqTrack] }

        context.spreadsheet.dataRows.forEach { row ->
            Cell seqTrackCell = row.getCell(seqTrackColumn)

            SeqTrack seqTrack = seqTrackMap.get(seqTrackCell.text as Long)
            if (seqTrack) {
                DataSwapColumn.duplicatedColumns.each { columnName ->
                    Column column = getOldColumn(context, columnName)
                    Cell cell = row.getCell(column)
                    String description = messageSourceService.getMessage(columnName.message)
                    checkForProblem(context, cell, seqTrackCell, columnName.seqTrackTextMapping(seqTrack) ?: '', description)
                }
            } else {
                context.addProblem([seqTrackCell] as Set, LogLevel.ERROR, "SeqTrack with ID \"${seqTrackCell.text}\" doesn't exist")
            }
        }
    }

    private Column getOldColumn(ValidationContext context, DataSwapColumn column) {
        return context.spreadsheet.getColumn(dataSwapService.getHeaderName(column, DataSwapService.HeaderType.OLD))
    }

    private void checkForProblem(ValidationContext context, Cell cellToCheck, Cell seqTrackCell, String originalValue, String description) {
        if (cellToCheck.text != originalValue) {
            context.addProblem([seqTrackCell, cellToCheck] as Set, LogLevel.ERROR, "${description.capitalize()} \"${cellToCheck.text}\" doesn't " +
                    "correspond to the seqTrack's current ${description.capitalize()} \"${originalValue}\".")
        }
    }
}
