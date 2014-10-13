package de.dkfz.tbi.otp.dataprocessing.snvcalling

import de.dkfz.tbi.otp.dataprocessing.OtpPath
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
     *
     * Since the number and name of result files is not clear for the filter step no name will be returned.
     */
    FILTER_VCF({ "" })

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

    OtpPath getCheckpointFilePath(final SnvCallingInstance snvCallingInstance) {
        return new OtpPath(snvCallingInstance.snvInstancePath, checkpointFileName)
    }

    /**
     * Returns the name of the index file, produced by each step in the SNV pipeline.
     *
     * For the filter step the index file name of the deep annotation step has to be used.
     * This is needed due to historical reasons of the CO group.
     */
    String getIndexFileName(Individual individual) {
        if (this == SnvCallingStep.CALLING) {
            return "${this.getResultFileName(individual, null)}.tbi"
        } else if (this == SnvCallingStep.FILTER_VCF) {
            return SnvCallingStep.SNV_DEEPANNOTATION.getIndexFileName(individual)
        } else {
            return "${this.getResultFileName(individual)}.tbi"
        }
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
