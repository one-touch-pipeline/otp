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

import org.junit.ClassRule
import org.junit.rules.TemporaryFolder
import spock.lang.*

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.otp.utils.CreateFileHelper
import de.dkfz.tbi.otp.utils.HelperUtils
import de.dkfz.tbi.util.spreadsheet.validation.LogLevel
import de.dkfz.tbi.util.spreadsheet.validation.Problem

import java.nio.file.*
import java.nio.file.attribute.PosixFilePermissions

import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement

class AbstractMetadataValidationContextSpec extends Specification {

    @Shared
    @ClassRule
    TemporaryFolder temporaryFolder

    @Unroll
    void 'readAndCheckFile, when file cannot be opened, adds an error'() {
        when:
        Map infoMetadata = AbstractMetadataValidationContext.readAndCheckFile(file, null)

        then:
        Problem problem = exactlyOneElement(infoMetadata.problems.getProblems())
        problem.affectedCells.isEmpty()
        problem.level == LogLevel.ERROR
        problem.message.contains(problemMessage)
        infoMetadata.metadataFileMd5sum == null
        infoMetadata.spreadsheet == null

        where:
        file                                                                                 || problemMessage
        Paths.get('metadata.tsv')                                                            || 'not a valid absolute path'
        Paths.get(TestCase.uniqueNonExistentPath.path, 'metadata.tsv')                       || 'does not exist'
        temporaryFolder.newFolder('folder.tsv').toPath()                                     || 'is not a file'
        temporaryFolder.newFile("${HelperUtils.uniqueString}.xls").toPath()                  || "does not end with an accepted extension"
        makeNotReadable(temporaryFolder.newFile("${HelperUtils.uniqueString}.tsv").toPath()) || 'is not readable'
        temporaryFolder.newFile("${HelperUtils.uniqueString}.tsv").toPath()                  || 'is empty'
        createTooLargeMetadataFile()                                                         || 'is larger than'
    }

    @SuppressWarnings('Indentation')
    void 'readAndCheckFile, when there is a parsing problem, adds a warning'() {
        given:
        Path file = temporaryFolder.newFile("${HelperUtils.uniqueString}.tsv").toPath()
        file.bytes = bytes

        when:
        Map infoMetadata = AbstractMetadataValidationContext.readAndCheckFile(file)

        then:
        Problem problem = exactlyOneElement(infoMetadata.problems.getProblems())
        problem.affectedCells.isEmpty()
        problem.level == LogLevel.WARNING
        problem.message.contains(problemMessage)
        infoMetadata.metadataFileMd5sum == md5sum
        infoMetadata.spreadsheet.dataRows[0].cells[0].text ==~ firstCellRegex

        where:
        bytes                                  || md5sum                             | firstCellRegex | problemMessage
        'x\nM\u00e4use'.getBytes('ISO-8859-1') || '577c31bc9f9d49ef016288cf94605c2a' | '^M.{0,2}use$' | 'not properly encoded with UTF-8'
    }

    @Unroll
    void 'readAndCheckFile, when there is a parsing problem, adds an error'() {
        given:
        Path file = temporaryFolder.newFile("${HelperUtils.uniqueString}.tsv").toPath()
        file.bytes = bytes

        when:
        Map infoMetadata = AbstractMetadataValidationContext.readAndCheckFile(file)

        then:
        Problem problem = exactlyOneElement(infoMetadata.problems.getProblems())
        problem.affectedCells.isEmpty()
        problem.level == LogLevel.ERROR
        problem.message.contains(problemMessage)
        infoMetadata.metadataFileMd5sum == md5sum
        infoMetadata.spreadsheet == null

        where:
        bytes                                                 || md5sum                             | problemMessage
        'a\tb\ta'.getBytes(MetadataValidationContext.CHARSET) || '51cfcea2dc88d9baff201d447d2316df' | "Duplicate column 'a'"
        'x\na"b'.getBytes(MetadataValidationContext.CHARSET)  || '01a73fb20c4582eb9668dc39431c4748' | "Unterminated quoted field at end of CSV line"
    }

