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
package de.dkfz.tbi.otp.ngsdata.metadatavalidation

import groovy.transform.stc.ClosureParams
import groovy.transform.stc.SimpleType

import de.dkfz.tbi.otp.utils.validation.OtpPathValidator
import de.dkfz.tbi.util.spreadsheet.*
import de.dkfz.tbi.util.spreadsheet.validation.*

import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest

import static de.dkfz.tbi.otp.utils.HelperUtils.byteArrayToHexString

abstract class AbstractMetadataValidationContext extends ValidationContext {

    static final Charset CHARSET = Charset.forName('UTF-8')
    static final long MAX_METADATA_FILE_SIZE_IN_MIB = 1
    static final long MAX_ADDITIONAL_FILE_SIZE_IN_GIB = 1

    final Path metadataFile
    final String metadataFileMd5sum
    final byte[] content

    protected AbstractMetadataValidationContext(Path metadataFile, String metadataFileMd5sum, Spreadsheet spreadsheet, Problems problems, byte[] content) {
        super(spreadsheet, problems)
        this.metadataFile = metadataFile
        this.content = content
        this.metadataFileMd5sum = metadataFileMd5sum
    }

    static Map readAndCheckFile(Path metadataFile,
                                @ClosureParams(value = SimpleType, options = ['java.lang.String']) Closure<String> renameHeader = Closure.IDENTITY,
                                @ClosureParams(value = SimpleType, options = ['de.dkfz.tbi.util.spreadsheet.Row']) Closure<Boolean> dataRowFilter = { true }) {
        Problems problems = new Problems()
        String metadataFileMd5sum = null
        Spreadsheet spreadsheet = null
        byte[] bytes = null
        List<String> acceptedExtensions = ['.tsv', '.csv', '.txt']

        if (!OtpPathValidator.isValidAbsolutePath(metadataFile.toString())) {
            problems.addProblem(Collections.emptySet(), LogLevel.ERROR, "'${metadataFile}' is not a valid absolute path.")
        } else if (!(acceptedExtensions.any { metadataFile.toString().endsWith(it) })) {
            problems.addProblem(Collections.emptySet(), LogLevel.ERROR, "The file name of '${metadataFile}' does not end with an accepted extension: " +
                    "${acceptedExtensions}")
        } else if (!Files.isRegularFile(metadataFile)) {
            if (Files.exists(metadataFile)) {
                problems.addProblem(Collections.emptySet(), LogLevel.ERROR, "${pathForMessage(metadataFile)} is not a file.")
            } else {
                problems.addProblem(Collections.emptySet(), LogLevel.ERROR,
                        "${pathForMessage(metadataFile)} does not exist or cannot be accessed by OTP.")
            }
        } else if (!Files.isReadable(metadataFile)) {
            problems.addProblem(Collections.emptySet(), LogLevel.ERROR, "${pathForMessage(metadataFile)} is not readable.")
        } else if (Files.size(metadataFile) == 0L) {
            problems.addProblem(Collections.emptySet(), LogLevel.ERROR, "${pathForMessage(metadataFile)} is empty.")
        } else if (Files.size(metadataFile) > MAX_METADATA_FILE_SIZE_IN_MIB * 1024L * 1024L) {
            problems.addProblem(Collections.emptySet(), LogLevel.ERROR, "${pathForMessage(metadataFile)} is larger than " +
                    "${MAX_METADATA_FILE_SIZE_IN_MIB} MiB.")
        } else {
            try {
                bytes = Files.readAllBytes(metadataFile)
                metadataFileMd5sum = byteArrayToHexString(MessageDigest.getInstance('MD5').digest(bytes))
                String document = new String(bytes, CHARSET)
                if (document.getBytes(CHARSET) != bytes) {
                    problems.addProblem(Collections.emptySet(), LogLevel.WARNING, "The content of ${pathForMessage(metadataFile)} is not properly " +
                            "encoded with ${CHARSET.name()}. Characters might be corrupted.")
                }
                spreadsheet = new FilteredSpreadsheet(document.replaceFirst(/[\t\r\n]+$/, ''), Delimiter.AUTO_DETECT,
                        renameHeader, dataRowFilter)
                if (spreadsheet.dataRows.size() < 1) {
                    spreadsheet = null
                    problems.addProblem(Collections.emptySet(), LogLevel.ERROR, "${pathForMessage(metadataFile)} contains less than two lines.")
                }
            } catch (Exception e) {
                problems.addProblem(Collections.emptySet(), LogLevel.ERROR, e.message)
            }
        }
        return [
                metadataFileMd5sum: metadataFileMd5sum,
                spreadsheet       : spreadsheet,
                bytes             : bytes,
                problems          : problems,
        ]
    }

    static String pathForMessage(Path path) {
        Path canonicalPath = canonicalPath(path)
        if (canonicalPath == path) {
            return "'${path}'"
        } else {
            return "'${canonicalPath}' (linked from '${path}')"
        }
    }

    /**
     * Replacement for {@link File#getCanonicalPath()}, which does not work when the target does not exist
     */
    static Path canonicalPath(Path path) {
        if (Files.isSymbolicLink(path)) {
            return canonicalPath(Files.readSymbolicLink(path))
        } else {
            return path
        }
    }
}
