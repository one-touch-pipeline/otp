package de.dkfz.tbi.otp.dataprocessing.snvcalling

import de.dkfz.tbi.otp.dataprocessing.ProcessedMergedBamFile

/**
 * For each tumor-control pair the snv pipeline will be called.
 * The SnvCallingInstance symbolizes one call of the pipeline.
 *
 *
 */
class SnvCallingInstance {

    /** Shows the version control id of the vcf pipeline, which was used for this snv workflow instance. */
    String pipelineVersion

    /**
     * Name of the config file which contains the configuration for this snv-calling instance.
     * It is located next to the results of the snv-calling for which it was the base.
     */
    String configFileName

    /**
     * Refers to the config file which is stored in the database and is used as a basis for all the files in the filesystem.
     */
    ConfigPerProjectAndSeqType config

    ProcessedMergedBamFile tumorBamFile

    ProcessedMergedBamFile controlBamFile

    Date dateCreated

    Date lastUpdated

    /**
     * The overall processing state of this SNV calling run.
     * Because the SNV StartJob creates an instance of a SnvCallingInstance immediately when starting it, this will always start
     * as {@link SnvProcessingStates.IN_PROGRESS}.
     *
     * {@link SnvProcessingStates.IGNORED} has a special meaning for SnvCallingInstance, it means this combination should NOT be run.
     * (e.g. because the control-sample is bad, parts of it are withdrawn, etc.)
     */
    SnvProcessingStates processingState = SnvProcessingStates.IN_PROGRESS


    File getConfigFile() {
        return new File(configFileName)
    }

    static constraints = {
        tumorBamFile validator: { val, obj ->
            return (
            val.individual == obj.controlBamFile.individual &&
            val.seqType == obj.controlBamFile.seqType)
        }
    }
}
