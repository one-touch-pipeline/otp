/*
 * Copyright 2011-2021 The OTP authors
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
package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.ngsdata.metadatavalidation.bam.BamMetadataValidator
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidator
import de.dkfz.tbi.util.spreadsheet.*
import de.dkfz.tbi.util.spreadsheet.validation.*

import java.util.regex.Pattern

@Component
class CommentAndNoteValidator<C extends ValidationContext> implements MetadataValidator, BamMetadataValidator, Validator<C> {

    static final Pattern COMMENT_OR_NOTE = Pattern.compile("(comment|note)", Pattern.CASE_INSENSITIVE)

    @Override
    Collection<String> getDescriptions() {
        return []
    }

    @Override
    void validate(MetadataValidationContext context) {
        List<Column> columns = []
        context.spreadsheet.header.cells.each { Cell cell ->
            if (COMMENT_OR_NOTE.matcher(cell.text).find()) {
                columns.add(context.spreadsheet.getColumn(cell.text))
            }
        }

        columns.each { Column column ->
            List<Cell> cells = []
            context.spreadsheet.dataRows.each { Row row ->
                Cell c = row.getCell(column)
                if (c.text) {
                    cells.add(row.getCell(column))
                }
            }
            cells.groupBy { it.text }.each { String text, List<Cell> cells1 ->
                context.addProblem(cells1 as Set, LogLevel.INFO, "Comment/Note (${column.headerCell.text}): ${text}", "Comment/Note.")
            }
        }
    }
}
