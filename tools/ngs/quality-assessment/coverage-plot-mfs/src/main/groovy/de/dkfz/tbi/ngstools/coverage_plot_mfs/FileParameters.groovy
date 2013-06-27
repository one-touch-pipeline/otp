package de.dkfz.tbi.ngstools.coverage_plot_mfs

import javax.validation.*
import javax.validation.constraints.*

/**
 * Holds the parameters of the call of this application inclusive the validation constraints.
 *
 *
 */
class FileParameters {

    /**
     * Holds the {@link List} of parameters for this applivation
     */
    public static final List<String> FIELDS = Collections.unmodifiableList([
        "formatingJsonFile",
        "coverageDataFile",
        "generatedCoverageDataFile",
        "overrideOutput"
    ])

    /**
     * The JSON file containing mapping, filtering and sorting information.
     */
    @NotNull
    @Size(min = 1, max = Integer.MAX_VALUE)
    String formatingJsonFile

    /**
     * The coverage file, which should be mapped, filtered and sorted
     */
    @NotNull
    @Size(min = 1, max = Integer.MAX_VALUE)
    String coverageDataFile

    /**
     * Name of the output file.
     */
    @NotNull
    @Size(min = 1, max = Integer.MAX_VALUE)
    String generatedCoverageDataFile

    /**
     * flag to indicate, if an already existing output file should be overridden.
     */
    @NotNull
    Boolean overrideOutput = false

    /**
     * validate the file parameters. The file parameters are needed to validate manually,
     * because there is no annotation available for it.
     */
    public void validate() {
        // validate input files
        ParameterUtils.INSTANCE.validateInputFile(this.formatingJsonFile)
        ParameterUtils.INSTANCE.validateInputFile(this.coverageDataFile)
        // validate output directories
        ParameterUtils.INSTANCE.validateOutputDirectory(this.generatedCoverageDataFile)
    }
}
