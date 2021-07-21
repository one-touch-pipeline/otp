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
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.bam.BamMetadataValidator
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidator
import de.dkfz.tbi.util.spreadsheet.validation.*

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.PROJECT
import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.SAMPLE_NAME

@Component
class ProjectInternalNoteValidator extends ValueTuplesValidator<ValidationContext> implements MetadataValidator, BamMetadataValidator {

    @Override
    Collection<String> getDescriptions() {
        return [
                "Shows the Internal Note of a Project.",
        ]
    }

    @Override
    List<String> getRequiredColumnTitles(ValidationContext context) {
        []
    }

    @Override
    List<String> getOptionalColumnTitles(ValidationContext context) {
        return [PROJECT, SAMPLE_NAME]*.name()
    }

    @Override
    void checkMissingOptionalColumn(ValidationContext context, String columnTitle) {
    }

    @Override
    void validateValueTuples(ValidationContext context, Collection<ValueTuple> valueTuples) {
        Set<Project> projects = []
        valueTuples.each {
            Project project = MetadataImportService.getProjectFromMetadata(it)
            if (project) {
                projects << project
            }
        }
        projects.each { Project project ->
            if (project && project.internalNotes) {
                context.addProblem(Collections.emptySet(), LogLevel.INFO, "Internal notes for project '${project.name}': ${project.internalNotes}", "A Project has internal notes")
            }
        }
    }
}
