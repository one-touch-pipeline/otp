package de.dkfz.tbi.otp.dataprocessing.snvcalling

import static de.dkfz.tbi.otp.utils.CollectionUtils.atMostOneElement

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.*

/**
 * For each tumor-control pair the snv pipeline will be called.
 * The SnvCallingInstance symbolizes one call of the pipeline.
 *
 *
 */
class SnvCallingInstance {
    /**
     * Refers to the config file which is stored in the database and is used as a basis for all the files in the filesystem.
     */
    SnvConfig config

    ProcessedMergedBamFile tumorBamFile

    ProcessedMergedBamFile controlBamFile

    /**
     * Used to construct paths in {@link #getSnvInstancePath()} and {@link #getConfigFilePath()}.
     * For example 2014-08-25_15h32.
     */
    String instanceName

    Date dateCreated

    Date lastUpdated

    static belongsTo = [
        sampleTypeCombination: SampleTypeCombinationPerIndividual
    ]

    /**
     * The overall processing state of this SNV calling run.
     * Because the SNV StartJob creates an instance of a SnvCallingInstance immediately when starting it, this will always start
     * as {@link SnvProcessingStates.IN_PROGRESS}.
     *
     * {@link SnvProcessingStates.IGNORED} has a special meaning for SnvCallingInstance, it means this combination should NOT be run.
     * (e.g. because the control-sample is bad, parts of it are withdrawn, etc.)
     */
    SnvProcessingStates processingState = SnvProcessingStates.IN_PROGRESS

    static boolean isConsistentWithSampleTypeCombination(ProcessedMergedBamFile bamFile, SnvCallingInstance instance, SampleType sampleType) {
        return (bamFile.individual == instance.individual &&
                bamFile.seqType == instance.seqType &&
                bamFile.sampleType == sampleType)
    }

    static constraints = {
        tumorBamFile validator: {val , obj -> isConsistentWithSampleTypeCombination(val, obj, obj.sampleTypeCombination.sampleType1)}
        controlBamFile validator: {val , obj -> isConsistentWithSampleTypeCombination(val, obj, obj.sampleTypeCombination.sampleType2)}
        instanceName blank: false, unique: 'sampleTypeCombination'
    }

    Project getProject() {
        return sampleTypeCombination.project
    }

    Individual getIndividual() {
        return sampleTypeCombination.individual
    }

    SeqType getSeqType() {
        return sampleTypeCombination.seqType
    }

    /**
     * Example: ${project}/sequencing/exon_sequencing/view-by-pid/${pid}/snv_results/paired/tumor_control/2014-08-25_15h32
     */
    OtpPath getSnvInstancePath() {
        return new OtpPath(sampleTypeCombination.sampleTypeCombinationPath, instanceName)
    }

    /**
     * Example: ${project}/sequencing/exon_sequencing/view-by-pid/${pid}/snv_results/paired/tumor_control/2014-08-25_15h32/config.txt
     */
    OtpPath getConfigFilePath() {
        return new OtpPath(snvInstancePath, "config.txt")
    }
}
