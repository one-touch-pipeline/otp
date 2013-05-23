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
        File bamFile = new File(path)
        if (!bamFile.canRead()) {
            throw new ValidationException("can not read file ${path}")
        }
        if (bamFile.size() == 0) {
            throw new ValidationException("file is empty: ${path}")
        }
    }

    private void validateOutputDirecory(String path) {
        File file = new File(path)
        if (file.exists() && !file.isFile()) {
            throw new ValidationException("${path} is not a normal file")
        }
        File dir = file.getParentFile()
        if (!dir.exists()) {
            throw new ValidationException("output directory ${dir} does not exist")
        }
        if (!dir.canRead()) {
            throw new ValidationException("can not read output directory ${dir}")
        }
        if (!dir.canWrite()) {
            throw new ValidationException("can not write output directory ${dir}")
        }
    }
}
