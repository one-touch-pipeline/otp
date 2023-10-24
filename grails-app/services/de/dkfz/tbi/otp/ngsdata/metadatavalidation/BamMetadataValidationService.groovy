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

import groovy.transform.CompileDynamic

import de.dkfz.tbi.otp.ngsdata.BamMetadataColumn
import de.dkfz.tbi.otp.ngsdata.BamMetadataImportService
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.bam.BamMetadataValidationContext
import de.dkfz.tbi.otp.utils.validation.OtpPathValidator
import de.dkfz.tbi.util.spreadsheet.Row
import de.dkfz.tbi.util.spreadsheet.Spreadsheet
import de.dkfz.tbi.util.spreadsheet.validation.LogLevel
import de.dkfz.tbi.util.spreadsheet.validation.Problems

import java.nio.file.*
import java.util.stream.Stream

class BamMetadataValidationService extends MetadataValidationService {

    @CompileDynamic
    BamMetadataValidationContext createFromFile(Path metadataFile, List<String> furtherFiles, FileSystem fileSystem, boolean linkSourceFiles) {
        Map parametersForFile = readAndCheckFile(metadataFile) { String s ->
            BamMetadataColumn.getColumnForName(s)?.name() ?: s
        }

        Problems allBamProblems = validateFurtherFiles(furtherFiles, parametersForFile.problems, parametersForFile.spreadsheet, fileSystem)

        return new BamMetadataValidationContext(metadataFile, parametersForFile.metadataFileMd5sum,
                parametersForFile.spreadsheet, allBamProblems, parametersForFile.bytes, fileSystem, linkSourceFiles)
    }

    /**
     * The method validates additional files, which also have to be copied during the import of bam files.
     * They must be located next to the corresponding bam files, i.e. in the same folder
     *
     * @return problems During the validation it could occur problems, which are used to display error messages on the GUI
     */
    Problems validateFurtherFiles(List<String> furtherFiles, Problems problems, Spreadsheet spreadsheet, FileSystem fileSystem) {
        for (String path : furtherFiles.findAll()) {
            checkPositionInFolder(path, problems, spreadsheet, fileSystem)
        }
        return problems
    }

    @SuppressWarnings('JavaIoPackageAccess')
    void checkPositionInFolder(String fileOrFolderRelativePath, Problems problems, Spreadsheet spreadsheet, FileSystem fileSystem) {
        spreadsheet.dataRows.each { Row row ->
            String bamFilePath = BamMetadataImportService.uniqueColumnValue(row, BamMetadataColumn.BAM_FILE_PATH)
            if (bamFilePath) {
                checkFileOrDirectory(fileSystem.getPath(new File(bamFilePath).parent, fileOrFolderRelativePath), problems)
            }
        }
    }

    void checkFileOrDirectory(Path furtherFile, Problems problems) {
        if (OtpPathValidator.isValidAbsolutePath(furtherFile.toString())) {
            if (Files.isDirectory(furtherFile)) {
                checkFilesInDirectory(furtherFile, problems)
            } else if (Files.isRegularFile(furtherFile)) {
                checkFile(furtherFile, problems)
            }
        } else {
            problems.addProblem(Collections.emptySet(), LogLevel.ERROR, "The path '${furtherFile}' is no absolute path.")
        }
    }

    void checkFilesInDirectory(Path furtherFile, Problems problems) {
        Stream<Path> stream = null
        boolean isEmpty = true
        try {
            stream = Files.list(furtherFile)
            stream.each { Path file ->
                isEmpty = false
                if (Files.isRegularFile(file)) {
                    checkFile(file, problems)
                } else if (Files.isDirectory(file)) {
                    checkFilesInDirectory(file, problems)
                } else {
                    problems.addProblem(Collections.emptySet(), LogLevel.ERROR, "'${file}' is not a file.")
                }
            }
            if (isEmpty) {
                problems.addProblem(Collections.emptySet(), LogLevel.WARNING, "'The folder ${furtherFile}' is empty.")
            }
        } finally {
            stream?.close()
        }
    }

    void checkFile(Path file, Problems problems) {
        if (!fileService.fileIsReadable(file)) {
            problems.addProblem(Collections.emptySet(), LogLevel.ERROR, "${pathForMessage(file)} is not readable.")
        } else if (Files.size(file) == 0L) {
            problems.addProblem(Collections.emptySet(), LogLevel.WARNING, "${pathForMessage(file)} is empty.")
        } else if (fileService.fileSizeExceeded(file, BamMetadataValidationContext.MAX_ADDITIONAL_FILE_SIZE_IN_GIB *
                BamMetadataValidationContext.FACTOR_1024 * BamMetadataValidationContext.FACTOR_1024 * BamMetadataValidationContext.FACTOR_1024)) {
            problems.addProblem(
                    Collections.emptySet(),
                    LogLevel.WARNING,
                    "${pathForMessage(file)} is larger than ${BamMetadataValidationContext.MAX_ADDITIONAL_FILE_SIZE_IN_GIB} GiB."
            )
        }
    }
}
