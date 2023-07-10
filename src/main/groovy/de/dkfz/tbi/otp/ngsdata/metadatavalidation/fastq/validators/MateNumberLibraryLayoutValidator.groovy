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

import de.dkfz.tbi.otp.ngsdata.SequencingReadType
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidator
import de.dkfz.tbi.util.spreadsheet.validation.*

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.READ
import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.SEQUENCING_READ_TYPE

@Component
class MateNumberLibraryLayoutValidator extends AbstractValueTuplesValidator<MetadataValidationContext> implements MetadataValidator {

    @Override
    Collection<String> getDescriptions() {
        return ['The mate number is inside the valid range for the sequencing layout (SINGLE = 1, PAIRED = 1,2).']
    }

    @Override
    List<String> getRequiredColumnTitles(MetadataValidationContext context) {
        return [READ, SEQUENCING_READ_TYPE]*.name()
    }

    @Override
    void checkMissingRequiredColumn(MetadataValidationContext context, String columnTitle) {
        if (columnTitle == SEQUENCING_READ_TYPE.name()) {
            addErrorForMissingRequiredColumn(context, columnTitle)
        }
    }

    @Override
    void validateValueTuples(MetadataValidationContext context, Collection<ValueTuple> valueTuples) {
        valueTuples.each {
            String libraryLayoutName = it.getValue(SEQUENCING_READ_TYPE.name())
            String mateNumber = it.getValue(READ.name())
            if (libraryLayoutName && mateNumber && mateNumber.isInteger()) {
                SequencingReadType libraryLayout = SequencingReadType.values().find {
                    it.toString() == libraryLayoutName
                }
                if (libraryLayout) {
                    int mate = mateNumber.toInteger()
                    int mateCount = libraryLayout.mateCount
                    if (mate > mateCount) {
                        context.addProblem(it.cells, LogLevel.ERROR, "The mate number '${mateNumber}' is bigger then the allowed value for the sequencing read type '${libraryLayoutName}' of '${mateCount}'.", "At least one mate number is bigger then the allowed value for the sequencing read type.")
                    }
                } else {
                    context.addProblem(it.cells, LogLevel.WARNING, "OTP does not know the sequencing read type '${libraryLayoutName}' and can therefore not validate the mate number.", "OTP does not know at least one sequencing read type and can therefore not validate the mate number.")
                }
            }
        }
    }
}
