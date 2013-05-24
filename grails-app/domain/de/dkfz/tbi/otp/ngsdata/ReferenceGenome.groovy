package de.dkfz.tbi.otp.ngsdata

/**
 * Represents a reference genome.
 *
 *
 */
class ReferenceGenome {

    /**
     * Name of a reference genome.
     * One human genome can have many versions (e.g. thousandGenomes is a version of hg_*).
     * Information about genome and version is not important for processing,
     * each new reference-genome-file in our case defines a new genome.
     * If needed we can add information about version later on.
     * Normally the name reflects the information about the genome and the version.
     */
    String name

    /**
     * Path to the file containing reference genome information
     * consists of 3 parts ${realmSpecificPath}/${referenceGenomePath}/${RefGenomeFileName}
     * ${realmSpecificPath}: depends on the realm and must be generated with some logic.
     * This property stores ${referenceGenomePath}/${RefGenomeFileName}
     */
    String filePath

    String toString() {
        name
    }
}
