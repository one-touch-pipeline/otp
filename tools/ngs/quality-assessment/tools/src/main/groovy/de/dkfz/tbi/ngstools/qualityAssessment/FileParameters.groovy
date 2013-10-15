package de.dkfz.tbi.ngstools.qualityAssessment

import javax.validation.*
import javax.validation.constraints.*

class FileParameters {

    Mode inputMode

    @NotNull
    @Size(min=1, max=Integer.MAX_VALUE)
    @FileCanRead
    String pathBamFile

    @NotNull
    @Size(min=1, max=Integer.MAX_VALUE)
    @FileCanRead
    String pathBamIndexFile

    @NotNull
    @Size(min=1, max=Integer.MAX_VALUE)
    String pathQaResulsFile

    @NotNull
    @Size(min=1, max=Integer.MAX_VALUE)
    String pathCoverateResultsFile

    @NotNull
    @Size(min=1, max=Integer.MAX_VALUE)
    String pathInsertSizeHistogramFile

    @NotNull
    Boolean overrideOutput = false

    /**
     * Path to the Bed File containing the coordinates for the target regions
     */
    String bedFilePath

    /**
     * Path to the file, containing the reference genome meta information name, length and lengthWithoutN
     */
    String refGenMetaInfoFilePath

    void validateFiles() {
        // validate output directories
        validateOutputDirecory(this.pathQaResulsFile)
        validateOutputDirecory(this.pathCoverateResultsFile)
        validateOutputDirecory(this.pathInsertSizeHistogramFile)
        // validate case specific file input parameters
        validateCaseSpecificInput()
    }

    /**
     * bedFilePath and refGenMetaInfoFilePath are provided only in the case of EXOME.
     * Hence they must be validate only in the case of EXOME.
     * This method performs such validation.
     */
    private void validateCaseSpecificInput() {
        if (inputMode == Mode.EXOME) {
            FileValidator.canRead(bedFilePath, 'bedFilePath')
            FileValidator.canRead(refGenMetaInfoFilePath, 'refGenMetaInfoFilePath')
        }
    }

    /**
     * Validate the given path as output parameter.
     * Therefore the following constraints are checked:
     * <ul>
     * <li>The parent directory of the path needs to exist</li>
     * <li>The parent directory of the path needs to be readable</li>
     * <li>The parent directory of the path needs to be writable</li>
     * <li>If a path with the given name exist, the path needs to reference a normal file (directory or special files are not allowed)</li>
     * </ul>
     *
     * @param path, the path of the file to be checked as output directory
     */
    private void validateOutputDirecory(String path) {
        File file = new File(path)
        if (file.exists() && !file.isFile()) {
            throw new ValidationException("The file '${path}' exists, but is not a normal file")
        }
        File dir = file.getParentFile()
        if (!dir.exists()) {
            throw new ValidationException("The output directory '${dir}', where the file '${path}' should be put, does not exist")
        }
        if (!dir.canRead()) {
            throw new ValidationException("The output directory '${dir}', where the file '${path}' should be put, can not be read")
        }
        if (!dir.canWrite()) {
            throw new ValidationException("The file '${path}' can not be put to the directory '${dir}', since the directory is not writeable")
        }
    }
}
