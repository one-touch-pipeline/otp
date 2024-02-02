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
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.InformationReliability
import de.dkfz.tbi.otp.ngsdata.LibraryPreparationKitService
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidator
import de.dkfz.tbi.util.spreadsheet.Cell
import de.dkfz.tbi.util.spreadsheet.validation.LogLevel
import de.dkfz.tbi.util.spreadsheet.validation.AbstractSingleValueValidator

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.LIB_PREP_KIT

@Component
class LibPrepKitValidator extends AbstractSingleValueValidator<MetadataValidationContext> implements MetadataValidator {

    @Autowired
    LibraryPreparationKitService libraryPreparationKitService

    @CompileDynamic
    @Override
    Collection<String> getDescriptions() {
        return ["The library preparation kit is registered in the OTP database or '${InformationReliability.UNKNOWN_VERIFIED.rawValue}' or empty."]
    }

    @Override
    String getColumnTitle(MetadataValidationContext context) {
        return LIB_PREP_KIT.name()
    }

    @Override
    void checkColumn(MetadataValidationContext context) {
        addWarningForMissingOptionalColumn(context, LIB_PREP_KIT.name())
    }

    @Override
    void validateValue(MetadataValidationContext context, String value, Set<Cell> cells) {
        if (value == "" || value == InformationReliability.UNKNOWN_VERIFIED.rawValue) {
            context.addProblem(cells, LogLevel.WARNING, "The library preparation kit column is ${value ?: 'empty'}", "At least one library preparation kit is ${value ?: 'empty'}")
        } else if (!libraryPreparationKitService.findByNameOrImportAlias(value)) {
            context.addProblem(cells, LogLevel.ERROR, "The library preparation kit '${value}' is neither registered in the OTP database nor '${InformationReliability.UNKNOWN_VERIFIED.rawValue}' nor empty.", "At least one library preparation kit is neither registered in the OTP database nor '${InformationReliability.UNKNOWN_VERIFIED.rawValue}' nor empty.")
        }
    }
}
