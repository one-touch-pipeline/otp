package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.utils.Entity

/**
 * This class represents a reference genome entry as found in reference genome fasta file.
 *
 */

class ReferenceGenomeEntry implements Entity {

    /**
     * Determines the different kinds of content the reference genome contains
     */
    enum Classification {
        /**
         * chromosomal DNA contains chromosome 1..22, X, Y
         */
        CHROMOSOME,
        /**
         * mitochondrial DNA
         */
        MITOCHONDRIAL,
        /**
         * contiguous DNA sequences from groups of reads, which could not be anchored
         * in the reference assembly
         */
        CONTIG,
        /**
         * DNA, which is neither chromosomal nor mitochrondrial DNA, e.g. viral DNA
         */
        UNDEFINED
    }

    /**
     * Contains the entry name, as found in the fasta file for the reference genome.
     */
    String name

    /**
     * Contains the entry name, as it will be used internally.
     */
    String alias

    /**
     * Length of entry in base pairs.
     */
    long length

    /**
     * Length of entry without N (N = unknown base pair) in base pairs.
     */
    long lengthWithoutN

    /**
     * Specifies whether the reference genome entry belongs to a chromosome,
     * the mitochrondrial DNA, a contig or is undefined
     */
    Classification classification = Classification.UNDEFINED

    static belongsTo = [
        referenceGenome: ReferenceGenome
    ]

    /**
     * In each reference genome the name and the alias for one entry shall be unique.
     * Nevertheless it is possible to have the same name or alias in two different reference genomes
     */
    static constraints = {
        name(unique: ['referenceGenome'], blank: false)
        alias(unique: ['referenceGenome'], blank: false)
    }

    String toString() {
        return name
    }

    static mapping = {
        referenceGenome index: "reference_genome_entry_reference_genome_idx"
    }
}
