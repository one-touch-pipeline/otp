package de.dkfz.tbi.otp.dataprocessing.snvcalling

import de.dkfz.tbi.otp.ngsdata.Individual
import de.dkfz.tbi.otp.utils.ExternalScript

/**
 * The SNV pipeline has multiple steps, each building on the previous.
 *
 */
enum SnvCallingStep {

    /**
     * Running SamTools to call SNVs
     */
    CALLING({ Individual individual, String chromosomeName ->
        if (chromosomeName != null) {
            // if the chromosome name is given, the chromosome file name is created
            assert !chromosomeName.empty
            "snvs_${individual.pid}.${chromosomeName}.vcf"
        } else {
            // otherwise the final snv calling file name is created
            "snvs_${individual.pid}_raw.vcf.gz"
        }
    }),

    /**
     * Annotates SNVs with biological context
     */
    SNV_ANNOTATION({ Individual individual -> "snvs_${individual.pid}_annotation.vcf.gz" }),

    /**
     * adds even MORE context.
     */
    SNV_DEEPANNOTATION({ Individual individual -> "snvs_${individual.pid}.vcf.gz" }),

    /**
     * The snv file can be filtered in three different ways:
     * - somatic mutations in the complete genome
     * - somatic and germline mutations in the coding regions
     * - mutatation, which changes the protein structure
     */
    FILTER_VCF({ throw new UnsupportedOperationException("TODO -> OTP-989") })

    /**
     * Closure which builds the name of the result file(s) specific for each snv step.
     */
    final Closure getResultFileName

    SnvCallingStep(Closure getResultFileName) {
        this.getResultFileName = getResultFileName
    }

    /**
     * Used to match the variables in the config file offered by the CO group.
     */
    String getConfigExecuteFlagVariableName() {
        return "RUN_${name()}"
    }

    /**
     * Used to create the name of the config files which are created for each snv calling step.
     */
    String getConfigFileNameSuffix() {
        return name().toLowerCase()
    }

    /**
     * File which is just for Roddy internal job system handling.
     */
    String getCheckpointFileName() {
        return "${name()}_checkpoint"
    }

    /**
     * Example: "SnvCallingStep.SNV_ANNOTATION"
     *
     * @see ExternalScript#scriptIdentifier
     */
    String getExternalScriptIdentifier() {
        return "${getClass().simpleName}.${name()}"
    }

    ExternalScript getExternalScript() {
        return ExternalScript.getLatestVersionOfScript(externalScriptIdentifier)
    }
}
