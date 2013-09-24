package de.dkfz.tbi.otp.ngsdata

/**
 * This class represents a Bed file
 *
 *
 */
class BedFile {

    /**
     * Name of the bed file located in a sub-directory
     * of the corresponding reference genome directory
     */
    String fileName

    /**
     * sum of lengths of all target regions covered by this bed file
     */
    long targetSize

    /**
     * A BedFile is always associated to a reference genome to know
     * on what coordinate system the regions specified in the BedFile
     * are based on.
     * A kit can have multiple BedFiles which specify the target regions
     * the kit has been designed for. The kit can have multiple BedFiles
     * because of the fact that the target regions specified can be based
     * on different reference genomes, not on the fact that the kit itself
     * has another version. ( The kit version is included in the name)
     *
     * A kit can exist in the database without having a bed file associated to
     * the kit.
     */
    static belongsTo = [
        referenceGenome: ReferenceGenome,
        exomeEnrichmentKit: ExomeEnrichmentKit
    ]

    static constraints = {
        fileName(unique: true, blank: false)
        // alternative primary key (referenceGenome, exomeEnrichmentKit)
        // we will never have 2 bed files in the db for the same
        // referenceGenome and exomeEnrichmentKit combination
        referenceGenome(unique: 'exomeEnrichmentKit')
        targetSize(min: 1L)
    }

    public String toString() {
        return "${fileName} for genome ${referenceGenome} and kit ${exomeEnrichmentKit}"
    }
}
