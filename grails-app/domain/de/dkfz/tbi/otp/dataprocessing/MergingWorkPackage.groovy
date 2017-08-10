package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*

/**
 * Represents all generations of one merged BAM file (whereas an {@link AbstractMergedBamFile} represents a single
 * generation). It specifies the concrete criteria for the {@link SeqTrack}s that are merged into the BAM file, and
 * processing parameters used for alignment and merging.
 */
class MergingWorkPackage extends AbstractMergingWorkPackage {

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

    SeqPlatformGroup seqPlatformGroup
    LibraryPreparationKit libraryPreparationKit

    // Processing parameters, part of merging criteria
    static final Collection<String> processingParameterNames = ['referenceGenome', 'statSizeFileName', 'pipeline', 'antibodyTarget'].asImmutable()
    String statSizeFileName

    //reference genome depending options
    Set<MergingWorkPackageAlignmentProperty> alignmentProperties

    boolean needsProcessing

    Set<SeqTrack> seqTracks

    static hasMany = [
            alignmentProperties: MergingWorkPackageAlignmentProperty,
            seqTracks: SeqTrack
    ]

    static constraints = {
        // TODO OTP-1401: In the future there may be more than one MWP for one sample and seqType.
        // As soon as you loosen this constraint, un-ignore:
        // - AlignmentPassUnitTests.testIsLatestPass_2PassesDifferentWorkPackages
        sample(validator: {val, obj ->
            MergingWorkPackage mergingWorkPackage = CollectionUtils.atMostOneElement(MergingWorkPackage.findAllBySampleAndSeqTypeAndAntibodyTarget(val, obj.seqType, obj.antibodyTarget),
                    "More than one MWP exists for sample ${val} and seqType ${obj.seqType} and antibodyTarget ${obj.antibodyTarget}")
            if (mergingWorkPackage && mergingWorkPackage.id != obj.id) {
                return "The mergingWorkPackage must be unique for one sample and seqType and antibodyTarget"
            }
        })

        needsProcessing(validator: {val, obj -> !val || [Pipeline.Name.PANCAN_ALIGNMENT, Pipeline.Name.RODDY_RNA_ALIGNMENT].contains(obj?.pipeline?.name)})
        pipeline(validator: { pipeline ->
            pipeline.type == Pipeline.Type.ALIGNMENT &&
            pipeline.name != Pipeline.Name.EXTERNALLY_PROCESSED
        })

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
            if (obj.pipeline?.name == Pipeline.Name.PANCAN_ALIGNMENT) {
                val != null && OtpPath.isValidPathComponent(val)
            } else if (obj.pipeline?.name == Pipeline.Name.DEFAULT_OTP) {
                val == null
            } else if (obj.pipeline?.name == Pipeline.Name.EXTERNALLY_PROCESSED) {
                val == null
            } else if (obj.pipeline?.name == Pipeline.Name.RODDY_RNA_ALIGNMENT) {
               return val == null
            } else {
                assert false: "Pipeline name is unknown: ${obj.pipeline?.name}"
            }
        }
    }


    Collection<SeqTrack> findMergeableSeqTracks() {
        return seqTracks ?: []
    }

    static Map getMergingProperties(SeqTrack seqTrack) {
        Map<String, Entity> properties = [
                sample          : seqTrack.sample,
                seqType         : seqTrack.seqType,
                seqPlatformGroup: seqTrack.seqPlatformGroup,
        ]

        if (!seqTrack.seqType.isWgbs()) {
            properties += [libraryPreparationKit: seqTrack.libraryPreparationKit]
        }
        if (seqTrack.seqType.isChipSeq()) {
            properties += [antibodyTarget: ((ChipSeqSeqTrack)seqTrack).antibodyTarget]
        }
        return properties
    }

    boolean satisfiesCriteria(SeqTrack seqTrack) {
        return getMergingProperties(seqTrack).every { key, value -> value?.id == this."${key}"?.id }
    }

    boolean satisfiesCriteria(final AbstractBamFile bamFile) {
        return bamFile.mergingWorkPackage.id == id
    }

    AbstractMergedBamFile getCompleteProcessableBamFileInProjectFolder() {
        AbstractMergedBamFile bamFile = getProcessableBamFileInProjectFolder()
        if (bamFile && bamFile.containedSeqTracks == findMergeableSeqTracks().toSet()) {
            return bamFile
        } else {
            return null
        }
    }

    static mapping = {
        needsProcessing index: "merging_work_package_needs_processing_idx"  // partial index: WHERE needs_processing = true
        alignmentProperties cascade: "all-delete-orphan"
    }

    String toStringWithoutIdAndPipeline() {
        return "${sample} ${seqType} ${libraryPreparationKit ?: ''} ${seqPlatformGroup} ${referenceGenome}"
    }

    @Override
    String toString() {
        return "MWP ${id}: ${toStringWithoutIdAndPipeline()} ${pipeline.name}"
    }
}
