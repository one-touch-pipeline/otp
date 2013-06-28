package de.dkfz.tbi.ngstools.qualityAssessment

import javax.validation.*
import javax.validation.constraints.*

class FileParameters {

    @NotNull
    @Size(min=1, max=Integer.MAX_VALUE)
    String pathBamFile

    @NotNull
    @Size(min=1, max=Integer.MAX_VALUE)
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

    void validateFiles() {
        // validate input files
        validateInputFile(this.pathBamFile)
        validateInputFile(this.pathBamIndexFile)
        // validate output directories
        validateOutputDirecory(this.pathQaResulsFile)
        validateOutputDirecory(this.pathCoverateResultsFile)
        validateOutputDirecory(this.pathInsertSizeHistogramFile)
    }

    private void validateInputFile(String path) {
        File file = new File(path)
        if (!file.canRead()) {
            throw new ValidationException("The file ${path} can not be read")
        }
        if (file.size() == 0) {
            throw new ValidationException("There is no content in the file ${path}")
        }
    }

    private void validateOutputDirecory(String path) {
        File file = new File(path)
        if (file.exists() && !file.isFile()) {
            throw new ValidationException("The file ${path} exists, but is not a normal file")
        }
        File dir = file.getParentFile()
        if (!dir.exists()) {
            throw new ValidationException("The output directory ${dir}, where the file ${path} should be put, does not exist")
        }
        if (!dir.canRead()) {
            throw new ValidationException("The output directory ${dir}, where the file ${path} should be put, can not be read")
        }
        if (!dir.canWrite()) {
            throw new ValidationException("The file ${path} can not be put to the directory ${dir}, since the directory is not writeable")
        }
    }
}
