package de.dkfz.tbi.otp.ngsdata

/**
 * This class represents a reference chromosome as found in reference genome fasta file.
 *
 */

class ReferenceChromosome {
    /**
     * Contains the chromosome name as found in the fasta file for the reference genome.
     */
    String name

    /**
     * Length of chromosome in base pairs.
     */
    long length

    /**
     * Length of chromosome without N (N = unknown base pair) in base pairs.
     */
    long lengthWithoutN

    static belongsTo = [
        referenceGenome: ReferenceGenome
    ]
}
