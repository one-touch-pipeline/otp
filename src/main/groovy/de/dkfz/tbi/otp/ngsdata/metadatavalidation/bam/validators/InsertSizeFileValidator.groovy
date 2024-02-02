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

import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.ngsdata.metadatavalidation.bam.BamMetadataValidationContext
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.bam.BamMetadataValidator
import de.dkfz.tbi.otp.utils.validation.OtpPathValidator
import de.dkfz.tbi.util.spreadsheet.validation.*

import java.nio.file.Files
import java.nio.file.Path

import static de.dkfz.tbi.otp.ngsdata.BamMetadataColumn.BAM_FILE_PATH
import static de.dkfz.tbi.otp.ngsdata.BamMetadataColumn.INSERT_SIZE_FILE

@Component
class InsertSizeFileValidator extends AbstractValueTuplesValidator<BamMetadataValidationContext> implements BamMetadataValidator {

    @Override
    Collection<String> getDescriptions() {
        return [
                "The insert size file must be an absolute path",
        ]
    }

    @Override
    List<String> getRequiredColumnTitles(BamMetadataValidationContext context) {
        return [INSERT_SIZE_FILE]*.name()
    }

    @Override
    List<String> getOptionalColumnTitles(BamMetadataValidationContext context) {
        return [BAM_FILE_PATH]*.name()
    }

    @Override
    void checkMissingRequiredColumn(BamMetadataValidationContext context, String columnTitle) {
        addWarningForMissingOptionalColumn(context, columnTitle, "'${INSERT_SIZE_FILE.name()}' has to be set for Sophia")
    }

    @Override
    void checkMissingOptionalColumn(BamMetadataValidationContext context, String columnTitle) {
    }

    @Override
    void validateValueTuples(BamMetadataValidationContext context, Collection<ValueTuple> valueTuples) {
        valueTuples.each {
            String bamFile = it.getValue(BAM_FILE_PATH.name())
            String insertSizeFile = it.getValue(INSERT_SIZE_FILE.name())

            if (insertSizeFile.isEmpty()) {
                context.addProblem(it.cells, LogLevel.WARNING, "${INSERT_SIZE_FILE} has to be set for Sophia",
                        "${INSERT_SIZE_FILE} file has to be set for Sophia")
            } else {
                if (OtpPathValidator.isValidRelativePath(insertSizeFile)) {
                    try {
                        if (bamFile != null && !bamFile.isEmpty()) {
                            Path bamFilePath = context.fileSystem.getPath(bamFile)
                            if (bamFilePath) {
                                if (OtpPathValidator.isValidAbsolutePath(bamFile)) {
                                    Path insertSizeFilePath = bamFilePath.resolveSibling(insertSizeFile)

                                    if (!Files.isRegularFile(insertSizeFilePath)) {
                                        if (Files.exists(insertSizeFilePath)) {
                                            context.addProblem(it.cells, LogLevel.ERROR,
                                                    "'${insertSizeFile}' is not a file.", "At least one file is not a file.")
                                        } else {
                                            context.addProblem(it.cells, LogLevel.ERROR,
                                                    "'${insertSizeFile}' does not exist or cannot be accessed by OTP.",
                                                    "At least one file does not exist or cannot be accessed by OTP.")
                                        }
                                    } else if (!Files.isReadable(insertSizeFilePath)) {
                                        context.addProblem(it.cells, LogLevel.ERROR,
                                                "'${insertSizeFile}' is not readable.", "At least one file is not readable.")
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        context.addProblem(Collections.emptySet(), LogLevel.ERROR, e.message)
                    }
                } else {
                    context.addProblem(it.cells, LogLevel.ERROR, "The path '${insertSizeFile}' is not a relative path.",
                            "At least one path is not a relative path.")
                }
            }
        }
    }
}
