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

package de.dkfz.tbi.otp.ngsdata.metadatavalidation.bam

import de.dkfz.tbi.otp.dataprocessing.OtpPath
import de.dkfz.tbi.otp.ngsdata.BamMetadataColumn
import de.dkfz.tbi.otp.ngsdata.BamMetadataImportService
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.AbstractMetadataValidationContext
import de.dkfz.tbi.util.spreadsheet.Row
import de.dkfz.tbi.util.spreadsheet.Spreadsheet
import de.dkfz.tbi.util.spreadsheet.validation.Level
import de.dkfz.tbi.util.spreadsheet.validation.Problems

import java.nio.file.*

class BamMetadataValidationContext extends AbstractMetadataValidationContext {

    FileSystem fileSystem

    private BamMetadataValidationContext(Path metadataFile, String metadataFileMd5sum, Spreadsheet spreadsheet, Problems problems, byte[] content, FileSystem fileSystem) {
        super(metadataFile, metadataFileMd5sum, spreadsheet, problems, content)
        this.fileSystem = fileSystem
    }

    static BamMetadataValidationContext createFromFile(Path metadataFile, List<String> furtherFiles, FileSystem fileSystem) {

        Map parametersForFile = readAndCheckFile(metadataFile)

        Problems allBamProblems = validateFurtherFiles(furtherFiles, parametersForFile.problems, parametersForFile.spreadsheet, fileSystem)

        return new BamMetadataValidationContext(metadataFile, parametersForFile.metadataFileMd5sum,
                parametersForFile.spreadsheet, allBamProblems, parametersForFile.bytes, fileSystem)
    }

    /**
     * The method validates additional files, which also have to be copied during the import of bam files.
     * They must be located next to the corresponding bam files, i.e. in the same folder
     *
     * @param furtherFiles
     * @return problems During the validation it could occur problems, which are used to display error messages on the GUI
     */
    static Problems validateFurtherFiles(List<String> furtherFiles, Problems problems, Spreadsheet spreadsheet, FileSystem fileSystem) {
        for (String path : furtherFiles.findAll()) {
            checkPositionInFolder(path, problems, spreadsheet, fileSystem)
        }
        return problems
    }

    static void checkPositionInFolder(String fileOrFolderRelativePath, Problems problems, Spreadsheet spreadsheet, FileSystem fileSystem) {
        spreadsheet.dataRows.each { Row row ->
            String bamFilePath = BamMetadataImportService.uniqueColumnValue(row, BamMetadataColumn.BAM_FILE_PATH)
            if (bamFilePath) {
                checkFileOrDirectory(fileSystem.getPath(new File(bamFilePath).parent, fileOrFolderRelativePath), problems)
            }
        }
    }

    static void checkFileOrDirectory(Path furtherFile, Problems problems) {
        if (!OtpPath.isValidAbsolutePath(furtherFile.toString())) {
            problems.addProblem(Collections.emptySet(), Level.ERROR, "The path '${furtherFile}' is not an absolute path.")
        } else {
            if (Files.isDirectory(furtherFile)) {
                checkFilesInDirectory(furtherFile, problems)
            } else if (Files.isRegularFile(furtherFile)) {
                checkFile(furtherFile, problems)
            }
        }
    }

    static void checkFile(Path file, Problems problems) {
        if (!Files.isReadable(file)) {
            problems.addProblem(Collections.emptySet(), Level.ERROR, "${pathForMessage(file)} is not readable.")
        } else if (Files.size(file) == 0L) {
            problems.addProblem(Collections.emptySet(), Level.WARNING, "${pathForMessage(file)} is empty.")
        } else if (Files.size(file) > MAX_ADDITIONAL_FILE_SIZE_IN_GIB * 1024L * 1024L * 1024L) {
            problems.addProblem(Collections.emptySet(), Level.WARNING, "${pathForMessage(file)} is larger than ${MAX_ADDITIONAL_FILE_SIZE_IN_GIB} GiB.")
        }
    }

    static checkFilesInDirectory(Path furtherFile, Problems problems) {
        if (Files.list(furtherFile).count() == 0) {
            problems.addProblem(Collections.emptySet(), Level.WARNING, "'The folder ${furtherFile}' is empty.")
        }
        for (Path file: Files.list(furtherFile)) {
            if (Files.isRegularFile(file)) {
                checkFile(file, problems)
            } else if (Files.isDirectory(file)) {
                checkFilesInDirectory(file, problems)
            } else {
                problems.addProblem(Collections.emptySet(), Level.ERROR, "'${file}' is not a file.")
            }
        }
    }
}
