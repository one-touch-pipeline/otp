package de.dkfz.tbi.otp.dataprocessing.snvcalling

/**
 * The SNV pipeline has multiple steps, each building on the previous.
 *
 */
enum SnvCallingStep {
    /**
     * Running SamTools to call SNVs
     */
    CALLING,
    /**
     * Annotates SNVs with biological context
     */
    ANNOTATION,
    /**
     * adds even MORE context.
     */
    DEEPANNOTATION,
    /**
     * The snv file can be filtered in three different ways:
     * - somatic mutations in the complete genome
     * - somatic and germline mutations in the coding regions
     * - mutatation, which changes the protein structure
     */
    FILTER_VCF

    /**
     * Used to match the variables in the config file offered by the CO group.
     */
    String getConfigExecuteFlagVariableName() {
        return "RUN_${name()}"
    }
}
