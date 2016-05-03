package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.dataprocessing.OtpPath
import de.dkfz.tbi.otp.utils.Entity

/**
 * This class represents a Bed file
 *
 */
class BedFile implements Entity {

    /**
     * Name of the bed file located in a sub-directory
     * of the corresponding reference genome directory
     */
    String fileName

    /**
     * Sum of lengths of all target regions covered by this bed file
     */
    long targetSize

    /**
     * Sum of length values of all unique target regions.
     * Existing overlapping target regions have been merged.
     */
    long mergedTargetSize

    /**
     * A BedFile is always associated to a reference genome to know
     * on what coordinate system the regions specified in the BedFile
     * are based on.
     * A kit can have multiple BedFiles which specify the target regions
     * the kit has been designed for. The kit can have multiple BedFiles
     * because of the fact that the specified target regions can be based
     * on different reference genomes, not on the fact that the kit itself
     * has another version. ( The kit version is included in the name)
     *
     * A kit can exist in the database without having a bed file associated to
     * the kit.
     */
    static belongsTo = [
        referenceGenome: ReferenceGenome,
        libraryPreparationKit: LibraryPreparationKit
    ]

    static constraints = {
        fileName(unique: true, blank: false, validator: { OtpPath.isValidPathComponent(it) })
        // alternative primary key (referenceGenome, libraryPreparationKit)
        // we will never have 2 bed files in the db for the same
        // referenceGenome and libraryPreparationKit combination
        referenceGenome(unique: 'libraryPreparationKit')
        targetSize(min: 1L)
    }

    public String toString() {
        return "${fileName} for genome ${referenceGenome} and kit ${libraryPreparationKit}"
    }

    static mapping = {
        referenceGenome index: "bed_file_reference_genome_idx"
        libraryPreparationKit index: "bed_file_library_preparation_kit_idx"
    }
}
