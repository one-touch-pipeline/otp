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


import grails.testing.gorm.DataTest
import org.junit.ClassRule
import org.junit.rules.TemporaryFolder
import spock.lang.*

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.ngsdata.FileType
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.BamMetadataValidationContextFactory
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.bam.BamMetadataValidationContext
import de.dkfz.tbi.otp.utils.LocalShellHelper
import de.dkfz.tbi.util.spreadsheet.validation.Level
import de.dkfz.tbi.util.spreadsheet.validation.Problem

import static de.dkfz.tbi.otp.ngsdata.BamMetadataColumn.*
import static de.dkfz.tbi.otp.utils.CollectionUtils.containSame

class BamFilePathValidatorSpec extends Specification implements DataTest {

    @Override
    Class[] getDomainClassesToMock() {
        [
                FileType,
        ]
    }

    @Shared
    @ClassRule
    TemporaryFolder temporaryFolder

    @Unroll
    void 'validate context with errors'() {
        given:
        File wrongFormatFile = temporaryFolder.newFile('test.xls')
        File file = temporaryFolder.newFile('abc.bam')
        File dir = temporaryFolder.newFolder('folder.bam')
        File notReadAble = temporaryFolder.newFile('abcde.bam')
        assert LocalShellHelper.executeAndAssertExitCodeAndErrorOutAndReturnStdout("chmod a-r ${notReadAble.absolutePath} && echo OK").trim() == 'OK'
        assert !notReadAble.canRead()

        BamMetadataValidationContext context = BamMetadataValidationContextFactory.createContext(
                "${BAM_FILE_PATH}\n" +
                        "testFile.bam\n" +
                        "abc/testFile.bam\n" +
                        "../testFile.bam\n" +
                        "./testFile.bam\n" +
                        "${wrongFormatFile}\n" +
                        "/tmp/test.bam\n" +
                        "${dir.absolutePath}\n" +
                        "${notReadAble.absolutePath}\n" +
                        "${file.absolutePath}\n" // valid

        )
        Collection<Problem> expectedProblems = [
                new Problem(context.spreadsheet.dataRows[0].cells as Set, Level.ERROR,
                        "The path 'testFile.bam' is no absolute path.", "At least one path is no absolute path."),
                new Problem(context.spreadsheet.dataRows[1].cells as Set, Level.ERROR,
                        "The path 'abc/testFile.bam' is no absolute path.", "At least one path is no absolute path."),
                new Problem(context.spreadsheet.dataRows[2].cells as Set, Level.ERROR,
                        "The path '../testFile.bam' is no absolute path.", "At least one path is no absolute path."),
                new Problem(context.spreadsheet.dataRows[3].cells as Set, Level.ERROR,
                        "The path './testFile.bam' is no absolute path.", "At least one path is no absolute path."),
                new Problem(context.spreadsheet.dataRows[4].cells as Set, Level.ERROR,
                        "Filename '${wrongFormatFile}' does not end with '.bam'.", "At least one filename does not end with '.bam'."),
                new Problem(context.spreadsheet.dataRows[5].cells as Set, Level.ERROR,
                         "'/tmp/test.bam' does not exist or cannot be accessed by OTP.", "At least one file does not exist or cannot be accessed by OTP."),
                new Problem(context.spreadsheet.dataRows[6].cells as Set, Level.ERROR,
                         "'${dir.absolutePath}' is not a file.", "At least one file is not a file."),
                new Problem(context.spreadsheet.dataRows[7].cells as Set, Level.ERROR,
                          "'${notReadAble.absolutePath}' is not readable.", "At least one file is not readable."),
        ]

        when:
        new BamFilePathValidator().validate(context)

        then:
        TestCase.assertContainSame(expectedProblems, context.problems)
    }

    void 'validate, when column BAM_FILE_PATH missing, then add expected problem'() {
        given:
        BamMetadataValidationContext context = BamMetadataValidationContextFactory.createContext(
                "${PROJECT}\t +" +
                "${INDIVIDUAL}\n"
        )
        Collection<Problem> expectedProblems = [
                new Problem(Collections.emptySet(), Level.ERROR,
                        "Required column '${BAM_FILE_PATH}' is missing.")
        ]

        when:
        new BamFilePathValidator().validate(context)

        then:
        containSame(context.problems, expectedProblems)
    }
}
