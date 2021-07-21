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

import de.dkfz.tbi.otp.ngsdata.IlseSubmission
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidator
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.util.spreadsheet.validation.*

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.ILSE_NO

@Component
class IlseNumberValidator extends ValueTuplesValidator<MetadataValidationContext> implements MetadataValidator {

    final static String ILSE_RANGE = "[${IlseSubmission.MIN_ILSE_VALUE}..${IlseSubmission.MAX_ILSE_NUMBER}]"

    @Override
    Collection<String> getDescriptions() {
        return ["The ILSe number is valid.",
                "All rows have the same value in the column '${ILSE_NO.name()}'.",
                "The ILSe number appears in the path of the metadata file.",
                "The ILSe is not registered yet in OTP.",
        ]
    }

    @Override
    List<String> getRequiredColumnTitles(MetadataValidationContext context) {
        return [ILSE_NO]*.name()
    }

    @Override
    void checkMissingRequiredColumn(MetadataValidationContext context, String columnTitle) { }

    @Override
    void validateValueTuples(MetadataValidationContext context, Collection<ValueTuple> allValueTuples) {
        if (allValueTuples) {
            Map<String, List<ValueTuple>> allIlseNumbers = allValueTuples.groupBy { it.getValue(ILSE_NO.name()) }
            if (allIlseNumbers.size() != 1) {
                context.addProblem((Set) allValueTuples.cells.sum(), LogLevel.INFO, "There are multiple ILSe numbers in the metadata file.")
            }
            allIlseNumbers.each { String ilseNo, List<ValueTuple> valueTuplesOfIlseNo ->
                ValueTuple tuple = CollectionUtils.exactlyOneElement(valueTuplesOfIlseNo)
                if (ilseNo != "") {
                    if (!ilseNo.isInteger()) {
                        context.addProblem(tuple.cells, LogLevel.ERROR, "The ILSe number '${ilseNo}' is not an integer.", "At least one ILSe number is not an integer.")
                    } else if ((ilseNo as int) < IlseSubmission.MIN_ILSE_VALUE || (ilseNo as int) > IlseSubmission.MAX_ILSE_NUMBER) {
                        context.addProblem(tuple.cells, LogLevel.ERROR, "The ILSe number '${ilseNo}' is out of range ${ILSE_RANGE}.", "At least one ILSe number is out of range ${ILSE_RANGE}.")
                    } else if (IlseSubmission.findByIlseNumber(ilseNo as int)) {
                        context.addProblem(tuple.cells, LogLevel.WARNING, "The ILSe number '${ilseNo}' already exists.", "At least one ILSe number already exists.")
                    }
                    if (!context.metadataFile.toString().contains(ilseNo)) {
                        context.addProblem(tuple.cells, LogLevel.WARNING, "The metadata file path '${context.metadataFile}' does not contain the ILSe number '${ilseNo}'.", "At least one metadata file path does not contain the ILSe number.")
                    }
                }
            }
        }
    }
}
