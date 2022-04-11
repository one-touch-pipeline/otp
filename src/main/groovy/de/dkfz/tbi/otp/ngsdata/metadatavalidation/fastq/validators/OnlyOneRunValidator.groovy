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
package de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.validators

import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.ngsdata.MetaDataColumn
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidator
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.directorystructures.DataFilesInGpcfSpecificStructure
import de.dkfz.tbi.util.spreadsheet.Cell
import de.dkfz.tbi.util.spreadsheet.validation.LogLevel

@Component
class OnlyOneRunValidator implements MetadataValidator {

    @Override
    Collection<String> getDescriptions() {
        return Collections.emptyList()
    }

    @Override
    void validate(MetadataValidationContext context) {
        List<Cell> runCells = context.spreadsheet.dataRows*.getCell(context.spreadsheet.getColumn(MetaDataColumn.RUN_ID.name()))
        List<String> runNames = runCells*.text.unique()
        List<Cell> ilseCells = context.spreadsheet.dataRows*.getCell(context.spreadsheet.getColumn(MetaDataColumn.ILSE_NO.name()))
        List<String> ilsen = ilseCells*.text.unique()

        if (runNames.size() > 1 &&
                !(ilsen.size() == 1 && ilsen != [""] && ilsen != [null] &&
                        context.directoryStructure instanceof DataFilesInGpcfSpecificStructure)
        ) {
            context.addProblem(runCells as Set, LogLevel.WARNING,
                    "Metadata file contains data from more than one run.")
        }
    }
}
