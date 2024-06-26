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

import groovy.transform.CompileDynamic
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidator
import de.dkfz.tbi.otp.utils.TimeFormats
import de.dkfz.tbi.otp.utils.spreadsheet.Cell
import de.dkfz.tbi.otp.utils.spreadsheet.validation.LogLevel
import de.dkfz.tbi.otp.utils.spreadsheet.validation.AbstractSingleValueValidator

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.RUN_DATE

@Component
class RunDateValidator extends AbstractSingleValueValidator<MetadataValidationContext> implements MetadataValidator {

    /**
     * The date format used in the run names.
     *
     * Since it is part of the interface, it is hardcoded here directly instead of referencing to another place.
     */
    static final String RUN_DATE_FORMAT = TimeFormats.DATE.format

    static final DateTimeFormatter RUN_DATE_FORMATTER = DateTimeFormatter.ofPattern(RUN_DATE_FORMAT)

    @CompileDynamic
    @Override
    Collection<String> getDescriptions() {
        return [
                "The run date has the ${RUN_DATE_FORMAT} format.",
                "The run date must not be from the future.",
        ]
    }

    @Override
    String getColumnTitle(MetadataValidationContext context) {
        return RUN_DATE.name()
    }

    @Override
    void validateValue(MetadataValidationContext context, String runDate, Set<Cell> cells) {
        try {
            LocalDate date = LocalDate.parse(runDate, RUN_DATE_FORMATTER)
            if (date > LocalDate.now().plusDays(1)) {
                context.addProblem(cells, LogLevel.ERROR, "The run date '${runDate}' must not be from the future.",
                        "No run date may be from the future.")
            }
        } catch (DateTimeParseException | IllegalArgumentException e) {
            context.addProblem(cells, LogLevel.ERROR,
                    "The format of the run date '${runDate}' is invalid, it must match ${RUN_DATE_FORMAT}.",
                    "The format of at least one run date is invalid, it must match ${RUN_DATE_FORMAT}.")
        }
    }
}
