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

    // SeqTrack properties, part of merging criteria
    static final Collection<String> qualifiedSeqTrackPropertyNames = ['sample', 'seqType', 'run.seqPlatform.seqPlatformGroup', 'libraryPreparationKit'].asImmutable()
    SeqPlatformGroup seqPlatformGroup
    LibraryPreparationKit libraryPreparationKit

    // Processing parameters, part of merging criteria
    static final Collection<String> processingParameterNames = ['referenceGenome', 'statSizeFileName', 'pipeline'].asImmutable()
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
            MergingWorkPackage mergingWorkPackage = CollectionUtils.atMostOneElement(MergingWorkPackage.findAllBySampleAndSeqType(val, obj.seqType),
                    "More than one MWP exists for sample ${val} and seqType ${obj.seqType}")
            if (mergingWorkPackage && mergingWorkPackage.id != obj.id) {
                return "The mergingWorkPackage must be unique for one sample and seqType"
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

    static final Collection<String> seqTrackPropertyNames = qualifiedSeqTrackPropertyNames.collect{nonQualifiedPropertyName(it)}.asImmutable()

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
