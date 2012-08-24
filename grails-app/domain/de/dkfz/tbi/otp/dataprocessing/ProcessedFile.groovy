package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.ngsdata.*

/**
 * Contains information on files which were created during data processing jobs.
 * A processed file can be derived from various other files.
 * Data processing related jobs output various files which depend on different
 * source files and types. Creating "generic" data processing jobs is
 * therefore tricky enough as passing around parameters would otherwise lead
 * to a lot of if-conditions. Introducing the ProcessFile domain class makes
 * everything a bit cleaner and easier to use.
 */
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
        PAIRED_BAM, // TODO: Paired_bam == Bam??
        STRUCTURAL_VARIATION_FILE,
        STRUCTURAL_VARIATION_PLOT
    }

    enum State {
        NOT_PROCESSED,
        TRUE,
        FALSE
    }

    /**
     * The type of this file
     */
    Type type

    /**
     * By which job was this created
     */
    String createdByJobClass

    /**
     * How was the value stored
     */
    String jobClassParameterName = "not an output parameter"

    /**
     * When was this object created in the database
     */
    Date dateCreated = null

    /**
     * Specifies the date when the file was created on the storage system.
     */
    Date fileSystemDate = null

    /**
     * Stores the original size of the file
     */
    long fileSize = -1

    /**
     * TODO: Do we need this?
     */
    String md5sum = ""

    /**
     * Set by validation job. Determines if the file exists on the fs
     */
    State fileExists = State.NOT_PROCESSED

    /**
     * A flag set by a job which actually checks the file contents
     */
    State fileIsValid = State.NOT_PROCESSED

    /**
     * By whom was this file created?
     */
    MergingLog.Execution executedBy = MergingLog.Execution.UNKNOWN

    /**
     * Helper
     */
    boolean dependenciesValid = true

    /**
     * A processed file belongs to exactly one of these objects (mutually exclusive, xor):
     *  processedFile, seqTrack, dataFile, sample, run
     * This means that a processed file belongs to exactly one entry AND the individual
     */
    static belongsTo = [
        processedFile : ProcessedFile,
        seqTrack : SeqTrack,
        dataFile : DataFile,
        sample : Sample,
        run : Run,
        individual : Individual
    ]

    static constraints = {
        type(nullable : false)

        processedFile(nullable: true)
        seqTrack(nullable : true)
        dataFile(nullable : true)
        sample(nullable : true)
        run(nullable : true)

        fileSystemDate(nullable : true)

        individual(nullable : false)
        executedBy(nullable : false)
        dependenciesValid(validator: {
                boolean b, ProcessedFile pf ->
                return (
                    pf.processedFile != null ^
                    pf.seqTrack != null ^
                    pf.dataFile != null ^
                    pf.sample != null ^
                    pf.run != null
                ) && pf.individual != null
            }
        )
    }
}
