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

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.ngsdata.AntibodyTargetService
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidator
import de.dkfz.tbi.util.spreadsheet.Cell
import de.dkfz.tbi.util.spreadsheet.validation.LogLevel
import de.dkfz.tbi.util.spreadsheet.validation.SingleValueValidator

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.ANTIBODY_TARGET

@Component
class AntibodyTargetValidator extends SingleValueValidator<MetadataValidationContext> implements MetadataValidator {

    @Autowired
    AntibodyTargetService antibodyTargetService

    @Override
    Collection<String> getDescriptions() {
        return ["If the antibody target is given, it must be registered in the OTP database (case-insensitive)."]
    }

    @Override
    String getColumnTitle(MetadataValidationContext context) {
        return ANTIBODY_TARGET.name()
    }

    @Override
    void checkColumn(MetadataValidationContext context) {
    }

    @Override
    void validateValue(MetadataValidationContext context, String antibodyTarget, Set<Cell> cells) {
        if (antibodyTarget && !antibodyTargetService.findByNameOrImportAlias(antibodyTarget)) {
            context.addProblem(cells, LogLevel.ERROR, "The antibody target '${antibodyTarget}' is not registered in OTP.",
                    "At least one antibody target is not registered in OTP.")
        }
    }
}
