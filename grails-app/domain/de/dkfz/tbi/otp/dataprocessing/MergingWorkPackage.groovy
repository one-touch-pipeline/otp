package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.dataprocessing.AbstractMergedBamFile.FileOperationStatus
import de.dkfz.tbi.otp.utils.Entity

/**
 * Represents all generations of one merged BAM file (whereas an {@link AbstractMergedBamFile} represents a single
 * generation). It specifies the concrete criteria for the {@link SeqTrack}s that are merged into the BAM file, and
 * processing parameters used for alignment and merging.
 */
class MergingWorkPackage implements Entity {

    /**
     * The BAM file which moving to the final destination has been initiated for most recently.
     *
     * Note that if {@link AbstractMergedBamFile#fileOperationStatus} is {@link FileOperationStatus#INPROGRESS}, moving
     * is still in progress or has failed, so the file system is in an unclear state.
     * Also note that the referenced BAM file might be withdrawn.
     * If you want to use the referenced BAM file as input for further processing, use
     * {@link #getProcessableBamFileInProjectFolder()}).
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
    static final Collection<String> qualifiedSeqTrackPropertyNames = ['sample', 'seqType', 'seqPlatform.seqPlatformGroup', 'libraryPreparationKit'].asImmutable()
    Sample sample
    SeqType seqType
    SeqPlatformGroup seqPlatformGroup
    LibraryPreparationKit libraryPreparationKit

    // Processing parameters, part of merging criteria
    static final Collection<String> processingParameterNames = ['referenceGenome', 'statSizeFileName', 'workflow'].asImmutable()
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
        libraryPreparationKit nullable: true, validator: {val, obj ->
            SeqTypeNames seqTypeName = obj.seqType?.seqTypeName
            if (seqTypeName == SeqTypeNames.EXOME) {
                return val != null
            } else if (seqTypeName?.isWgbs()) {
                return val == null
            } else {
                return true
            }
        }
        statSizeFileName nullable: true, blank: false, matches: ReferenceGenomeProjectSeqType.TAB_FILE_PATTERN, validator : { val, obj ->
            if (obj.workflow?.name == Workflow.Name.PANCAN_ALIGNMENT) {
                val != null && OtpPath.isValidPathComponent(val)
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

    static final Collection<String> seqTrackPropertyNames = qualifiedSeqTrackPropertyNames.collect{nonQualifiedPropertyName(it)}.asImmutable()

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
        String mergeableSeqTracksQuery = 'FROM SeqTrack WHERE ' + qualifiedSeqTrackPropertyNames.collect { String qualifiedPropertyName ->
            String nonQualifiedPropertyName = nonQualifiedPropertyName(qualifiedPropertyName)
            if (nonQualifiedPropertyName == 'libraryPreparationKit' && seqType.isWgbs()) {
                return null
            }
            def value = this."${nonQualifiedPropertyName}"
            if (value) {
                properties."${nonQualifiedPropertyName}" = value
                return "${qualifiedPropertyName} = :${nonQualifiedPropertyName}"
            } else {
                return "${qualifiedPropertyName} is null"
            }
        }.findAll().join(' AND ')

        return SeqTrack.findAll(mergeableSeqTracksQuery, properties).findAll {
            assert satisfiesCriteria(it)
            return SeqTrackService.mayAlign(it, false)
        }
    }

    static Map getMergingProperties(SeqTrack seqTrack) {
        Collection<String> propertyNames = seqTrackPropertyNames
        if (seqTrack.seqType.isWgbs()) {
            propertyNames -= 'libraryPreparationKit'
        }
        Map properties = [:]
        propertyNames.each {
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

    static nonQualifiedPropertyName(String property) {
        return property.substring(property.lastIndexOf('.') + 1)
    }

    /**
     * Returns the BAM file which is currently in the project folder, or <code>null</code> if there is no BAM file or it
     * is withdrawn or it is unknown which one currently is there.
     *
     * If you use the returned BAM file as input for further processing, ensure that the file on the file system is
     * consistent with the database object by comparing the file size on the file system to
     * {@link AbstractFileSystemBamFile#fileSize}. Perform this check a second time <em>after</em> reading from the file
     * to ensure that the file has not been overwritten between the first check and starting to read the file.
     */
    AbstractMergedBamFile getProcessableBamFileInProjectFolder() {
        if (bamFileInProjectFolder && !bamFileInProjectFolder.withdrawn &&
                bamFileInProjectFolder.fileOperationStatus == FileOperationStatus.PROCESSED) {
            return bamFileInProjectFolder
        } else {
            return null
        }
    }

    static mapping = {
        sample index: "merging_work_package_sample_idx"
        seqType index: "merging_work_package_seq_type_idx"
        referenceGenome index: "merging_work_package_reference_genome_idx"
        needsProcessing index: "merging_work_package_needs_processing_idx"  // partial index: WHERE needs_processing = true
        bamFileInProjectFolder index: "merging_work_package_bam_file_in_project_folder_idx"
    }

    String toStringWithoutIdAndWorkflow() {
        return "${sample} ${seqType} ${seqPlatformGroup} ${referenceGenome} ${libraryPreparationKit ?: ''}"
    }

    @Override
    String toString() {
        return "MWP ${id}: ${toStringWithoutIdAndWorkflow()} ${workflow.name}"
    }
}