    void 'readAndCheckFile, when file can be parsed successfully, does not add problems'() {
        given:
        Path file = temporaryFolder.newFile("${HelperUtils.uniqueString}.tsv").toPath()
        file.bytes = 'x\nM\u00e4use'.getBytes(MetadataValidationContext.CHARSET)

        when:
        Map infoMetadata = AbstractMetadataValidationContext.readAndCheckFile(file)

        then:
        infoMetadata.problems.getProblems().isEmpty()
        infoMetadata.metadataFileMd5sum == '2628f03624261e75bba6960ff9d15291'
        infoMetadata.spreadsheet.dataRows[0].cells[0].text == 'M\u00e4use'
    }

    void 'readAndCheckFile, removes tabs and newlines at end of file'() {
        given:
        Path file = temporaryFolder.newFile("${HelperUtils.uniqueString}.tsv").toPath()
        file.bytes = 'a\nb\t\r\n\t\t\r\n'.getBytes(MetadataValidationContext.CHARSET)

        when:
        Map infoMetadata = AbstractMetadataValidationContext.readAndCheckFile(file)

        then:
        infoMetadata.spreadsheet.dataRows.size() == 1
        infoMetadata.spreadsheet.dataRows[0].cells.size() == 1
    }

    void 'readAndCheckFile, when file contains only one line, adds error'() {
        given:
        Path file = temporaryFolder.newFile("${HelperUtils.uniqueString}.tsv").toPath()
        file.bytes = 'a\n\n'.getBytes(MetadataValidationContext.CHARSET)

        when:
        Map infoMetadata = AbstractMetadataValidationContext.readAndCheckFile(file)

        then:
        Problem problem = exactlyOneElement(infoMetadata.problems.getProblems())
        problem.affectedCells.isEmpty()
        problem.level == LogLevel.ERROR
        problem.message.contains('contains less than two lines')
        infoMetadata.spreadsheet == null
    }

    void 'pathForMessage, when path does not point to a symlink, returns the path'() {
        given:
        Path targetPath = TestCase.uniqueNonExistentPath.toPath()
        String expected = "'${targetPath}'"

        when:
        String actual = MetadataValidationContext.pathForMessage(targetPath)

        then:
        actual == expected
    }

    void 'pathForMessage, when path points to a symlink, returns path of symlink and path of target'() {
        given:
        Path linkPath = temporaryFolder.newFolder().toPath().resolve('i_am_a_symlink')
        Path targetPath = TestCase.uniqueNonExistentPath.toPath()
        Files.createSymbolicLink(linkPath, targetPath)
        String expected = "'${targetPath}' (linked from '${linkPath}')"

        when:
        String actual = MetadataValidationContext.pathForMessage(linkPath)

        then:
        actual == expected
    }

    void 'pathForMessage, when path points to a symlink to a symlink, returns path of symlink and path of final target'() {
        given:
        Path folder = temporaryFolder.newFolder().toPath()
        Path sourceSymlink = folder.resolve('source_symlink')
        Path middleSymlink = folder.resolve('middle_symlink')
        Path targetPath = TestCase.uniqueNonExistentPath.toPath()
        Files.createSymbolicLink(sourceSymlink, middleSymlink)
        Files.createSymbolicLink(middleSymlink, targetPath)
        String expected = "'${targetPath}' (linked from '${sourceSymlink}')"

        when:
        String actual = MetadataValidationContext.pathForMessage(sourceSymlink)

        then:
        actual == expected
    }

    private static Path makeNotReadable(Path file) {
        Files.setPosixFilePermissions(file, PosixFilePermissions.fromString("-wx-wx-wx"))
        return file
    }

    private Path createTooLargeMetadataFile() {
        return CreateFileHelper.createFile(temporaryFolder.newFile("${HelperUtils.uniqueString}.tsv"),
                'x' * (MetadataValidationContext.MAX_METADATA_FILE_SIZE_IN_MIB * 1024L * 1024L + 1L)).toPath()
    }
}
