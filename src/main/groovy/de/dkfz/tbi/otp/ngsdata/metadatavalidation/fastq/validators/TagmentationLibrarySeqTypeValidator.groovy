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

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidator
import de.dkfz.tbi.util.spreadsheet.validation.*

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.*

@Component
class TagmentationLibrarySeqTypeValidator extends ValueTuplesValidator<MetadataValidationContext> implements MetadataValidator {

    static final String TAGMENTATION_WITHOUT_LIBRARY = "For tagmentation sequencing types there should be a value in the ${TAGMENTATION_LIBRARY} column."
    static final String LIBRARY_WITHOUT_TAGMENTATION = "At least one tagmentation library is given in ${TAGMENTATION_LIBRARY} for a non tagmentation sequencing type"

    @Override
    Collection<String> getDescriptions() {
        return ["If the sequencing type is '${SeqTypeNames.WHOLE_GENOME_BISULFITE_TAGMENTATION.seqTypeName}', " +
                        "the tagmentation library number (${TAGMENTATION_LIBRARY}) must be given."]
    }

    @Override
    List<String> getRequiredColumnTitles(MetadataValidationContext context) {
        return []
    }

    @Override
    List<String> getOptionalColumnTitles(MetadataValidationContext context) {
        return [TAGMENTATION_LIBRARY, SEQUENCING_TYPE, TAGMENTATION]*.name()
    }

    @Override
    void checkMissingRequiredColumn(MetadataValidationContext context, String columnTitle) { }

    @Override
    void checkMissingOptionalColumn(MetadataValidationContext context, String columnTitle) { }

    @Override
    void validateValueTuples(MetadataValidationContext context, Collection<ValueTuple> valueTuples) {
        valueTuples.each { ValueTuple valueTuple ->
            String seqType = MetadataImportService.getSeqTypeNameFromMetadata(valueTuple)
            String library = valueTuple.getValue(TAGMENTATION_LIBRARY.name())
            if (seqType.endsWith(SeqType.TAGMENTATION_SUFFIX)) {
                if (!library) {
                    context.addProblem(valueTuple.cells, Level.ERROR,
                            "For the tagmentation sequencing type '${seqType}' there should be a value in the ${TAGMENTATION_LIBRARY} column.",
                            TAGMENTATION_WITHOUT_LIBRARY)
                }
            } else {
                if (library) {
                    context.addProblem(valueTuple.cells, Level.WARNING,
                            "The tagmentation library '${library}' in column ${TAGMENTATION_LIBRARY} indicates tagmentation, " +
                                    "but the sequencing type '${seqType}' is without tagmentation",
                            LIBRARY_WITHOUT_TAGMENTATION)
                }
            }
        }
    }
}
