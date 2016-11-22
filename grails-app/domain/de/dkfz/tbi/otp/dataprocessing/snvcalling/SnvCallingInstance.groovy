package de.dkfz.tbi.otp.dataprocessing.snvcalling

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.AbstractMergedBamFile.FileOperationStatus
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.RoddyWorkflowConfig
import de.dkfz.tbi.otp.job.processing.ProcessParameterObject
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.Entity
import org.hibernate.*

import static de.dkfz.tbi.otp.utils.CollectionUtils.atMostOneElement


/**
 * For each tumor-control pair the snv pipeline will be called.
 * The SnvCallingInstance symbolizes one call of the pipeline.
 *
 *
 */
class SnvCallingInstance implements ProcessParameterObject, Entity {
    /**
     * Refers to the config file which is stored in the database and is used as a basis for all the files in the filesystem.
     */
    ConfigPerProject config

    AbstractMergedBamFile sampleType1BamFile

    AbstractMergedBamFile sampleType2BamFile

    /**
     * The maximum value of {@link DataFile#dateCreated} of all {@link DataFile}s that have been merged into one of
     * {@link #sampleType1BamFile} and {@link #sampleType2BamFile}.
     */
    Date latestDataFileCreationDate

    /**
     * Used to construct paths in {@link #getSnvInstancePath()} and {@link #getConfigFilePath()}.
     * For example 2014-08-25_15h32.
     */
    String instanceName

    Date dateCreated

    Date lastUpdated

    boolean withdrawn = false

    SamplePair samplePair

    static belongsTo = SamplePair

    /**
     * The overall processing state of this SNV calling run.
     * Because the SNV StartJob creates an instance of a SnvCallingInstance immediately when starting it, this will always start
     * as {@link AnalysisProcessingStates#IN_PROGRESS}.
     */
    AnalysisProcessingStates processingState = AnalysisProcessingStates.IN_PROGRESS

    static constraints = {
        sampleType1BamFile validator: { AbstractMergedBamFile val, SnvCallingInstance obj ->
            obj.samplePair &&
                    val.fileOperationStatus == FileOperationStatus.PROCESSED &&
                    val.mergingWorkPackage.id == obj.samplePair.mergingWorkPackage1.id}
        sampleType2BamFile validator: { AbstractMergedBamFile val, SnvCallingInstance obj ->
            obj.samplePair &&
                    val.fileOperationStatus == FileOperationStatus.PROCESSED &&
                    val.mergingWorkPackage.id == obj.samplePair.mergingWorkPackage2.id}
        latestDataFileCreationDate validator: { Date latestDataFileCreationDate, SnvCallingInstance instance ->
            latestDataFileCreationDate == AbstractBamFile.getLatestSequenceDataFileCreationDate(instance.sampleType1BamFile, instance.sampleType2BamFile)
        }
        instanceName blank: false, unique: 'samplePair', validator: { OtpPath.isValidPathComponent(it) }
        processingState validator: {val, obj ->
            // there must be at least one withdrawn {@link SnvJobResult}
            // if {@link this#withdrawn} is true
            if (obj.withdrawn == true) {
                return !SnvJobResult.findAllBySnvCallingInstanceAndWithdrawn(obj, true).empty || SnvJobResult.findAllBySnvCallingInstance(obj).empty
            } else {
                return true
            }
        }
        config validator: { val ->
            (SnvConfig.isAssignableFrom(Hibernate.getClass(val)) ||
                    RoddyWorkflowConfig.isAssignableFrom(Hibernate.getClass(val))) &&
                    val?.pipeline?.type == Pipeline.Type.SNV
        }
    }

    static mapping = {
        sampleType1BamFile index: "snv_calling_instance_sample_type_1_bam_file_idx"
        sampleType2BamFile index: "snv_calling_instance_sample_type_2_bam_file_idx"
        samplePair index: "snv_calling_instance_sample_pair_idx"
    }

    Project getProject() {
        return samplePair.project
    }

    @Override
    short getProcessingPriority() {
        return project.processingPriority
    }

    @Override
    Individual getIndividual() {
        return samplePair.individual
    }

    @Override
    SeqType getSeqType() {
        return samplePair.seqType
    }

    /**
     * Example: ${project}/sequencing/exon_sequencing/view-by-pid/${pid}/snv_results/paired/tumor_control/2014-08-25_15h32
     */
    OtpPath getSnvInstancePath() {
        return new OtpPath(samplePair.samplePairPath, instanceName)
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
        return new OtpPath(samplePair.samplePairPath, "config_${step.configFileNameSuffix}_${instanceName}.txt")
    }

    OtpPath getAllSNVdiagnosticsPlots() {
        return new OtpPath(snvInstancePath, "snvs_${getIndividual().pid}_allSNVdiagnosticsPlots.pdf")
    }

    @Override
    Set<SeqTrack> getContainedSeqTracks() {
        return sampleType1BamFile.containedSeqTracks + sampleType2BamFile.containedSeqTracks
    }

    /**
     * Returns the non-withdrawn, finished {@link SnvJobResult} for the specified {@link SnvCallingStep} belonging to
     * the latest (even {@link SnvCallingInstance#withdrawn}) {@link SnvCallingInstance} that has such a result and is based on the same BAM files as this instance;
     * <code>null</code> if no such {@link SnvCallingInstance} exists.
     */
    SnvJobResult findLatestResultForSameBamFiles(final SnvCallingStep step) {
        assert step
        final SnvJobResult result = atMostOneElement(SnvJobResult.createCriteria().list {
            eq 'step', step
            eq 'withdrawn', false
            eq 'processingState', AnalysisProcessingStates.FINISHED
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
            assert result.processingState == AnalysisProcessingStates.FINISHED
            assert result.sampleType1BamFile.id == sampleType1BamFile.id
            assert result.sampleType2BamFile.id == sampleType2BamFile.id
        }
        return result
    }

    void updateProcessingState(AnalysisProcessingStates state) {
        assert state : 'The argument "state" is not allowed to be null'
        if (processingState != state) {
            processingState = state
            this.save([flush: true])
        }
    }

    SnvCallingInstance getPreviousFinishedInstance() {
        return SnvCallingInstance.findBySamplePairAndProcessingStateAndIdLessThan(samplePair, AnalysisProcessingStates.FINISHED, this.id, [max: 1, sort: 'id', order: 'desc'])
    }

    @Override
    public String toString() {
        return "SCI ${id}: ${instanceName} ${samplePair.toStringWithoutId()}"
    }
}
