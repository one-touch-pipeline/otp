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

package de.dkfz.tbi.otp.ngsdata.metadatavalidation.bam.validators

import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.ngsdata.LibraryPreparationKit
import de.dkfz.tbi.otp.ngsdata.SeqTypeNames
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.bam.BamMetadataValidationContext
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.bam.BamMetadataValidator
import de.dkfz.tbi.util.spreadsheet.validation.*

import static de.dkfz.tbi.otp.ngsdata.BamMetadataColumn.LIBRARY_PREPARATION_KIT
import static de.dkfz.tbi.otp.ngsdata.BamMetadataColumn.SEQUENCING_TYPE

@Component
class LibraryPreparationKitValidator extends ValueTuplesValidator<BamMetadataValidationContext> implements BamMetadataValidator {

    @Override
    Collection<String> getDescriptions() {
        return ["The library preparation kit is registered in OTP"]
    }

    @Override
    List<String> getRequiredColumnTitles(BamMetadataValidationContext context) {
        return []
    }

    @Override
    List<String> getOptionalColumnTitles(BamMetadataValidationContext context) {
        return [LIBRARY_PREPARATION_KIT, SEQUENCING_TYPE]*.name()
    }

    @Override
    void checkMissingOptionalColumn(BamMetadataValidationContext context, String columnTitle) { }

    @Override
    void validateValueTuples(BamMetadataValidationContext context, Collection<ValueTuple> valueTuples) {
        valueTuples.each {
            String libraryPreparationKit = it.getValue(LIBRARY_PREPARATION_KIT.name())
            String seqType = it.getValue(SEQUENCING_TYPE.name())

            if (libraryPreparationKit) {
                if (!LibraryPreparationKit.findByName(libraryPreparationKit)) {
                    context.addProblem(it.cells, Level.ERROR, "The ${LIBRARY_PREPARATION_KIT} '${libraryPreparationKit}' is not registered in OTP.", "At least one ${LIBRARY_PREPARATION_KIT} is not registered in OTP.")
                }
            } else {
                if (seqType.toUpperCase() == SeqTypeNames.EXOME.seqTypeName) {
                    context.addProblem(it.cells, Level.WARNING, "The ${SEQUENCING_TYPE} is '${seqType}' but no ${LIBRARY_PREPARATION_KIT} is given. The ${LIBRARY_PREPARATION_KIT} is needed for Indel.", "If the ${SEQUENCING_TYPE} is '${SeqTypeNames.EXOME.seqTypeName}' the ${LIBRARY_PREPARATION_KIT} should be given. The ${LIBRARY_PREPARATION_KIT} is needed for Indel.")
                }
            }
        }
    }
}
