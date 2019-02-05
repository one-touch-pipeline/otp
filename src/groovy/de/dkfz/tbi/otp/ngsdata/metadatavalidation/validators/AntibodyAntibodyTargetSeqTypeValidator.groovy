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

package de.dkfz.tbi.otp.ngsdata.metadatavalidation.validators

import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.ngsdata.MetadataImportService
import de.dkfz.tbi.otp.ngsdata.SeqTypeNames
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidator
import de.dkfz.tbi.util.spreadsheet.validation.*

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.*

@Component
class AntibodyAntibodyTargetSeqTypeValidator extends ValueTuplesValidator<MetadataValidationContext> implements MetadataValidator {

    @Override
    Collection<String> getDescriptions() {
        return [
                "Antibody target must be given if the sequencing type is '${SeqTypeNames.CHIP_SEQ.seqTypeName}'.",
                "Antibody target and antibody should not be given if the sequencing type is not '${SeqTypeNames.CHIP_SEQ.seqTypeName}'.",
        ]
    }

    @Override
    List<String> getColumnTitles(MetadataValidationContext context) {
        return [ANTIBODY_TARGET.name(), ANTIBODY.name(), SEQUENCING_TYPE.name(), TAGMENTATION_BASED_LIBRARY.name()]
    }

    @Override
    boolean columnMissing(MetadataValidationContext context, String columnTitle) {
        if (columnTitle == ANTIBODY_TARGET.name() || columnTitle == ANTIBODY.name() || columnTitle == TAGMENTATION_BASED_LIBRARY.name()) {
            return true
        } else {
            mandatoryColumnMissing(context, columnTitle)
            return false
        }
    }

    @Override
    void validateValueTuples(MetadataValidationContext context, Collection<ValueTuple> valueTuples) {
        valueTuples.each { ValueTuple valueTuple ->
            String antibodyTarget = valueTuple.getValue(ANTIBODY_TARGET.name()) ?: ""
            String antibody = valueTuple.getValue(ANTIBODY.name()) ?: ""
            String seqType = MetadataImportService.getSeqTypeNameFromMetadata(valueTuple)

            if ((antibodyTarget || antibody) && seqType != SeqTypeNames.CHIP_SEQ.seqTypeName) {
                context.addProblem(valueTuple.cells, Level.WARNING, "Antibody target ('${antibodyTarget}') and/or antibody ('${antibody}') are/is provided although data is no ChIP seq data. OTP will ignore the values.", "Antibody target and/or antibody are/is provided although data is no ChIP seq data. OTP will ignore the values.")
            }
            if (seqType == SeqTypeNames.CHIP_SEQ.seqTypeName && !antibodyTarget) {
                context.addProblem(valueTuple.cells, Level.ERROR, "Antibody target is not provided although data is ChIP seq data.")
            }
        }
    }
}
