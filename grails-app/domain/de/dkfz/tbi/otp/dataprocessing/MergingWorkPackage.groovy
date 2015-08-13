package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.ngsdata.*

/**
 * Represents all generations of one merged BAM file (whereas an {@link AbstractMergedBamFile} represents a single
 * generation). It specifies the concrete criteria for the {@link SeqTrack}s that are merged into the BAM file, and
 * processing parameters used for alignment and merging.
 */
class MergingWorkPackage {

    /**
     * The BAM file which moving to the final destination has been initiated for most recently.
     * Note that if {@link AbstractMergedBamFile#fileOperationStatus} is {@link FileOperationStatus#INPROGRESS}, moving is still in progress or has failed, so the file system is in an unclear state.
     * Also note that the referenced BAM file might be withdrawn.
     * If you use the referenced BAM file as input for further processing,
     * <ul>
     *     <li>ensure that it is not withdrawn,</li>
     *     <li>ensure that its {@link AbstractMergedBamFile#fileOperationStatus} is {@link FileOperationStatus#PROCESSED},</li>
     *     <li>ensure that the file on the file system is consistent with the database object by comparing the file size on the file system to {@link AbstractFileSystemBamFile#fileSize}. Perform this check a second time <em>after</em> reading from the file to ensure that the file has not been overwritten between the first check and starting to read the file.</li>
     * </ul>
     */
    /*
     * Due some strange behavior of GORM (?), this has to be set on null explicitly where objects of
     * {@link de.dkfz.tbi.otp.dataprocessing.AbstractMergedBamFile} or its subclasses are built or created.
     * See {@link de.dkfz.tbi.otp.ngsdata.DomainFactory#createProcessedMergedBamFile}
     */
    AbstractMergedBamFile bamFileInProjectFolder

    /**
     * Identifies the way how this {@link MergingWorkPackage} is maintained (updated):
     */
    enum ProcessingType {
        /**
         * Files to be merged are defined by the user according to his own rules
         * (but still criteria can be used).
         * The new {@link MergingSet} are added to the {@link MergingWorkPackage} manually.
         * The user defines when to start processing of certain {@link MergingSet} from
         * this {@link MergingWorkPackage}
         */
        MANUAL,
        /**
         * The {@link MergingWorkPackage} is maintained by the system.
         * New {@link MergingSet}s are added by the system based on the presence of
         * new {@link ProcessedBamFile}s.
         * Processing of {@link MergingSet} is started automatically.
         */
        SYSTEM
    }

    ProcessingType processingType = ProcessingType.SYSTEM

    // SeqTrack properties, part of merging criteria
    static final Collection<String> qualifiedSeqTrackPropertyNames = ['sample', 'seqType', 'seqPlatform.seqPlatformGroup'].asImmutable()
    Sample sample
    SeqType seqType
    SeqPlatformGroup seqPlatformGroup

    // Processing parameters, part of merging criteria
    ReferenceGenome referenceGenome
    String statSizeFileName
    Workflow workflow

    boolean needsProcessing

    static belongsTo = Sample

    static constraints = {
        // TODO OTP-1401: In the future there may be more than one MWP for one sample and seqType.
        // As soon a you loosen this constraint, un-ignore:
        // - AlignmentPassUnitTests.testIsLatestPass_2PassesDifferentWorkPackages
        sample unique: 'seqType'
        needsProcessing(validator: {val, obj -> !val || obj.workflow.name == Workflow.Name.PANCAN_ALIGNMENT})
        workflow(validator: {workflow -> workflow.type == Workflow.Type.ALIGNMENT})
        statSizeFileName nullable: true, blank: false, matches: ReferenceGenomeProjectSeqType.TAB_FILE_PATTERN, validator : { val, obj ->
            if (obj.workflow?.name == Workflow.Name.PANCAN_ALIGNMENT) {
                val != null
            } else if (obj.workflow?.name == Workflow.Name.DEFAULT_OTP) {
                val == null
            } else {
                assert false: "Workflow name is unknown: ${obj.workflow?.name}"
            }
        }
        bamFileInProjectFolder nullable: true, validator: { AbstractMergedBamFile val, MergingWorkPackage obj ->
            if(val) {
                val.workPackage.id == obj.id && [AbstractMergedBamFile.FileOperationStatus.INPROGRESS, AbstractMergedBamFile.FileOperationStatus.PROCESSED].contains(val.fileOperationStatus)
            } else {
                return true
            }
        }
    }

    static final Collection<String> seqTrackPropertyNames = qualifiedSeqTrackPropertyNames.collect{nonQualifiedPropertyName(it)}
    static final String mergeableSeqTracksQuery = 'FROM SeqTrack WHERE ' + qualifiedSeqTrackPropertyNames.collect{"${it} = :${nonQualifiedPropertyName(it)}"}.join(' AND ')

    Project getProject() {
        return sample.project
    }

    Individual getIndividual() {
        return sample.individual
    }

    SampleType getSampleType() {
        return sample.sampleType
    }

    Collection<SeqTrack> findMergeableSeqTracks() {
        Map properties = [:]
        seqTrackPropertyNames.each {
            properties."${it}" = this."${it}"
        }
        return SeqTrack.findAll(mergeableSeqTracksQuery, properties).findAll {
            assert satisfiesCriteria(it)
            return SeqTrackService.mayAlign(it)
        }
    }

    static Map getMergingProperties(SeqTrack seqTrack) {
        Map properties = [:]
        seqTrackPropertyNames.each {
            properties."${it}" = seqTrack."${it}"
        }
        return properties
    }

    boolean satisfiesCriteria(SeqTrack seqTrack) {
        return getMergingProperties(seqTrack).every { key, value -> value?.id == this."${key}"?.id }
    }

    boolean satisfiesCriteria(final AbstractBamFile bamFile) {
        return bamFile.mergingWorkPackage.id == id
    }

    private static nonQualifiedPropertyName(String property) {
        return property.substring(property.lastIndexOf('.') + 1)
    }

    static mapping = {
        sample index: "merging_work_package_sample_idx"
        seqType index: "merging_work_package_seq_type_idx"
        referenceGenome index: "merging_work_package_reference_genome_idx"
        needsProcessing index: "merging_work_package_needs_processing_idx"  // partial index: WHERE needs_processing = true
        bamFileInProjectFolder index: "merging_work_package_bam_file_in_project_folder_idx"
    }

    String toStringWithoutIdAndWorkflow() {
        return "${sample} ${seqType} ${seqPlatformGroup} ${referenceGenome}"
    }

    @Override
    String toString() {
        return "MWP ${id}: ${toStringWithoutIdAndWorkflow()} ${workflow}"
    }
}
