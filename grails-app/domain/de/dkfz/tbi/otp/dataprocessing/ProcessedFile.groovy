package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.ngsdata.*

class ProcessedFile {

    /**
     * Different possible file types
     */
    enum Type {
        ALIGNED_SEQUENCE,
        ALIGNED_LANE_QUALITY,
        ALIGNMENT_SUMMARY_FILE,
        BAM,
        BAM_INDEX,
        FASTQC_ARCHIVE,
        FLAGSTATS,
        GENOME_COVERAGE_FILE,
        INSERTSIZE_DISTRIBUTION_FILE,
        INSERTSIZE_DISTRIBUTION_PLOT,
        PAIRED_BAM,
        STRUCTURAL_VARIATION_FILE,
        STRUCTURAL_VARIATION_PLOT
    }

    enum State {
        NOT_PROCESSED,
        TRUE,
        FALSE
    }

    Type type
    String createdByJobClass
    String derivedFromClassName
    long derivedFromId

    Date dateCreated = null
    Date fileSystemDate = null
    long fileSize = -1
    State fileExists = State.NOT_PROCESSED
    State fileIsValid = State.NOT_PROCESSED

    /**
     * How was the value stored
     */
    String jobClassParameterName = "not an output parameter"

    /**
     * md5sum of the file
     */
    String md5sum = ""

    /**
     * By whom was this file created?
     */
    MergingLog.Execution executedBy = MergingLog.Execution.UNKNOWN

    static belongsTo = [
        individual : Individual
    ]

    static constraints = {
        type(nullable : false)
        fileSystemDate(nullable : true)
        individual(nullable : false)
        executedBy(nullable : false)
    }
}
