/*
 * Copyright 2011-2026 The OTP authors
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
package de.dkfz.tbi.otp.ngsdata.metadatavalidation.bam.validators

import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.ngsdata.BamMetadataColumn
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.bam.BamMetadataValidationContext
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.bam.BamMetadataValidator
import de.dkfz.tbi.otp.utils.spreadsheet.Cell
import de.dkfz.tbi.otp.utils.spreadsheet.validation.*

@Component
class MaximalReadLengthValidator extends AbstractSingleValueValidator implements BamMetadataValidator {
    @Override
    Collection<String> getDescriptions() {
        return [
                "MaximalReadLength must be an integer number",
                "If the source files should only link, the MaximalReadLength is required.",
        ]
    }

    @Override
    String getColumnTitle(ValidationContext context) {
        return BamMetadataColumn.MAXIMAL_READ_LENGTH.name()
    }

    @Override
    void checkColumn(ValidationContext context) {
        BamMetadataValidationContext bamContext = context as BamMetadataValidationContext
        if (bamContext.linkSourceFiles) {
            context.addProblem(Collections.emptySet(), LogLevel.ERROR,
                    "If source files should only linked, the column '${BamMetadataColumn.MAXIMAL_READ_LENGTH.name()}' is required.")
        } else {
            addWarningForMissingOptionalColumn(context, BamMetadataColumn.MAXIMAL_READ_LENGTH.name())
        }
    }

    @Override
    void validateValue(ValidationContext context, String maximalReadLength, Set<Cell> cells) {
        BamMetadataValidationContext bamContext = context as BamMetadataValidationContext
        if (maximalReadLength) {
            if (!maximalReadLength.integer) {
                context.addProblem(cells, LogLevel.ERROR, "The maximalReadLength '${maximalReadLength}' should be an integer number.",
                        "At least one maximalReadLength is not an integer number.")
            }
        } else if (bamContext.linkSourceFiles) {
            context.addProblem(cells, LogLevel.ERROR, "The maximalReadLength is required, if the files should only be linked")
        }
    }
}
