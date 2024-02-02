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
package de.dkfz.tbi.otp.ngsdata.metadatavalidation

import groovy.transform.CompileDynamic
import groovy.transform.stc.ClosureParams
import groovy.transform.stc.SimpleType

import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.otp.utils.validation.OtpPathValidator
import de.dkfz.tbi.util.spreadsheet.*
import de.dkfz.tbi.util.spreadsheet.validation.LogLevel
import de.dkfz.tbi.util.spreadsheet.validation.Problems

import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest

import static de.dkfz.tbi.otp.utils.HelperUtils.byteArrayToHexString

class MetadataValidationService {

    FileService fileService

    static String pathForMessage(Path path) {
        Path canonicalPath = canonicalPath(path)
        return canonicalPath == path ? "'${path}'" : "'${canonicalPath}' (linked from '${path}')"
    }

    /**
     * Replacement for {@link File#getCanonicalPath()}, which does not work when the target does not exist
     */
    static Path canonicalPath(Path path) {
        return Files.isSymbolicLink(path) ? canonicalPath(Files.readSymbolicLink(path)) : path
    }

    ContentWithPathAndProblems readPath(Path metadataFile) {
        Problems problems = new Problems()
        byte[] content = null
        List<String> acceptedExtensions = ['.tsv', '.csv', '.txt']

        if (!OtpPathValidator.isValidAbsolutePath(metadataFile.toString())) {
            problems.addProblem(Collections.emptySet(), LogLevel.ERROR, "'${metadataFile}' is not a valid absolute path.")
        } else if (!(acceptedExtensions.any { metadataFile.toString().endsWith(it) })) {
            problems.addProblem(Collections.emptySet(), LogLevel.ERROR, "The file name of '${metadataFile}' does not end with an accepted extension: " +
                    "${acceptedExtensions}")
        } else if (!Files.exists(metadataFile)) {
            problems.addProblem(Collections.emptySet(), LogLevel.ERROR,
                    "${pathForMessage(metadataFile)} does not exist or cannot be accessed by OTP.")
        } else if (!Files.isRegularFile(metadataFile)) {
            problems.addProblem(Collections.emptySet(), LogLevel.ERROR, "${pathForMessage(metadataFile)} is not a file.")
        } else if (!fileService.fileIsReadable(metadataFile)) {
            problems.addProblem(Collections.emptySet(), LogLevel.ERROR, "${pathForMessage(metadataFile)} is not readable.")
        } else if (Files.size(metadataFile) == 0L) {
            problems.addProblem(Collections.emptySet(), LogLevel.ERROR, "${pathForMessage(metadataFile)} is empty.")
        } else if (Files.size(metadataFile) > MetadataValidationContext.MAX_METADATA_FILE_SIZE_IN_MIB * 1024L * 1024L) {
            problems.addProblem(Collections.emptySet(), LogLevel.ERROR, "${pathForMessage(metadataFile)} is larger than " +
                    "${MetadataValidationContext.MAX_METADATA_FILE_SIZE_IN_MIB} MiB.")
        } else {
            try {
                content = Files.readAllBytes(metadataFile)
            } catch (Exception e) {
                problems.addProblem(Collections.emptySet(), LogLevel.ERROR, e.message)
            }
        }
        return new ContentWithPathAndProblems(content, metadataFile, problems)
    }

    @CompileDynamic
    Map readAndCheckFile(Path metadataFile,
                         @ClosureParams(value = SimpleType, options = ['java.lang.String']) Closure<String> renameHeader = Closure.IDENTITY,
                         @ClosureParams(value = SimpleType, options = ['de.dkfz.tbi.util.spreadsheet.Row']) Closure<Boolean> dataRowFilter = { true }) {
        Problems problems = new Problems()

        ContentWithPathAndProblems readPathMap = readPath(metadataFile)
        problems.addProblems(readPathMap.problems)

        byte[] content = readPathMap.content

        Map checkContentMap = checkContent(content, renameHeader, dataRowFilter)
        problems.addProblems(checkContentMap.problems)

        return [
                metadataFileMd5sum: checkContentMap.metadataFileMd5sum,
                spreadsheet       : checkContentMap.spreadsheet,
                bytes             : content,
                problems          : problems,
        ]
    }

    Map checkContent(byte[] content, @ClosureParams(value = SimpleType, options = ['java.lang.String']) Closure<String> renameHeader = Closure.IDENTITY,
                     @ClosureParams(value = SimpleType, options = ['de.dkfz.tbi.util.spreadsheet.Row']) Closure<Boolean> dataRowFilter = { true }) {
        Problems problems = new Problems()
        String metadataFileMd5sum = null
        Spreadsheet spreadsheet = null
        try {
            metadataFileMd5sum = byteArrayToHexString(MessageDigest.getInstance('MD5').digest(content))
            String document = new String(content, MetadataValidationContext.CHARSET)
            if (document.getBytes(MetadataValidationContext.CHARSET) != content) {
                problems.addProblem(Collections.emptySet(), LogLevel.WARNING, "The content of the file is not properly " +
                        "encoded with ${MetadataValidationContext.CHARSET.name()}. Characters might be corrupted.")
            }
            spreadsheet = new FilteredSpreadsheet(document.replaceFirst(/[\t\r\n]+$/, ''), Delimiter.AUTO_DETECT,
                    renameHeader, dataRowFilter)
            if (spreadsheet.dataRows.size() < 1) {
                spreadsheet = null
                problems.addProblem(Collections.emptySet(), LogLevel.ERROR, "The file contains less than two lines.")
            }
        } catch (Exception e) {
            problems.addProblem(Collections.emptySet(), LogLevel.ERROR, e.message)
        }

        return [
                metadataFileMd5sum: metadataFileMd5sum,
                spreadsheet       : spreadsheet,
                problems          : problems,
        ]
    }
}
