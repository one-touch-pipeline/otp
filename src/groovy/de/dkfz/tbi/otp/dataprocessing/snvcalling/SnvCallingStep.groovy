package de.dkfz.tbi.otp.dataprocessing.snvcalling

/**
 * The SNV pipeline has multiple steps, each building on the previous.
 *
 */
enum SnvCallingStep {
    /**
     * Running SamTools to call SNVs
     */
    SNV_CALL,
    /**
     * Annotates SNVs with biological context
     */
    ANNOTATION,
    /**
     * adds even MORE context.
     */
    DEEP_ANNOTATION,
    /**
     * The snv file can be filtered in three different ways:
     * - somatic mutations in the complete genome
     * - somatic and germline mutations in the coding regions
     * - mutatation, which changes the protein structure
     */
    FILTER,
}
