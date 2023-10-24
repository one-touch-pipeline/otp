/*
 * Copyright 2011-2023 The OTP authors
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

import spock.lang.Specification
import spock.lang.TempDir

import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.bam.BamMetadataValidationContext
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.fastq.MetadataValidationContext
import de.dkfz.tbi.otp.utils.*
import de.dkfz.tbi.util.spreadsheet.validation.*

import java.nio.file.*

import static de.dkfz.tbi.otp.ngsdata.BamMetadataColumn.BAM_FILE_PATH
import static de.dkfz.tbi.otp.ngsdata.BamMetadataColumn.SEQUENCING_READ_TYPE
import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement

class BamMetadataValidationServiceSpec extends Specification {

    @TempDir
    Path tempDir

    Problems problems

    BamMetadataValidationService bamMetadataValidationFileService

    def setup() {
        problems = new Problems()
        bamMetadataValidationFileService = new BamMetadataValidationService()
    }

    void 'createFromFile, when file header contains alias, replace it'() {
        given:
        Path file = tempDir.resolve("${HelperUtils.uniqueString}.tsv")
        file.bytes = ("UNKNOWN ${BAM_FILE_PATH} ${SEQUENCING_READ_TYPE.importAliases.first()}\n" +
                "1 2 3"
        ).replaceAll(' ', '\t').getBytes(MetadataValidationContext.CHARSET)

        when:
        BamMetadataValidationContext context = bamMetadataValidationFileService.createFromFile(file, [], FileSystems.default, false)

        then:
        bamMetadataValidationFileService.fileService = Mock(FileService) {
            1 * fileIsReadable(_) >> true
        }

        and:
        context.spreadsheet.header.cells[0].text == "UNKNOWN"
        context.spreadsheet.header.cells[1].text == BAM_FILE_PATH.name()
        context.spreadsheet.header.cells[2].text == SEQUENCING_READ_TYPE.name()
    }

    void "checkFilesInDirectory, when a folder is empty, add a warning"() {
        given:
        Path emptyFolder = tempDir.resolve("emptyFolder")
        Files.createDirectory(emptyFolder)

        when:
        bamMetadataValidationFileService.checkFilesInDirectory(emptyFolder, problems)

        then:
        Problem problem = exactlyOneElement(problems.problems)
        problem.level == LogLevel.WARNING
        problem.message.contains("is empty.")
    }

    void "checkFilesInDirectory, when find a subfolder, check the content"() {
        given:
        Path folder = tempDir.resolve("folder")
        Path subfolder = folder.resolve("subfolder")
        assert Files.createDirectories(subfolder)
        Path file2 = subfolder.resolve("file2.txt")
        file2.write("something other")

        when:
        bamMetadataValidationFileService.checkFilesInDirectory(folder, problems)

        then:
        bamMetadataValidationFileService.fileService = Mock(FileService) {
            1 * fileIsReadable(_) >> true
        }

        and:
        problems.problems.empty
    }

    void "checkFile, when is not readable, add the corresponding problem"() {
        given:
        Path notReadAble = CreateFileHelper.createFile(tempDir.resolve('notReadable.txt'))
        assert LocalShellHelper.executeAndAssertExitCodeAndErrorOutAndReturnStdout("chmod a-r ${notReadAble.toAbsolutePath()} && echo OK").trim() == 'OK'

        when:
        bamMetadataValidationFileService.checkFile(notReadAble, problems)

        then:
        bamMetadataValidationFileService.fileService = Mock(FileService) {
            1 * fileIsReadable(_) >> false
        }

        and:
        Problem problem = exactlyOneElement(problems.problems)
        problem.level == LogLevel.ERROR
        problem.message.contains("is not readable.")
    }

    void "checkFile, when is empty, add the corresponding problem"() {
        given:
        Path emptyFile = CreateFileHelper.createFile(tempDir.resolve('emptyFile.txt'), "")

        when:
        bamMetadataValidationFileService.checkFile(emptyFile, problems)

        then:
        bamMetadataValidationFileService.fileService = Mock(FileService) {
            1 * fileIsReadable(_) >> true
        }

        and:
        Problem problem = exactlyOneElement(problems.problems)
        problem.level == LogLevel.WARNING
        problem.message.contains("is empty.")
    }

    void "checkFile, when is to large, add the corresponding problem"() {
        given:
        bamMetadataValidationFileService.fileService = Mock(FileService)

        Path bigFile = CreateFileHelper.createFile(tempDir.resolve('bigFile.txt'))

        bamMetadataValidationFileService.fileService.fileSizeExceeded(_, _) >> true

        when:
        bamMetadataValidationFileService.checkFile(bigFile, problems)

        then:
        1 * bamMetadataValidationFileService.fileService.fileIsReadable(_) >> true

        and:
        Problem problem = exactlyOneElement(problems.problems)
        problem.level == LogLevel.WARNING
        problem.message.contains("is larger than ${AbstractMetadataValidationContext.MAX_ADDITIONAL_FILE_SIZE_IN_GIB} GiB.")
    }
}
