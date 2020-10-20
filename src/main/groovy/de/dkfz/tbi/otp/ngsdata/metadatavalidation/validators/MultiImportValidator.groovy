/*
 * Copyright 2011-2020 The OTP authors
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

import de.dkfz.tbi.otp.ngsdata.DataFile
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.AbstractMetadataValidationContext
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.bam.BamMetadataValidator
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidator
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.util.spreadsheet.validation.*

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.*

@Component
class MultiImportValidator extends ValueTuplesValidator<AbstractMetadataValidationContext> implements MetadataValidator, BamMetadataValidator {

    @Override
    Collection<String> getDescriptions() {
        return ["Files with both the same MD5 sum and same sampleType cannot be stored in a project twice."]
    }

    @Override
    List<String> getRequiredColumnTitles(AbstractMetadataValidationContext context) {
        return [MD5, SAMPLE_TYPE, PROJECT]*.name()
    }

    @Override
    void validateValueTuples(AbstractMetadataValidationContext context, Collection<ValueTuple> valueTuples) {
        valueTuples.each { ValueTuple valueTuple ->
            validateValueTuple(context, valueTuple)
        }
    }

    void validateValueTuple(AbstractMetadataValidationContext context, ValueTuple valueTuple) {
        String projectName = valueTuple.getValue(PROJECT.name())
        String sampleTypeName = valueTuple.getValue(SAMPLE_TYPE.name())
        String md5sum = valueTuple.getValue(MD5.name())

        Project project = Project.getByNameOrNameInMetadataFiles(projectName)

        if (!project) {
            return
        }

        Integer result = DataFile.createCriteria().count {
            eq("md5sum", md5sum)
            seqTrack {
                sample {
                    individual {
                        eq("project", project)
                    }
                    sampleType {
                        eq("name", sampleTypeName.toLowerCase(Locale.ENGLISH))
                    }
                }
            }
        }

        if (result > 0) {
            context.addProblem(valueTuple.cells,
                    Level.ERROR, "A file with the md5sum '${md5sum}' and sample type '${sampleTypeName}' already exists for project '${project.name}'.", "At least one file with the md5sum and sample type combination exists in the corresponding project.")
        }
    }
}
