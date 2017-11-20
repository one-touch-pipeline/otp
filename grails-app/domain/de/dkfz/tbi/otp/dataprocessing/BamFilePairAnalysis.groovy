package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.*
import de.dkfz.tbi.otp.dataprocessing.AbstractMergedBamFile.FileOperationStatus
import de.dkfz.tbi.otp.dataprocessing.AbstractMergedBamFile.QcTrafficLightStatus
import de.dkfz.tbi.otp.dataprocessing.roddyExecution.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*
import de.dkfz.tbi.otp.utils.logging.*
import org.hibernate.*

abstract class BamFilePairAnalysis implements ProcessParameterObject, Entity {
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
     * Used to construct paths in {@link SnvCallingInstance#getInstancePath()}/{@link IndelCallingInstance#getInstancePath()} and {@link SnvCallingInstance#getConfigFilePath()}.
     * For example 2014-08-25_15h32.
     */
    String instanceName

    Date dateCreated

    Date lastUpdated

    boolean withdrawn = false

    SamplePair samplePair

    static belongsTo = SamplePair

    /**
     * The overall processing state of this analysis run.
     * Because the analysis StartJob creates an instance of a BamFilePairAnalysis immediately when starting it, this will always start
     * as {@link AnalysisProcessingStates#IN_PROGRESS}.
     */
    AnalysisProcessingStates processingState = AnalysisProcessingStates.IN_PROGRESS

    Comment comment

    QcTrafficLightStatus qcTrafficLightStatus


    static constraints = {
        sampleType1BamFile validator: { AbstractMergedBamFile val, BamFilePairAnalysis obj ->
            obj.samplePair &&
                    val.fileOperationStatus == FileOperationStatus.PROCESSED &&
                    val.mergingWorkPackage.id == obj.samplePair.mergingWorkPackage1.id
        }
        sampleType2BamFile validator: { AbstractMergedBamFile val, BamFilePairAnalysis obj ->
            obj.samplePair &&
                    val.fileOperationStatus == FileOperationStatus.PROCESSED &&
                    val.mergingWorkPackage.id == obj.samplePair.mergingWorkPackage2.id
        }
        latestDataFileCreationDate validator: { Date latestDataFileCreationDate, BamFilePairAnalysis instance ->
            latestDataFileCreationDate == AbstractBamFile.getLatestSequenceDataFileCreationDate(instance.sampleType1BamFile, instance.sampleType2BamFile)
        }
        instanceName blank: false, unique: 'samplePair', validator: { OtpPath.isValidPathComponent(it) }
        config validator: { val ->
            (SnvConfig.isAssignableFrom(Hibernate.getClass(val)) ||
                    RoddyWorkflowConfig.isAssignableFrom(Hibernate.getClass(val))) &&
                    val?.pipeline?.type != Pipeline.Type.ALIGNMENT
        }
        qcTrafficLightStatus nullable: true, validator: { status, obj ->
            if ([QcTrafficLightStatus.ACCEPTED, QcTrafficLightStatus.REJECTED, QcTrafficLightStatus.BLOCKED].contains(status) && !obj.comment) {
                return "a comment is required then the QC status is set to ACCEPTED, REJECTED or BLOCKED"
            }
        }
        comment nullable: true
    }

    static mapping = {
        sampleType1BamFile index: "bam_file_pair_analysis_sample_type_1_bam_file_idx"
        sampleType2BamFile index: "bam_file_pair_analysis_sample_type_2_bam_file_idx"
        samplePair index: "bam_file_pair_analysis_sample_pair_idx"
        config lazy: false
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

    @Override
    Set<SeqTrack> getContainedSeqTracks() {
        return sampleType1BamFile.containedSeqTracks + sampleType2BamFile.containedSeqTracks
    }

    File getWorkDirectory() {
        return getInstancePath().absoluteDataManagementPath
    }

    void updateProcessingState(AnalysisProcessingStates state) {
        assert state: 'The argument "state" is not allowed to be null'
        if (processingState != state) {
            processingState = state
            this.save([flush: true])
        }
    }

    abstract OtpPath getInstancePath()

    void withdraw() {
        withTransaction {
            withdrawn = true
            assert save(flush: true)
            LogThreadLocal.threadLog.info("Withdrawing ${this}")
        }
    }
}
