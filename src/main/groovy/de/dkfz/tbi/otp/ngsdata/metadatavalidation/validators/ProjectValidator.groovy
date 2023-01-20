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

import de.dkfz.tbi.otp.ngsdata.BamMetadataColumn
import de.dkfz.tbi.otp.ngsdata.MetaDataColumn
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.AbstractMetadataValidationContext
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.bam.BamMetadataValidationContext
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.bam.BamMetadataValidator
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidator
import de.dkfz.tbi.otp.project.ProjectService
import de.dkfz.tbi.util.spreadsheet.Cell
import de.dkfz.tbi.util.spreadsheet.validation.LogLevel
import de.dkfz.tbi.util.spreadsheet.validation.SingleValueValidator

@Component
class ProjectValidator extends SingleValueValidator<AbstractMetadataValidationContext> implements MetadataValidator, BamMetadataValidator {

    @CompileDynamic
    @Override
    Collection<String> getDescriptions() {
        return ["The project is registered in OTP or the column ${MetaDataColumn.PROJECT} does not exist.",]
    }

    @Override
    String getColumnTitle(AbstractMetadataValidationContext context) {
        return MetaDataColumn.PROJECT.name()
    }

    @Override
    void checkColumn(AbstractMetadataValidationContext context) {
        if (context instanceof BamMetadataValidationContext) {
            addErrorForMissingRequiredColumn(context, BamMetadataColumn.PROJECT.name())
        }
    }

    @Override
    void validateValue(AbstractMetadataValidationContext context, String projectName, Set<Cell> cells) {
        if (!ProjectService.findByNameOrNameInMetadataFiles(projectName)) {
            def level
            if (context instanceof BamMetadataValidationContext) {
                level = LogLevel.ERROR
            } else {
                level = LogLevel.WARNING
            }
            context.addProblem(cells, level, "The project '${projectName}' is not registered in OTP.", "At least one project is not registered in OTP.")
        }
    }
}
