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

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidator
import de.dkfz.tbi.util.spreadsheet.validation.*

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.*

@Component
class LowCoverageRequestedValidator extends AbstractValueTuplesValidator<MetadataValidationContext> implements MetadataValidator {
    static final List<String> VALID_VALUES = ["true", "false"]
    static final List<String> EMPTY_VALUES = [null, ""]

    @Autowired
    MetadataImportService metadataImportService

    @Autowired
    LibraryPreparationKitService libraryPreparationKitService

    @Autowired
    ValidatorHelperService validatorHelperService

    @Override
    Collection<String> getDescriptions() {
        return ["Checks if the value in the LOW_COVERAGE_REQUESTED column is a valid value [true/false/empty/missing]."]
    }

    @Override
    List<String> getRequiredColumnTitles(MetadataValidationContext context) {
        return [SEQUENCING_TYPE, SEQUENCING_READ_TYPE, LOW_COVERAGE_REQUESTED]*.name()
    }

    @Override
    List<String> getOptionalColumnTitles(MetadataValidationContext context) {
        return [BASE_MATERIAL.name()]
    }

    @Override
    void checkMissingRequiredColumn(MetadataValidationContext context, String columnTitle) {
    }

    @Override
    void checkMissingOptionalColumn(MetadataValidationContext context, String columnTitle) {
    }

    @Override
    void validateValueTuples(MetadataValidationContext context, Collection<ValueTuple> valueTuples) {
        valueTuples.each { ValueTuple valueTuple ->
            validateValueTuple(context, valueTuple)
        }
    }

    void validateValueTuple(MetadataValidationContext context, ValueTuple valueTuple) {
        SeqType seqType = validatorHelperService.getSeqTypeFromMetadata(valueTuple)
        String lowCov = valueTuple.getValue(LOW_COVERAGE_REQUESTED.name())?.toLowerCase()

        if (!seqType) {
            return
        }

        if (EMPTY_VALUES.contains(lowCov)) {
            return
        }

        if (VALID_VALUES.contains(lowCov)) {
            if (!seqType.isWgs()) {
                context.addProblem(valueTuple.cells, LogLevel.ERROR, "Low coverage value is set but the sequencing type '${seqType.displayNameWithLibraryLayout}' is not from Type '${SeqTypeNames.WHOLE_GENOME.seqTypeName}'", "low coverage set for non ${SeqTypeNames.WHOLE_GENOME.seqTypeName} data")
            }
        } else {
            context.addProblem(valueTuple.cells, LogLevel.ERROR, "The value '${lowCov}' is not a valid value " +
                    "for the ${LOW_COVERAGE_REQUESTED.name()} column of the metadata table. It needs to be [true/false/empty/missing column].", "invalid value for low coverage column")
        }
    }
}
