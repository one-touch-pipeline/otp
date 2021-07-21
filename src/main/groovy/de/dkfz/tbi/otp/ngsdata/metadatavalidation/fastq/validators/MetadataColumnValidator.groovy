/*
 * Copyright 2011-2020 The OTP authors
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
package de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.validators

import org.springframework.stereotype.Component
import de.dkfz.tbi.otp.ngsdata.MetaDataKey
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidator
import de.dkfz.tbi.util.spreadsheet.Cell
import de.dkfz.tbi.util.spreadsheet.Row
import de.dkfz.tbi.util.spreadsheet.validation.LogLevel

@Component
class MetadataColumnValidator implements MetadataValidator {

    @Override
    Collection<String> getDescriptions() {
        return ['The columns in the metadata file should be registered in OTP.']
    }

    @Override
    void validate(MetadataValidationContext context) {
        Row row = context.spreadsheet.header
        List<String> metadataColumnList = MetaDataKey.list()*.name

        Map<String, Set<Cell>> cellsByValue = row.cells.collectEntries { Cell cell ->
            [(cell.text): [cell] as Set]
        }

        cellsByValue.each { String value, Set<Cell> cells ->
            if (!metadataColumnList.contains(value)) {
                context.addProblem(cells, LogLevel.WARNING, "Column '${value}' is not registered in OTP.",
                        "At least one metadata column is not registered in OTP.")
            }
        }
    }
}
