package de.dkfz.tbi.ngstools.qualityAssessment

import javax.validation.constraints.*

/**
 * Contains the parsed input parameter
 *
 *
 */
class Parameters {

    /**
     * The name of the "ALL" chromosome.
     */
    @NotNull
    @Size(min = 1, max = 50)
    String allChromosomeName

    @Min(value = 0l)
    @Max(value = 10000l)
    Integer minAlignedRecordLength

    /**
     * compare to the meanBeaseQualPlusGaps as in coverageQc.py and then used to MappedQualityLong and QcBases
     */
    @Min(value = 0l)
    @Max(value = 100l)
    Integer minMeanBaseQuality

    /**
     * used to decide if read is quality mapped, for json mappedQualityLong, QCBases...
     */
    @Min(value = 0l)
    @Max(value = 100l)
    Integer mappingQuality

    /**
     * WindowSize for the coverage plot
     */
    @Min(value = 1l)
    @Max(value = 10000l)
    Integer winSize

    /**
     * Basket size for the insert size histogram
     */
    @Min(value = 0l)
    @Max(value = 1000000l)
    Integer binSize

    /**
     * Is used if read is added for the coverage plot, default 1
     */
    @Min(value = 0l)
    @Max(value = 100l)
    Integer coverageMappingQualityThreshold

    /**
     * Indicates, if the test mode should be used.
     * In the test mode some predefined filtering are done.
     */
    @NotNull
    Boolean testMode = false

    /**
     * The names of chromosomes to filtered, if {@link #testMode} is true.
     */
    final Set<String> filteredChromosomes = new HashSet<String>(["*", "M", "chrM"])
}
