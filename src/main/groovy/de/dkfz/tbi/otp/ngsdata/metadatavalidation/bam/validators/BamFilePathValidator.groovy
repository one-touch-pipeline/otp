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
package de.dkfz.tbi.otp.ngsdata.metadatavalidation.bam.validators

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.ngsdata.BamMetadataColumn
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.bam.BamMetadataValidationContext
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.bam.BamMetadataValidator
import de.dkfz.tbi.otp.utils.validation.OtpPathValidator
import de.dkfz.tbi.otp.utils.spreadsheet.Cell
import de.dkfz.tbi.otp.utils.spreadsheet.validation.LogLevel
import de.dkfz.tbi.otp.utils.spreadsheet.validation.AbstractSingleValueValidator

import java.nio.file.Files
import java.nio.file.Path

@Component
class BamFilePathValidator extends AbstractSingleValueValidator<BamMetadataValidationContext> implements BamMetadataValidator {

    @Autowired
    FileService fileService

    @Override
    Collection<String> getDescriptions() {
        return [
                "The bam file must be an absolute path",
                "The bam file path must end with '.bam'.",
                "The bam file path must contain only legal characters.",
        ]
    }

    @Override
    String getColumnTitle(BamMetadataValidationContext context) {
        return BamMetadataColumn.BAM_FILE_PATH.name()
    }

    @Override
    void validateValue(BamMetadataValidationContext context, String filePath, Set<Cell> cells) {
        if (!filePath.endsWith(".bam")) {
            context.addProblem(cells, LogLevel.ERROR, "Filename '${filePath}' does not end with '.bam'.", "At least one filename does not end with '.bam'.")
        }
        if (OtpPathValidator.isValidAbsolutePath(filePath)) {
            try {
                Path bamFile = context.fileSystem.getPath(filePath)
                if (!Files.isRegularFile(bamFile)) {
                    if (Files.exists(bamFile)) {
                        context.addProblem(cells, LogLevel.ERROR, "'${filePath}' is not a file.", "At least one file is not a file.")
                    } else {
                        context.addProblem(cells, LogLevel.ERROR, "'${filePath}' does not exist or cannot be accessed by OTP.",
                                "At least one file does not exist or cannot be accessed by OTP.")
                    }
                } else if (!fileService.fileIsReadable(bamFile)) {
                    context.addProblem(cells, LogLevel.ERROR, "'${filePath}' is not readable.", "At least one file is not readable.")
                }
            } catch (Exception e) {
                context.addProblem(Collections.emptySet(), LogLevel.ERROR, e.message)
            }
       } else {
            context.addProblem(cells, LogLevel.ERROR, "The path '${filePath}' is no absolute path.", "At least one path is no absolute path.")
        }
    }
}
