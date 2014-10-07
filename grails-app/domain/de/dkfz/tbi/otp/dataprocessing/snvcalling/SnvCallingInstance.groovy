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

    ProcessedMergedBamFile sampleType1BamFile

    ProcessedMergedBamFile sampleType2BamFile

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
        sampleType1BamFile validator: {val, obj ->
            obj.sampleTypeCombination && isConsistentWithSampleTypeCombination(val, obj, obj.sampleTypeCombination.sampleType1)}
        sampleType2BamFile validator: {val, obj ->
            obj.sampleTypeCombination && isConsistentWithSampleTypeCombination(val, obj, obj.sampleTypeCombination.sampleType2)}
        instanceName blank: false, unique: 'sampleTypeCombination'
    }

    static mapping = {
        sampleType1BamFile index: "snv_calling_instance_sample_type_1_bam_file_idx"
        sampleType2BamFile index: "snv_calling_instance_sample_type_2_bam_file_idx"
        sampleTypeCombination index: "snv_calling_instance_sample_type_combination_idx"
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

    /**
     * Example: ${project}/sequencing/exon_sequencing/view-by-pid/${pid}/paired/snv_results/tumor_control/config_calling_2014-08-25_15h32.txt
     */
    OtpPath getStepConfigFileLinkedPath(final SnvCallingStep step) {
        return new OtpPath(sampleTypeCombination.sampleTypeCombinationPath, "config_${step.configFileNameSuffix}_${instanceName}.txt")
    }

    /**
     * Returns the non-withdrawn, finished {@link SnvJobResult} for the specified {@link SnvCallingStep} belonging to
     * the latest {@link SnvCallingInstance} that has such a result and is based on the same BAM files as this instance;
     * <code>null</code> if no such {@link SnvCallingInstance} exists.
     */
    SnvJobResult findLatestResultForSameBamFiles(final SnvCallingStep step) {
        assert step
        final SnvJobResult result = atMostOneElement(SnvJobResult.createCriteria().list {
            eq 'step', step
            eq 'withdrawn', false
            eq 'processingState', SnvProcessingStates.FINISHED
            snvCallingInstance {
                sampleType1BamFile {
                    eq 'id', sampleType1BamFile.id
                }
                sampleType2BamFile {
                    eq 'id', sampleType2BamFile.id
                }
            }
            order('snvCallingInstance.id', 'desc')
            maxResults(1)
        })
        if (result != null) {
            assert result.step == step
            assert !result.withdrawn
            assert result.processingState == SnvProcessingStates.FINISHED
            assert result.sampleType1BamFile == sampleType1BamFile
            assert result.sampleType2BamFile == sampleType2BamFile
        }
        return result
    }

    void updateProcessingState(SnvProcessingStates state) {
        assert state : 'The argument "state" is not allowed to be null'
        if (processingState != state) {
            processingState = state
            this.save([flush: true])
        }
    }
}
