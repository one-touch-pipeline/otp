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

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidator
import de.dkfz.tbi.otp.utils.spreadsheet.validation.*

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.*

@Component
class AntibodyAntibodyTargetSeqTypeValidator extends AbstractValueTuplesValidator<MetadataValidationContext> implements MetadataValidator {

    @Autowired
    SeqTypeService seqTypeService

    @Autowired
    ValidatorHelperService validatorHelperService

    @CompileDynamic
    @Override
    Collection<String> getDescriptions() {
        String seqTypeString = seqTypesStringWithAntibody()
        return [
                "Antibody target must be given if the sequencing type is ${seqTypeString}.",
                "Antibody target and antibody should not be given if the sequencing type is not ${seqTypeString}.",
        ]
    }

    private String seqTypesStringWithAntibody() {
        return seqTypeService.seqTypesWithAntibodyTarget.collect {
            "'${it}'".toString()
        }.sort().join(', ')
    }

    @Override
    List<String> getRequiredColumnTitles(MetadataValidationContext context) {
        return [SEQUENCING_TYPE, SEQUENCING_READ_TYPE]*.name()
    }

    @Override
    List<String> getOptionalColumnTitles(MetadataValidationContext context) {
        return [ANTIBODY_TARGET, ANTIBODY, BASE_MATERIAL]*.name()
    }

    @Override
    void checkMissingOptionalColumn(MetadataValidationContext context, String columnTitle) {
    }

    @Override
    void validateValueTuples(MetadataValidationContext context, Collection<ValueTuple> valueTuples) {
        valueTuples.each { ValueTuple valueTuple ->
            String antibodyTarget = valueTuple.getValue(ANTIBODY_TARGET.name()) ?: ""
            String antibody = valueTuple.getValue(ANTIBODY.name()) ?: ""
            String seqTypeName = validatorHelperService.getSeqTypeNameFromMetadata(valueTuple)

            String baseMaterial = valueTuple.getValue(BASE_MATERIAL.name())
            boolean isSingleCell = SeqTypeService.isSingleCell(baseMaterial)

            SequencingReadType libraryLayout = SequencingReadType.getByName(valueTuple.getValue(SEQUENCING_READ_TYPE.name()))

            SeqType seqType = seqTypeService.findByNameOrImportAlias(seqTypeName, [libraryLayout: libraryLayout, singleCell: isSingleCell])

            if (!seqType) {
                return
            }

            if ((antibodyTarget || antibody) && !seqType.hasAntibodyTarget) {
                context.addProblem(valueTuple.cells, LogLevel.WARNING, "Antibody target ('${antibodyTarget}') and/or antibody ('${antibody}') are/is provided although the SeqType '${seqType} do not support it. OTP will ignore the values.", "Antibody target and/or antibody are/is provided for an SeqType not supporting it. OTP will ignore the values.")
            }
            if (seqType.hasAntibodyTarget && !antibodyTarget) {
                context.addProblem(valueTuple.cells, LogLevel.ERROR, "Antibody target is not provided although the SeqType '${seqType}' require it.", "Antibody target is not provided for SeqType require AntibodyTarget")
            }
        }
    }
}
