package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.ngsdata.ReferenceGenome
import de.dkfz.tbi.otp.ngsdata.SeqTrack

/**
 */
abstract class AbstractMergedBamFile extends AbstractFileSystemBamFile {

    /**
     * Holds the number of lanes which were merged in this bam file
     */
    Integer numberOfMergedLanes

    public abstract boolean isMostRecentBamFile()

    static constraints = {
        numberOfMergedLanes min: 1
    }

    static mapping = {
        numberOfMergedLanes index: "abstract_merged_bam_file_number_of_merged_lanes_idx"
    }
}
