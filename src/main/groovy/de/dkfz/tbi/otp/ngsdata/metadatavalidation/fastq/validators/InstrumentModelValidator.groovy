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

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.ngsdata.MetaDataColumn
import de.dkfz.tbi.otp.ngsdata.SeqPlatformModelLabelService
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidator
import de.dkfz.tbi.util.spreadsheet.Cell
import de.dkfz.tbi.util.spreadsheet.validation.LogLevel
import de.dkfz.tbi.util.spreadsheet.validation.AbstractSingleValueValidator

@Component
class InstrumentModelValidator extends AbstractSingleValueValidator<MetadataValidationContext> implements MetadataValidator {

    @Autowired
    SeqPlatformModelLabelService seqPlatformModelLabelService

    @Override
    Collection<String> getDescriptions() {
        return ['The instrument model is registered in the OTP database.']
    }

    @Override
    String getColumnTitle(MetadataValidationContext context) {
        return MetaDataColumn.INSTRUMENT_MODEL.name()
    }

    @Override
    void validateValue(MetadataValidationContext context, String seqPlatformModelLabelNameOrAlias, Set<Cell> cells) {
        if (!seqPlatformModelLabelNameOrAlias) {
            context.addProblem(cells, LogLevel.ERROR, "Instrument model must not be empty.", "At least one Instrument model is empty.")
        } else if (!seqPlatformModelLabelService.findByNameOrImportAlias(seqPlatformModelLabelNameOrAlias)) {
            context.addProblem(cells, LogLevel.ERROR, "Instrument model '${seqPlatformModelLabelNameOrAlias}' is not registered in the OTP database.", "At least one instrument model is not registered in the OTP database.")
        }
    }
}
