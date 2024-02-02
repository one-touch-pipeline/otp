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
package de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.validators

import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.ngsdata.MetaDataColumn
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidator
import de.dkfz.tbi.util.spreadsheet.Cell
import de.dkfz.tbi.util.spreadsheet.validation.LogLevel
import de.dkfz.tbi.util.spreadsheet.validation.AbstractSingleValueValidator

@Component
class MateNumberValidator extends AbstractSingleValueValidator<MetadataValidationContext> implements MetadataValidator {

    static final private String MATE_NUMBER_EXPRESSION = /^(i|I)?[1-9]\d*$/

    static final String ALLOWED_VALUE_POSTFIX = "must be a positive integer (value >= 1), which may be prefixed by an 'I'/'i' to indicate, that it is an index file"

    static final String ERROR_NOT_PROVIDED = "The mate number must be provided and ${ALLOWED_VALUE_POSTFIX}"

    static final String ERROR_INVALID_VALUE_SUMMARY = "At least one mate number is not a positive integer number, probably prefixed by an 'I'/'i'"

    @Override
    Collection<String> getDescriptions() {
        return [
                ERROR_NOT_PROVIDED,
        ]
    }

    @Override
    String getColumnTitle(MetadataValidationContext context) {
        return MetaDataColumn.READ.name()
    }

    @Override
    void checkColumn(MetadataValidationContext context) {
        addErrorForMissingRequiredColumn(context, MetaDataColumn.READ.name())
    }

    @Override
    void validateValue(MetadataValidationContext context, String mateNumber, Set<Cell> cells) {
        if (!mateNumber) {
            context.addProblem(cells, LogLevel.ERROR, ERROR_NOT_PROVIDED)
        } else if (!(mateNumber ==~ MATE_NUMBER_EXPRESSION)) {
            context.addProblem(cells, LogLevel.ERROR, "The mate number ('${mateNumber}') ${ALLOWED_VALUE_POSTFIX}", ERROR_INVALID_VALUE_SUMMARY)
        }
    }
}
