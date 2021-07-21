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
import org.junit.rules.TemporaryFolder
import spock.lang.*

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.BamMetadataValidationContextFactory
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.bam.BamMetadataValidationContext
import de.dkfz.tbi.otp.utils.LocalShellHelper
import de.dkfz.tbi.util.spreadsheet.validation.LogLevel
import de.dkfz.tbi.util.spreadsheet.validation.Problem

import static de.dkfz.tbi.otp.ngsdata.BamMetadataColumn.*
import static de.dkfz.tbi.otp.utils.CollectionUtils.containSame

class InsertSizeFileValidatorSpec extends Specification {

    @Shared
    @ClassRule
    TemporaryFolder temporaryFolder

    @Unroll
    void 'validate context with errors'() {
        given:
        File bamFile = temporaryFolder.newFile('abc')
        File insertFile = temporaryFolder.newFile('insertFile')
        File dir = temporaryFolder.newFolder('folder')
        File notReadAble = temporaryFolder.newFile('abcde')
        assert LocalShellHelper.executeAndAssertExitCodeAndErrorOutAndReturnStdout("chmod a-r ${notReadAble.absolutePath} && echo OK").trim() == 'OK'
        assert !notReadAble.canRead()

        BamMetadataValidationContext context = BamMetadataValidationContextFactory.createContext(
                "${BAM_FILE_PATH}\t${INSERT_SIZE_FILE}\n" +
                "${bamFile.absolutePath}\ttestFile\n" +
                "\t/abc/testFile\n" +
                "${bamFile.absolutePath}\t../testFile\n" +
                "${bamFile.absolutePath}\t./testFile\n" +
                "${bamFile.absolutePath}\t${dir.name}\n" +
                "${bamFile.absolutePath}\t${notReadAble.name}\n" +
                "\tabc/testFile\n" + //ignored because of no bamFilePath
                "${bamFile.absolutePath}\t${insertFile.name}\n" // valid
        )

        Collection<Problem> expectedProblems = [
                new Problem(context.spreadsheet.dataRows[0].cells as Set, LogLevel.ERROR,
                        "'testFile' does not exist or cannot be accessed by OTP.", "At least one file does not exist or cannot be accessed by OTP."),
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
        ]

        when:
        new InsertSizeFileValidator().validate(context)

        then:
        TestCase.assertContainSame(expectedProblems, context.problems)
    }

    void 'validate, when column INSERT_SIZE_FILE is missing, then add expected problem'() {
        given:
        BamMetadataValidationContext context = BamMetadataValidationContextFactory.createContext(
                "${PROJECT}\t +" +
                        "${INDIVIDUAL}\n"
        )
        Collection<Problem> expectedProblems = [
                new Problem(Collections.emptySet(), LogLevel.WARNING,
                        "Optional column 'INSERT_SIZE_FILE' is missing. '${INSERT_SIZE_FILE}' has to be set for Sophia", "Optional column 'INSERT_SIZE_FILE' is missing. '${INSERT_SIZE_FILE}' has to be set for Sophia")
        ]

        when:
        new InsertSizeFileValidator().validate(context)

        then:
        containSame(context.problems, expectedProblems)
    }
}
