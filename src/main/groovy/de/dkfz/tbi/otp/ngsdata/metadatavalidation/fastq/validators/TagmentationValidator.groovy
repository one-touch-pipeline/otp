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

import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidator
import de.dkfz.tbi.util.spreadsheet.Cell
import de.dkfz.tbi.util.spreadsheet.validation.Level
import de.dkfz.tbi.util.spreadsheet.validation.SingleValueValidator

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.TAGMENTATION

@Component
class TagmentationValidator extends SingleValueValidator<MetadataValidationContext> implements MetadataValidator {

    static final List<String> ALLOWED_VALUES = [
            '',
            '0',
            '1',
            'true',
            'false',
    ].asImmutable()

    @Override
    Collection<String> getDescriptions() {
        return ["The tagmentation value must be from the list: '${ALLOWED_VALUES.join("', '")}'."]
    }

    @Override
    String getColumnTitle(MetadataValidationContext context) {
        return TAGMENTATION.name()
    }

    @Override
    void checkColumn(MetadataValidationContext context) {
    }

    @Override
    void validateValue(MetadataValidationContext context, String tagmentation, Set<Cell> cells) {
        if (!(tagmentation.toLowerCase() in ALLOWED_VALUES)) {
            context.addProblem(cells, Level.ERROR,
                    "The tagmentation column value should be '${ALLOWED_VALUES.join("', '")}' instead of '${tagmentation}'.",
                    "The tagmentation column value should be '${ALLOWED_VALUES.join("', '")}'.")
        }
    }
}
