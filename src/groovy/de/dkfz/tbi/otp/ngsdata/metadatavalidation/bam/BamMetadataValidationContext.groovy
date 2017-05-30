package de.dkfz.tbi.otp.ngsdata.metadatavalidation.bam

import de.dkfz.tbi.otp.dataprocessing.OtpPath
import de.dkfz.tbi.otp.ngsdata.BamMetadataColumn
import de.dkfz.tbi.otp.ngsdata.BamMetadataImportService
import de.dkfz.tbi.otp.ngsdata.metadatavalidation.*
import de.dkfz.tbi.util.spreadsheet.*
import de.dkfz.tbi.util.spreadsheet.validation.*

class BamMetadataValidationContext extends AbstractMetadataValidationContext {

    private BamMetadataValidationContext(File metadataFile, String metadataFileMd5sum, Spreadsheet spreadsheet, Problems problems, byte[] content) {
        super(metadataFile, metadataFileMd5sum, spreadsheet, problems, content)
    }

    static BamMetadataValidationContext createFromFile(File metadataFile, List<String> furtherFiles) {

        Map parametersForFile = readAndCheckFile(metadataFile)

        Problems allBamProblems = validateFurtherFiles(furtherFiles, parametersForFile.problems, parametersForFile.spreadsheet)

        return new BamMetadataValidationContext(metadataFile, parametersForFile.metadataFileMd5sum,
                parametersForFile.spreadsheet, allBamProblems, parametersForFile.bytes)
    }

    /**
     * The method validates additional files, which also have to be copied during the import of bam files.
     * They must be located next to the corresponding bam files, i.e. in the same folder
     *
     * @param furtherFiles
     * @return problems During the validation it could occur problems, which are used to display error messages on the GUI
     */
    static Problems validateFurtherFiles(List<String> furtherFiles, Problems problems, Spreadsheet spreadsheet) {
        for (String path : furtherFiles.findAll()) {
            checkPositionInFolder(path, problems, spreadsheet)
        }
        return problems
    }

    static void checkPositionInFolder(String fileOrFolderRelativePath, Problems problems, Spreadsheet spreadsheet) {
        spreadsheet.dataRows.each { Row row ->
            String bamFilePath = BamMetadataImportService.uniqueColumnValue(row, BamMetadataColumn.BAM_FILE_PATH)
            if (bamFilePath) {
                String bamParentPath = new File(bamFilePath).parent
                File furtherFile = new File(bamParentPath, fileOrFolderRelativePath)
                checkFileOrDirectory(furtherFile, problems)
            }
        }
    }

    static void checkFileOrDirectory(File furtherFile, Problems problems) {
        if (!OtpPath.isValidAbsolutePath(furtherFile.path)) {
            problems.addProblem(Collections.emptySet(), Level.ERROR, "The path '${furtherFile}' is not an absolute path.")
        } else {
            if (furtherFile.isDirectory()) {
                checkFilesInDirectory(furtherFile, problems)
            } else if (furtherFile.isFile()) {
                checkFile(furtherFile, problems)
            }
        }
    }

    static void checkFile(File file, Problems problems) {
        if (!file.canRead()) {
            problems.addProblem(Collections.emptySet(), Level.ERROR, "${pathForMessage(file)} is not readable.")
        } else if (file.length() == 0L) {
            problems.addProblem(Collections.emptySet(), Level.ERROR, "${pathForMessage(file)} is empty.")
        } else if (file.length() > MAX_METADATA_FILE_SIZE_IN_MIB * 1024L * 1024L) {
            problems.addProblem(Collections.emptySet(), Level.WARNING, "${pathForMessage(file)} is larger than ${MAX_METADATA_FILE_SIZE_IN_MIB} MiB.")
        }
    }

    static checkFilesInDirectory(File furtherFile, Problems problems) {
        File[] folder = furtherFile.listFiles()
        if (folder.toList().empty) {
            problems.addProblem(Collections.emptySet(), Level.WARNING, "'The folder ${furtherFile}' is empty.")
        }
        for (File file: folder) {
            if (file.isFile()) {
                checkFile(file, problems)
            } else if (file.isDirectory()) {
                checkFilesInDirectory(file, problems)
            } else {
                problems.addProblem(Collections.emptySet(), Level.ERROR, "'${file}' is not a file.")
            }
        }
    }
}