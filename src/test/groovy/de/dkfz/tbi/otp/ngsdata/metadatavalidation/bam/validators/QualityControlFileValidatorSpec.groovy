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

import org.junit.ClassRule
import spock.lang.*

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.BamMetadataValidationContextFactory
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.bam.BamMetadataValidationContext
import de.dkfz.tbi.otp.utils.LocalShellHelper
import de.dkfz.tbi.util.spreadsheet.validation.LogLevel
import de.dkfz.tbi.util.spreadsheet.validation.Problem

import java.nio.file.Files
import java.nio.file.Path

import static de.dkfz.tbi.otp.ngsdata.BamMetadataColumn.*
import static de.dkfz.tbi.otp.utils.CollectionUtils.containSame

class QualityControlFileValidatorSpec extends Specification {

    @Shared
    @ClassRule
    @TempDir
    Path tempDir

    @Unroll
    void 'validate context with errors'() {
        given:
        File bamFile = Files.createFile(tempDir.resolve('bam')).toFile()
        File dir = Files.createDirectory(tempDir.resolve('folder')).toFile()
        File notReadable = Files.createFile(tempDir.resolve('notReadable')).toFile()
        assert LocalShellHelper.executeAndAssertExitCodeAndErrorOutAndReturnStdout("chmod a-r ${notReadable.absolutePath} && echo OK").trim() == 'OK'
        assert !notReadable.canRead()

        File qualityControl = Files.createFile(tempDir.resolve("qualityControl.json")).toFile()
        qualityControl.toPath().bytes = ("""\
        { "all":{"insertSizeCV": 23, "insertSizeMedian": 425, "pairedInSequencing": 2134421157, "properlyPaired": 2050531101 }}
        """).getBytes(BamMetadataValidationContext.CHARSET)

        File qualityControlInvalid = Files.createFile(tempDir.resolve("qualityControlInvalid.json")).toFile()
        qualityControlInvalid.toPath().bytes = ("""\
        { "all":{"insertSizeCV": 23, "insertSizeMedian": 425, "pairedInSequencing": 2134421157}}
        """).getBytes(BamMetadataValidationContext.CHARSET)

        File qualityControlInvalidJson = Files.createFile(tempDir.resolve("qualityControlInvalidJson")).toFile()
        qualityControlInvalidJson.toPath().bytes = ("""\
        {{"insertSizeCV": 23, "insertSizeMedian": 425, "pairedInSequencing": 2134421157, 12:34
        """).getBytes(BamMetadataValidationContext.CHARSET)

        BamMetadataValidationContext context = contextForTestValidateContextWithErrors(bamFile, dir, notReadable, qualityControl, qualityControlInvalid, qualityControlInvalidJson)

        Collection<Problem> expectedProblems = expectedProblemsForTestValidateContextWithErrors(context, dir, notReadable, qualityControlInvalid, qualityControlInvalidJson)

        when:
        new QualityControlFileValidator().validate(context)

        then:
        TestCase.assertContainSame(expectedProblems, context.problems)
    }

    private BamMetadataValidationContext contextForTestValidateContextWithErrors(
            File bamFile, File dir, File notReadAble, File qualityControl, File qualityControlInvalid, File qualityControlInvalidJson) {
        return BamMetadataValidationContextFactory.createContext(
                "${BAM_FILE_PATH}\t${QUALITY_CONTROL_FILE}\n" +
                        "${bamFile.absolutePath}\ttestFile\n" +
                        "\t/abc/testFile\n" +
                        "${bamFile.absolutePath}\t../testFile\n" +
                        "${bamFile.absolutePath}\t./testFile\n" +
                        "${bamFile.absolutePath}\t${dir.name}\n" +
                        "${bamFile.absolutePath}\t${notReadAble.name}\n" +
                        "\tabc/testFile\n" + //ignored because of no bamFilePath
                        "${bamFile.absolutePath}\t${qualityControlInvalid.name}\n" + // invalid
                        "${bamFile.absolutePath}\t${qualityControlInvalidJson.name}\n" + // invalid
                        "${bamFile.absolutePath}\t${qualityControl.name}\n" // valid
        )
    }

    private Collection<Problem> expectedProblemsForTestValidateContextWithErrors(
            BamMetadataValidationContext context, File dir, File notReadAble, File qualityControlInvalid, File qualityControlInvalidJson) {
        return [
                new Problem(context.spreadsheet.dataRows[0].cells as Set, LogLevel.ERROR,
                        "'testFile' does not exist or cannot be accessed by OTP.",
                        "At least one file does not exist or cannot be accessed by OTP."),
                new Problem(context.spreadsheet.dataRows[1].cells as Set, LogLevel.ERROR,
                        "The path '/abc/testFile' is not a relative path.", "At least one path is not a relative path."),
                new Problem(context.spreadsheet.dataRows[2].cells as Set, LogLevel.ERROR,
                        "The path '../testFile' is not a relative path.", "At least one path is not a relative path."),
                new Problem(context.spreadsheet.dataRows[3].cells as Set, LogLevel.ERROR,
                        "The path './testFile' is not a relative path.", "At least one path is not a relative path."),
                new Problem(context.spreadsheet.dataRows[4].cells as Set, LogLevel.ERROR,
                        "'${dir.name}' is not a file.", "At least one file is not a file."),
                new Problem(context.spreadsheet.dataRows[5].cells as Set, LogLevel.ERROR,
                        "'${notReadAble.name}' is not readable.", "At least one file is not readable."),
                new Problem(context.spreadsheet.dataRows[7].cells as Set, LogLevel.ERROR,
                        "'${qualityControlInvalid.name}' has not all needed values.",
                        "At least one value is missing."),
                new Problem(context.spreadsheet.dataRows[8].cells as Set, LogLevel.ERROR,
                        "'${qualityControlInvalidJson.name}' has no valid JSON structure.",
                        "At least one file has no valid JSON structure."),
        ]
    }

    void 'validate, when optional column QUALITY_CONTROL_FILE is missing, then expected warning'() {
        given:
        BamMetadataValidationContext context = BamMetadataValidationContextFactory.createContext(
                "${PROJECT}\t${INDIVIDUAL}\n"
        )

        Collection<Problem> expectedProblems = [
                new Problem(Collections.emptySet(), LogLevel.WARNING,
                        "Optional column 'QUALITY_CONTROL_FILE' is missing. 'QUALITY_CONTROL_FILE' has to be set for Sophia",
                        "Optional column 'QUALITY_CONTROL_FILE' is missing. 'QUALITY_CONTROL_FILE' has to be set for Sophia"),
                new Problem(Collections.emptySet(), LogLevel.ERROR,
                        "Required column 'BAM_FILE_PATH' is missing.",
                        "Required column 'BAM_FILE_PATH' is missing."),
        ]

        when:
        new QualityControlFileValidator().validate(context)

        then:
        containSame(context.problems, expectedProblems)
    }
}
