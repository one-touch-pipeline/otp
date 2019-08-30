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

import de.dkfz.tbi.otp.dataprocessing.OtpPath
import de.dkfz.tbi.otp.ngsdata.MetaDataColumn
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidator
import de.dkfz.tbi.util.spreadsheet.Cell
import de.dkfz.tbi.util.spreadsheet.validation.Level
import de.dkfz.tbi.util.spreadsheet.validation.SingleValueValidator

import java.util.regex.Matcher

@Component
class LibraryValidator extends SingleValueValidator<MetadataValidationContext> implements MetadataValidator {

    final String REGEX = /^(?:lib(?:[1-9]\d*|NA)|)$/

    @Override
    Collection<String> getDescriptions() {
        return ['The Library contains only valid characters.']
    }

    @Override
    String getColumnTitle(MetadataValidationContext context) {
        return MetaDataColumn.CUSTOMER_LIBRARY.name()
    }

    @Override
    void checkColumn(MetadataValidationContext context) { }

    @Override
    void validateValue(MetadataValidationContext context, String library, Set<Cell> cells) {
        if (library) {
            Matcher matcher = library =~ REGEX
            if (!OtpPath.isValidPathComponent(library)) {
                context.addProblem(cells, Level.ERROR, "Library '${library}' contains invalid characters.", "At least one library contains invalid characters.")
            } else if (!matcher) {
                context.addProblem(cells, Level.WARNING, "Library '${library}' does not match regular expression '${REGEX}'.", "At least one Library does not match regular expression '${REGEX}'.")
            }
        }
    }
}
