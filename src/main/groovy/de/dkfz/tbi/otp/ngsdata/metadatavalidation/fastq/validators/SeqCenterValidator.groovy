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

import de.dkfz.tbi.otp.ngsdata.SeqCenter
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidator
import de.dkfz.tbi.otp.utils.spreadsheet.Cell
import de.dkfz.tbi.otp.utils.spreadsheet.validation.LogLevel
import de.dkfz.tbi.otp.utils.spreadsheet.validation.AbstractSingleValueValidator

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.CENTER_NAME
import static de.dkfz.tbi.otp.utils.CollectionUtils.atMostOneElement

@Component
class SeqCenterValidator extends AbstractSingleValueValidator<MetadataValidationContext> implements MetadataValidator {

    @CompileDynamic
    @Override
    Collection<String> getDescriptions() {
        return ['The sequencing center is registered in the OTP database.']
    }

    @Override
    String getColumnTitle(MetadataValidationContext context) {
        return CENTER_NAME.name()
    }

    @CompileDynamic
    @Override
    void validateValue(MetadataValidationContext context, String centerName, Set<Cell> cells) {
        if (!atMostOneElement(SeqCenter.findAllByName(centerName))) {
            context.addProblem(cells, LogLevel.ERROR, "Sequencing center '${centerName}' is not registered in the OTP database.", "At least one sequencing center is not registered in the OTP database.")
        }
    }
}
