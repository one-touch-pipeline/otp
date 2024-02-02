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

import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidator
import de.dkfz.tbi.util.spreadsheet.Cell
import de.dkfz.tbi.util.spreadsheet.validation.LogLevel
import de.dkfz.tbi.util.spreadsheet.validation.AbstractSingleValueValidator

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.LANE_NO

@Component
class LaneNumberValidator extends AbstractSingleValueValidator<MetadataValidationContext> implements MetadataValidator {

    @Override
    Collection<String> getDescriptions() {
        return [
                'The lane number should be a single digit in the range of 1 to 8.',
        ]
    }

    @Override
    String getColumnTitle(MetadataValidationContext context) {
        return LANE_NO.name()
    }

    @Override
    void validateValue(MetadataValidationContext context, String laneNumber, Set<Cell> cells) {
        if (laneNumber.empty) {
            context.addProblem(cells, LogLevel.ERROR, "The lane number must not be empty.")
        } else if (!(laneNumber ==~ /^[0-9a-zA-Z]+$/)) {
            context.addProblem(cells, LogLevel.ERROR, "'${laneNumber}' is not a well-formed lane number. It must contain only digits (0 to 9) and/or letters (a to z, A to Z). It should be a single digit in the range from 1 to 8.", "At least one lane number is not well-formed.")
        } else if (!(laneNumber ==~ /^[1-8]$/)) {
            context.addProblem(cells, LogLevel.WARNING, "'${laneNumber}' is not a well-formed lane number. It should be a single digit in the range from 1 to 8.", "At least one lane number is not well-formed.")
        }
    }
}
