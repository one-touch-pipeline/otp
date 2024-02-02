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
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidator
import de.dkfz.tbi.otp.utils.validation.OtpPathValidator
import de.dkfz.tbi.util.spreadsheet.Cell
import de.dkfz.tbi.util.spreadsheet.validation.LogLevel
import de.dkfz.tbi.util.spreadsheet.validation.AbstractSingleValueValidator

import static de.dkfz.tbi.otp.ngsdata.MetaDataColumn.FASTQ_FILE

@Component
class FilenameValidator extends AbstractSingleValueValidator<MetadataValidationContext> implements MetadataValidator {

    /**
     * '_' is required because the AlignmentAndQCWorkflows Roddy Plugin uses it as a
     * separator to group FastQs.
     *
     * See: https://github.com/DKFZ-ODCF/AlignmentAndQCWorkflows
     * QCPipelineScriptFileServiceHelper.sortAndPairLaneFilesToGroupsForSampleAndRun()
      */
    static final List<String> REQUIRED_CHARACTERS = ['_']

    @CompileDynamic
    @Override
    Collection<String> getDescriptions() {
        return [
                "The filename must end with '.gz'.",
                "The filename must contain '_fastq' or '.fastq'.",
                "The filename must contain only legal characters.",
                "The filename must contain all required characters. ${requiredCharactersAsReadableList}",
        ]
    }

    @Override
    String getColumnTitle(MetadataValidationContext context) {
        return FASTQ_FILE.name()
    }

    @Override
    void validateValue(MetadataValidationContext context, String filename, Set<Cell> cells) {
        String basename = filename.split("/").last()
        if (!filename.endsWith('.gz')) {
            context.addProblem(cells, LogLevel.ERROR, "Filename '${filename}' does not end with '.gz'.", "At least one filename does not end with '.gz'.")
        }
        try {
            FileTypeService.getFileType(filename, FileType.Type.SEQUENCE)
        } catch (FileTypeUndefinedException e) {
            context.addProblem(cells, LogLevel.ERROR, "Filename '${filename}' contains neither '_fastq' nor '.fastq'.", "At least one filename contains neither '_fastq' nor '.fastq'.")
        }
        if (!(OtpPathValidator.isValidPathComponent(filename) || OtpPathValidator.isValidAbsolutePath(filename))) {
            context.addProblem(cells, LogLevel.ERROR, "Filename '${filename}' contains invalid characters.", "At least one filename contains invalid characters.")
        }
        if (!REQUIRED_CHARACTERS.every { String it -> basename.contains(it) }) {
            context.addProblem(cells, LogLevel.WARNING,
                    "Filename '${filename}' does not contain all required characters: ${requiredCharactersAsReadableList}",
                    "At least one filename does not contain all required characters."
            )
        }
    }

    static String getRequiredCharactersAsReadableList() {
        return "['${REQUIRED_CHARACTERS.join("', '")}']"
    }
}
