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

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.AbstractMetadataValidationContext
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.bam.BamMetadataValidationContext
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.bam.BamMetadataValidator
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidator
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.util.spreadsheet.validation.*

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.*

@Component
class SeqTypeLibraryLayoutValidator extends ValueTuplesValidator<AbstractMetadataValidationContext> implements MetadataValidator, BamMetadataValidator {

    @Autowired
    SeqTypeService seqTypeService

    @Override
    Collection<String> getDescriptions() {
        return ['The combination of sequencing type, sequencing read type and single cell is registered in the OTP database.']
    }

    @Override
    List<String> getRequiredColumnTitles(AbstractMetadataValidationContext context) {
        return [SEQUENCING_TYPE, SEQUENCING_READ_TYPE]*.name()
    }

    @Override
    List<String> getOptionalColumnTitles(AbstractMetadataValidationContext context) {
        return context instanceof BamMetadataValidationContext ? [] : [MetaDataColumn.BASE_MATERIAL.name()]
    }

    @Override
    void checkMissingOptionalColumn(AbstractMetadataValidationContext context, String columnTitle) {
    }

    @Override
    void validateValueTuples(AbstractMetadataValidationContext context, Collection<ValueTuple> valueTuples) {
        List<SeqType> seqTypes = []
        valueTuples.each {
            String seqTypeName
            if (context instanceof BamMetadataValidationContext) {
                seqTypeName = it.getValue(SEQUENCING_TYPE.name())
            } else {
                seqTypeName = MetadataImportService.getSeqTypeNameFromMetadata(it)
            }

            String baseMaterial = it.getValue(BASE_MATERIAL.name())
            boolean isSingleCell = SeqTypeService.isSingleCell(baseMaterial)

            SequencingReadType libraryLayout = SequencingReadType.getByName(it.getValue(SEQUENCING_READ_TYPE.name()))

            if (seqTypeName &&
                    libraryLayout &&
                    seqTypeService.findByNameOrImportAlias(seqTypeName) &&
                    CollectionUtils.notEmpty(SeqType.findAllByLibraryLayout(libraryLayout))) {
                SeqType seqType = seqTypeService.findByNameOrImportAlias(seqTypeName, [libraryLayout: libraryLayout, singleCell: isSingleCell])
                if (seqType) {
                    seqTypes << seqType
                } else {
                    String msgStart = "The combination of sequencing type '${seqTypeName}' and sequencing read type '${libraryLayout}' and"
                    String msgDefaultStart = "At least one combination of sequencing type and sequencing read type and"
                    if (isSingleCell) {
                        context.addProblem(it.cells, LogLevel.ERROR, "${msgStart} Single Cell is not registered in the OTP database.",
                                "${msgDefaultStart} Single Cell is not registered in the OTP database.")
                    } else {
                        context.addProblem(it.cells, LogLevel.ERROR, "${msgStart} without Single Cell is not registered in the OTP database.",
                                "${msgDefaultStart} without Single Cell is not registered in the OTP database.")
                    }
                }
            }
        }
        if (seqTypes) {
            context.addProblem([] as Set, LogLevel.INFO, "The submission contains following seqTypes:\n- ${seqTypes*.toString().sort().unique().join('\n- ')}")
        }
    }
}
