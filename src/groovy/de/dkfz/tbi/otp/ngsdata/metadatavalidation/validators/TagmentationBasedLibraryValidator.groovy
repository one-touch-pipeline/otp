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

package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidator
import de.dkfz.tbi.util.spreadsheet.Cell
import de.dkfz.tbi.util.spreadsheet.validation.Level
import de.dkfz.tbi.util.spreadsheet.validation.SingleValueValidator

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.TAGMENTATION_BASED_LIBRARY

@Component
class TagmentationBasedLibraryValidator extends SingleValueValidator<MetadataValidationContext> implements MetadataValidator {

    @Override
    Collection<String> getDescriptions() {
        return ['The tagmentation based library value is registered in the OTP database.']
    }

    @Override
    String getColumnTitle(MetadataValidationContext context) {
        return TAGMENTATION_BASED_LIBRARY.name()
    }

    @Override
    void columnMissing(MetadataValidationContext context) {
        optionalColumnMissing(context, TAGMENTATION_BASED_LIBRARY.name())
    }

    @Override
    void validateValue(MetadataValidationContext context, String tagmentationBasedLibrary, Set<Cell> cells) {
        if (!(tagmentationBasedLibrary.toLowerCase() in ["", "1", "true", "false"])) {
           context.addProblem(cells, Level.ERROR, "The tagmentation based library column value should be '1', 'true', 'false' or an empty string instead of '${tagmentationBasedLibrary}'.", "The tagmentation based library column value should be '1', 'true', 'false' or an empty string.")
        }
    }
}
