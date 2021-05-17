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
package de.dkfz.tbi.otp.ngsdata.metadatavalidation.bam.validators

import groovy.json.JsonSlurper
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.ngsdata.metadatavalidation.bam.BamMetadataValidationContext
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.bam.BamMetadataValidator
import de.dkfz.tbi.otp.utils.validation.OtpPathValidator
import de.dkfz.tbi.util.spreadsheet.validation.*

import java.nio.file.Files
import java.nio.file.Path

import static de.dkfz.tbi.otp.ngsdata.BamMetadataColumn.BAM_FILE_PATH
import static de.dkfz.tbi.otp.ngsdata.BamMetadataColumn.QUALITY_CONTROL_FILE

@Component
class QualityControlFileValidator extends ValueTuplesValidator<BamMetadataValidationContext> implements BamMetadataValidator {

    @Override
    Collection<String> getDescriptions() {
        return [
                "The quality control file must be an relative path",
        ]
    }

    @Override
    List<String> getRequiredColumnTitles(BamMetadataValidationContext context) {
        return [QUALITY_CONTROL_FILE, BAM_FILE_PATH]*.name()
    }

    @Override
    void checkMissingRequiredColumn(BamMetadataValidationContext context, String columnTitle) {
        if (columnTitle == QUALITY_CONTROL_FILE.name())  {
            addWarningForMissingOptionalColumn(context, columnTitle, "'${QUALITY_CONTROL_FILE.name()}' has to be set for Sophia")
        } else {
            super.checkMissingRequiredColumn(context, columnTitle)
        }
    }

    @Override
    void checkMissingOptionalColumn(BamMetadataValidationContext context, String columnTitle) { }

    @Override
    void validateValueTuples(BamMetadataValidationContext context, Collection<ValueTuple> valueTuples) {
        valueTuples.each {
            String bamFile = it.getValue(BAM_FILE_PATH.name())
            String qualityControlFile = it.getValue(QUALITY_CONTROL_FILE.name())

            if (!qualityControlFile) {
                context.addProblem(it.cells, Level.WARNING,
                        "${QUALITY_CONTROL_FILE} has to be set for Sophia",
                        "${QUALITY_CONTROL_FILE} file has to be set for Sophia")
                return
            }
            if (!OtpPathValidator.isValidRelativePath(qualityControlFile)) {
                context.addProblem(it.cells, Level.ERROR,
                        "The path '${qualityControlFile}' is not a relative path.",
                        "At least one path is not a relative path.")
                return
            }
            if (!bamFile || !OtpPathValidator.isValidAbsolutePath(bamFile)) {
                return
            }

            Path qualityControlFilePath = context.fileSystem.getPath(bamFile).resolveSibling(qualityControlFile)
            if (!Files.exists(qualityControlFilePath)) {
                context.addProblem(it.cells, Level.ERROR,
                        "'${qualityControlFile}' does not exist or cannot be accessed by OTP.",
                        "At least one file does not exist or cannot be accessed by OTP.")
            } else if (!Files.isRegularFile(qualityControlFilePath)) {
                context.addProblem(it.cells, Level.ERROR,
                        "'${qualityControlFile}' is not a file.",
                        "At least one file is not a file.")
            } else if (!Files.isReadable(qualityControlFilePath)) {
                context.addProblem(it.cells, Level.ERROR,
                        "'${qualityControlFile}' is not readable.",
                        "At least one file is not readable.")
            } else {
                Object qcValues
                try {
                    qcValues = new JsonSlurper().parse(qualityControlFilePath.bytes)
                } catch (IOException ex) {
                    context.addProblem(it.cells, Level.ERROR, ex.message)
                    return
                } catch (Exception ex) {
                    context.addProblem(it.cells, Level.ERROR,
                            "'${qualityControlFile}' has no valid JSON structure.",
                            "At least one file has no valid JSON structure.")
                    return
                }
                try {
                    assert qcValues.all
                    assert qcValues.all.properlyPaired
                    assert qcValues.all.pairedInSequencing
                    assert qcValues.all.insertSizeMedian
                    assert qcValues.all.insertSizeCV
                } catch (AssertionError | Exception ex) {
                    context.addProblem(it.cells, Level.ERROR,
                            "'${qualityControlFile}' has not all needed values.",
                            "At least one value is missing.")
                }
            }
        }
    }
}
