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
     * Reference genome specific directory
     *
     * The complete path to the files containing reference genome information consists of 4 parts:
     * ${realmSpecificPath}: depends on the realm and must be generated with some logic.
     * ${allReferenceGenomes}: directory containing all reference genomes at this ${realmSpecificPath}.
     * ${referenceGenomeSpecificPath}: directory, which is specific for the reference genome (is stored in this property).
     * ${refGenomeFileNamePrefix}.*: prefix name of all files, which belong to one reference genome.
     */
    String path

    /**
     * Reference genome information is represented with several files, having the same file name prefix but different extensions.
     * This property stores such a common file name prefix.
     * It is the last part of the complete path to the reference genome files (${refGenomeFileNamePrefix})
     */
    String fileNamePrefix

    /**
     * This value stores the prefix common to names of ReferenceChromosome.
     */
    String chromosomeNamePrefix = ""

    /**
     * This value stores the suffix common to names of ReferenceChromosome.
     */
    String chromosomeNameSuffix = ""

    /**
     * Total genome size in base pairs.
     * Is calculated as the sum of all chromosome lengths.
     */
    long length

    /**
     * Total genome size in base pairs calculated as
     * the sum of all chromosome lengthWithoutN.
     */
    long lengthWithoutN

    /**
     * It has to be ensured that there is only one reference genome stored per directory -> unique path
     */
    static constraints = {
        name(unique: true, blank: false)
        path(unique: true, blank: false)
        fileNamePrefix(blank: false)
    }

    String toString() {
        name
    }
}
