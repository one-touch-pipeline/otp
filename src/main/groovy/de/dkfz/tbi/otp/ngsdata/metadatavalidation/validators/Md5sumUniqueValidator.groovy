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

import groovy.transform.CompileDynamic
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.dataprocessing.ExternallyProcessedBamFile
import de.dkfz.tbi.otp.ngsdata.DataFile
import de.dkfz.tbi.otp.ngsdata.MetaDataColumn
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.AbstractMetadataValidationContext
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.bam.BamMetadataValidationContext
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.bam.BamMetadataValidator
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidator
import de.dkfz.tbi.util.spreadsheet.validation.*

@Component
class Md5sumUniqueValidator extends ValueTuplesValidator<AbstractMetadataValidationContext> implements MetadataValidator, BamMetadataValidator {

    @Override
    Collection<String> getDescriptions() {
        return [
                'Each MD5 sum is unique.',
        ]
    }

    @Override
    List<String> getRequiredColumnTitles(AbstractMetadataValidationContext context) {
        return [MetaDataColumn.MD5.name()]
    }

    @Override
    void checkMissingRequiredColumn(AbstractMetadataValidationContext context, String columnTitle) {
    }

    @CompileDynamic
    @Override
    void validateValueTuples(AbstractMetadataValidationContext context, Collection<ValueTuple> valueTuples) {
        valueTuples.groupBy {
            it.getValue(MetaDataColumn.MD5.name()).toLowerCase(Locale.ENGLISH)
        }.each { String md5sum, Collection<ValueTuple> valueTuplesOfMd5sum ->
            if (valueTuplesOfMd5sum*.cells.sum().size() > 1) {
                context.addProblem(valueTuplesOfMd5sum*.cells.sum(), LogLevel.WARNING, "The MD5 sum '${md5sum}' is not unique in the metadata file.", "At least one MD5 sum is not unique in the metadata file.")
            }
            if (context instanceof BamMetadataValidationContext) {
                if (ExternallyProcessedBamFile.findAllByMd5sum(md5sum)) {
                    context.addProblem(valueTuplesOfMd5sum*.cells.sum(), LogLevel.WARNING, "A bam file with the MD5 sum '${md5sum}' is already registered in OTP.", "At least one bam file has a MD5 sum is already registered in OTP.")
                }
            } else {
                if (DataFile.findAllByMd5sum(md5sum)) {
                    context.addProblem(valueTuplesOfMd5sum*.cells.sum(), LogLevel.WARNING, "A fastq file with the MD5 sum '${md5sum}' is already registered in OTP.", "At least one fastq file has a MD5 sum which is already registered in OTP.")
                }
            }
        }
    }
}
