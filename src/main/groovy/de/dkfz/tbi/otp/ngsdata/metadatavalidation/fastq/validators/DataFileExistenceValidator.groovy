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
package de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.validators

import groovy.transform.CompileDynamic
import groovyx.gpars.GParsPool
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidator
import de.dkfz.tbi.util.spreadsheet.Cell
import de.dkfz.tbi.util.spreadsheet.validation.*

import java.nio.file.Files
import java.nio.file.Path

import static de.dkfz.tbi.otp.ngsdata.metadatavalidation.AbstractMetadataValidationContext.pathForMessage

@Component
class DataFileExistenceValidator extends ValueTuplesValidator<MetadataValidationContext> implements MetadataValidator {

    @Override
    Collection<String> getDescriptions() {
        return [
                'The referenced FASTQ files must exist on the file system.',
                'Different rows should not reference the same FASTQ file.',
        ]
    }

    @Override
    List<String> getRequiredColumnTitles(MetadataValidationContext context) {
        return context.directoryStructure.requiredColumnTitles
    }

    @CompileDynamic
    @Override
    void validateValueTuples(MetadataValidationContext context, Collection<ValueTuple> allValueTuples) {
        boolean directoryStructureInfoAdded = false
        Closure addDirectoryStructureInfo = {
            if (!directoryStructureInfoAdded) {
                context.addProblem(Collections.emptySet(), LogLevel.INFO,
                        "Using directory structure '${context.directoryStructureDescription}'. If this is incorrect, please select the correct one.")
                directoryStructureInfoAdded = true
            }
        }

        Object sync = new Object()

        GParsPool.withPool(16) {
            allValueTuples.groupBy {
                context.directoryStructure.getDataFilePath(context, it)
            }.findAll { it.key != null }.eachParallel { Path path, List<ValueTuple> valueTuples ->
                if (valueTuples*.cells.sum()*.row.unique().size() != 1) {
                    synchronized (sync) {
                        addDirectoryStructureInfo()
                        context.addProblem((Set<Cell>) valueTuples*.cells.sum(), LogLevel.WARNING,
                                "Multiple rows reference the same file '${path}'.", "Multiple rows reference the same file.")
                    }
                }
                String message = null
                if (!Files.isRegularFile(path)) {
                    if (Files.exists(path)) {
                        message = "${pathForMessage(path)} is not a file."
                    } else {
                        message = "${pathForMessage(path)} does not exist or cannot be accessed by OTP."
                    }
                } else if (!Files.isReadable(path)) {
                    message = "File ${pathForMessage(path)} is not readable by OTP."
                } else if (Files.size(path) == 0L) {
                    message = "${pathForMessage(path)} is empty."
                }
                if (message) {
                    synchronized (sync) {
                        addDirectoryStructureInfo()
                        context.addProblem((Set<Cell>) valueTuples*.cells.sum(), LogLevel.ERROR, message, "At least one file can not be accessed by OTP, does not exist, is empty or is not a file.")
                    }
                }
            }
        }
    }
}
