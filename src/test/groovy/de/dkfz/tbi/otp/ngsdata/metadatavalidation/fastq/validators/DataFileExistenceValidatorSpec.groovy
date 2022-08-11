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

import grails.testing.gorm.DataTest
import spock.lang.Specification
import spock.lang.TempDir

import de.dkfz.tbi.otp.ngsdata.Realm
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.directorystructures.DirectoryStructure
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.util.spreadsheet.Cell
import de.dkfz.tbi.util.spreadsheet.validation.*

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.regex.Matcher

import static de.dkfz.tbi.TestCase.assertContainSame
import static de.dkfz.tbi.otp.ngsdata.metadatavalidation.MetadataValidationContextFactory.createContext

class DataFileExistenceValidatorSpec extends Specification implements DataTest {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                Realm,
        ]
    }

    @TempDir
    Path tempDir

    void 'validate adds expected problems'() {
        given:
        File dir = tempDir.toFile()
        Files.createDirectory(tempDir.resolve('dir_instead_of_file'))
        Files.createFile(tempDir.resolve('empty_file'))
        File nonEmptyFile = Files.createFile(tempDir.resolve('not_empty_file')).toFile()
        nonEmptyFile.text = "not empty"
        File nonReadableFile = Files.createFile(tempDir.resolve('not_readable_file')).toFile()
        nonReadableFile.readable = false
        DirectoryStructure directoryStructure = Mock(DirectoryStructure) {
            getRequiredColumnTitles() >> ['FILENAME']
            getDataFilePath(_, _) >> { MetadataValidationContext context, ValueTuple valueTuple ->
                Matcher matcher = valueTuple.getValue('FILENAME') =~ /^(.+) .$/
                if (matcher) {
                    return Paths.get(dir.path, matcher.group(1))
                }
                return null
            }
        }
        MetadataValidationContext context = createContext(
                "FILENAME\n" +
                        "invalid\n" +
                        "not_empty_file A\n" +
                        "not_empty_file A\n" +
                        "dir_instead_of_file A\n" +
                        "empty_file A\n" +
                        "not_found1 A\n" +
                        "not_found1 A\n" +
                        "not_found2 A\n" +
                        "not_found2 B\n" +
                        "not_found3 A\n" +
                        "not_found3 A\n" +
                        "not_found3 B\n" +
                        "not_found3 B\n" +
                        "not_readable_file A\n",
                [directoryStructure: directoryStructure, directoryStructureDescription: 'test directory structure',]
        )
        Collection<Problem> expectedProblems = [
                new Problem(Collections.emptySet(),
                        LogLevel.INFO, "Using directory structure 'test directory structure'. If this is incorrect, please select the correct one."),
                new Problem((context.spreadsheet.dataRows[1].cells + context.spreadsheet.dataRows[2].cells) as Set<Cell>,
                        LogLevel.WARNING, "Multiple rows reference the same file '${new File(dir, 'not_empty_file')}'.", "Multiple rows reference the same file."),
                new Problem(context.spreadsheet.dataRows[3].cells as Set<Cell>,
                        LogLevel.ERROR, "'${new File(dir, 'dir_instead_of_file')}' is not a file.", "At least one file can not be accessed by OTP, does not exist, is empty or is not a file."),
                new Problem(context.spreadsheet.dataRows[4].cells as Set<Cell>,
                        LogLevel.ERROR, "'${new File(dir, 'empty_file')}' is empty.", "At least one file can not be accessed by OTP, does not exist, is empty or is not a file."),
                new Problem((context.spreadsheet.dataRows[5].cells + context.spreadsheet.dataRows[6].cells) as Set<Cell>,
                        LogLevel.WARNING, "Multiple rows reference the same file '${new File(dir, 'not_found1')}'.", "Multiple rows reference the same file."),
                new Problem((context.spreadsheet.dataRows[5].cells + context.spreadsheet.dataRows[6].cells) as Set<Cell>,
                        LogLevel.ERROR, "'${new File(dir, 'not_found1')}' does not exist or cannot be accessed by OTP.", "At least one file can not be accessed by OTP, does not exist, is empty or is not a file."),
                new Problem((context.spreadsheet.dataRows[7].cells + context.spreadsheet.dataRows[8].cells) as Set<Cell>,
                        LogLevel.WARNING, "Multiple rows reference the same file '${new File(dir, 'not_found2')}'.", "Multiple rows reference the same file."),
                new Problem((context.spreadsheet.dataRows[7].cells + context.spreadsheet.dataRows[8].cells) as Set<Cell>,
                        LogLevel.ERROR, "'${new File(dir, 'not_found2')}' does not exist or cannot be accessed by OTP.", "At least one file can not be accessed by OTP, does not exist, is empty or is not a file."),
                new Problem((context.spreadsheet.dataRows[9].cells + context.spreadsheet.dataRows[10].cells +
                        context.spreadsheet.dataRows[11].cells + context.spreadsheet.dataRows[12].cells) as Set<Cell>,
                        LogLevel.WARNING, "Multiple rows reference the same file '${new File(dir, 'not_found3')}'.", "Multiple rows reference the same file."),
                new Problem((context.spreadsheet.dataRows[9].cells + context.spreadsheet.dataRows[10].cells +
                        context.spreadsheet.dataRows[11].cells + context.spreadsheet.dataRows[12].cells) as Set<Cell>,
                        LogLevel.ERROR, "'${new File(dir, 'not_found3')}' does not exist or cannot be accessed by OTP.", "At least one file can not be accessed by OTP, does not exist, is empty or is not a file."),
                new Problem((context.spreadsheet.dataRows[13].cells) as Set<Cell>,
                        LogLevel.ERROR, "File '${new File(dir, 'not_readable_file')}' is not readable by OTP.", "At least one file can not be accessed by OTP, does not exist, is empty or is not a file."),
        ]
        DataFileExistenceValidator validator = new DataFileExistenceValidator()

        when:
        validator.validate(context)

        then:
        assertContainSame(context.problems, expectedProblems)
    }
}
