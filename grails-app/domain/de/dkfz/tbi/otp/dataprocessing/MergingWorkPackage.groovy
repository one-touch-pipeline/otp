package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.ngsdata.*

/**
 * Represents all generations of one merged BAM file (whereas {@link ProcessedMergedBamFile} represents a single
 * generation). It specifies the concrete criteria for the {@link SeqTrack}s that are merged into the BAM file, and
 * processing parameters used for alignment and merging.
 */
class MergingWorkPackage {

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
    Sample sample
    SeqType seqType
    SeqPlatformGroup seqPlatformGroup
    SequencingKit sequencingKit

    // Processing parameters, part of merging criteria
    ReferenceGenome referenceGenome

    static belongsTo = Sample

    static constraints = {
        // TODO OTP-1401: In the future there may be more than one MWP for one sample and seqType.
        // As soon a you loosen this constraint, un-ignore:
        // - AlignmentPassUnitTests.testIsLatestPass_2PassesDifferentWorkPackages
        // - MergingCriteriaSpecificServiceTests.testProcessedBamFilesForMerging_TwoBamFilesSeqPlatformNotEqual
        // - MergingCriteriaSpecificServiceTests.testProcessedMergedBamFileForMerging_OnlyOneMergedBamFileWrongPlatformWrongName
        // - MergingCriteriaSpecificServiceTests.testProcessedMergedBamFileForMerging_TwoMergedBamFileDifferentPlatform
        sample unique: 'seqType'
        sequencingKit nullable: true
    }

    Project getProject() {
        return sample.project
    }

    Individual getIndividual() {
        return sample.individual
    }

    SampleType getSampleType() {
        return sample.sampleType
    }

    static Map getMergingProperties(SeqTrack seqTrack) {
        SequencingKit sequencingKit = seqTrack.sequencingKit
        // TODO OTP-1409: This hardcoded implementation will be replaced.
        if (['V1', 'V2', 'V3'].contains(sequencingKit?.name) && seqTrack.seqPlatformGroup.name == 'HiSeq 2000/2500') {
            sequencingKit = null
        }
        return [
                sample: seqTrack.sample,
                seqType: seqTrack.seqType,
                seqPlatformGroup: seqTrack.seqPlatformGroup,
                sequencingKit: sequencingKit,
        ]
    }

    boolean satisfiesCriteria(SeqTrack seqTrack) {
        return getMergingProperties(seqTrack).every { key, value -> value?.id == this."${key}"?.id }
    }

    boolean satisfiesCriteria(final AbstractBamFile bamFile) {
        return bamFile.mergingWorkPackage.id == id
    }

    static mapping = {
        sample index: "merging_work_package_sample_idx"
        seqType index: "merging_work_package_seq_type_idx"
        referenceGenome index: "merging_work_package_reference_genome_idx"
    }

    @Override
    String toString() {
        return "MWP ${id}: ${sample} ${seqType} ${seqPlatformGroup} ${sequencingKit} ${referenceGenome}"
    }
}
