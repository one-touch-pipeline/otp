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

import groovy.transform.CompileDynamic
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidator
import de.dkfz.tbi.util.spreadsheet.Cell
import de.dkfz.tbi.util.spreadsheet.validation.LogLevel
import de.dkfz.tbi.util.spreadsheet.validation.SingleValueValidator

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.WITHDRAWN_DATE

@Component
class WithdrawnDateValidator extends SingleValueValidator<MetadataValidationContext> implements MetadataValidator {

    static final String NONE = 'NONE'

    @CompileDynamic
    @Override
    Collection<String> getDescriptions() {
        return ["If a '${WITHDRAWN_DATE}' column exists, it must only contain cells which are empty or contain 'None'."]
    }

    @Override
    String getColumnTitle(MetadataValidationContext context) {
        return WITHDRAWN_DATE.name()
    }

    @Override
    void checkColumn(MetadataValidationContext context) {
    }

    @Override
    void validateValue(MetadataValidationContext context, String value, Set<Cell> cells) {
        final String uppercaseValue = value.toUpperCase(Locale.ENGLISH)
        if (value != "" && uppercaseValue != NONE) {
            context.addProblem(cells, LogLevel.ERROR, "'${value}' is not an acceptable '${WITHDRAWN_DATE}' value. It must be empty or 'None'. Withdrawn data cannot be imported into OTP.", "Withdrawn data cannot be imported into OTP.")
        }
    }
}
