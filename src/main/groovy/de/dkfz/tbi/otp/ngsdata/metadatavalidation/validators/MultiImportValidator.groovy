/*
 * Copyright 2011-2023 The OTP authors
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

import de.dkfz.tbi.otp.ngsdata.RawSequenceFile
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.AbstractMetadataValidationContext
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.extractData.ExtractProjectSampleType
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.extractData.ProjectSampleType
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidator
import de.dkfz.tbi.util.spreadsheet.validation.*

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.*

@Component
class MultiImportValidator extends AbstractValueTuplesValidator<AbstractMetadataValidationContext> implements MetadataValidator, ExtractProjectSampleType {

    @Override
    Collection<String> getDescriptions() {
        return ["Files with both the same MD5 sum and same sampleType cannot be stored in a project twice."]
    }

    @Override
    List<String> getRequiredColumnTitles(AbstractMetadataValidationContext context) {
        return [MD5, SAMPLE_NAME, PROJECT]*.name()
    }

    @Override
    void checkMissingRequiredColumn(AbstractMetadataValidationContext context, String columnTitle) {
        // should not create the missing required column, that are part of another validators
    }

    @Override
    void checkMissingOptionalColumn(AbstractMetadataValidationContext context, String columnTitle) {
        // should not create the missing optional column, that are part of another validators
    }

    @Override
    void validateValueTuples(AbstractMetadataValidationContext context, Collection<ValueTuple> valueTuples) {
        valueTuples.each { ValueTuple valueTuple ->
            validateValueTuple(context, valueTuple)
        }
    }

    @CompileDynamic
    void validateValueTuple(AbstractMetadataValidationContext context, ValueTuple valueTuple) {
        String md5sum = valueTuple.getValue(MD5.name())

        ProjectSampleType projectSampleType = getProjectAndSampleTypeFromMetadata(valueTuple)
        if (!projectSampleType || !md5sum) {
            return
        }

        Integer result = RawSequenceFile.createCriteria().count {
            eq("fastqMd5sum", md5sum)
            seqTrack {
                sample {
                    individual {
                        eq("project", projectSampleType.project)
                    }
                    eq('sampleType', projectSampleType.sampleType)
                }
            }
        }

        if (result > 0) {
            context.addProblem(valueTuple.cells,
                    LogLevel.ERROR, "A file with the md5sum '${md5sum}' and sample type '${projectSampleType.sampleType.name}' already exists " +
                    "for project '${projectSampleType.project.name}'.",
                    "At least one file with the md5sum and sample type combination exists in the corresponding project.")
        }
    }
}
