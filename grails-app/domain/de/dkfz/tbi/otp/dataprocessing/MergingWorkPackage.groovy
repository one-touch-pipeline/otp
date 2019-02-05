/*
 * Copyright 2011-2019 The OTP authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.utils.Entity

import static de.dkfz.tbi.otp.utils.CollectionUtils.atMostOneElement

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
    String statSizeFileName

    //reference genome depending options
    Set<MergingWorkPackageAlignmentProperty> alignmentProperties

    boolean needsProcessing

    Set<SeqTrack> seqTracks

    static hasMany = [
            alignmentProperties: MergingWorkPackageAlignmentProperty,
            seqTracks          : SeqTrack,
    ]

    static constraints = {
        // TODO OTP-1401: In the future there may be more than one MWP for one sample and seqType.
        // As soon as you loosen this constraint, un-ignore:
        // - AlignmentPassUnitTests.testIsLatestPass_2PassesDifferentWorkPackages
        sample(validator: { val, obj ->
            MergingWorkPackage mergingWorkPackage = CollectionUtils.atMostOneElement(
                    MergingWorkPackage.findAllBySampleAndSeqTypeAndAntibodyTarget(val, obj.seqType, obj.antibodyTarget),
                    "More than one MWP exists for sample ${val} and seqType ${obj.seqType} and antibodyTarget ${obj.antibodyTarget}")
            if (mergingWorkPackage && mergingWorkPackage.id != obj.id) {
                return "The mergingWorkPackage must be unique for one sample and seqType and antibodyTarget"
            }
        })

        needsProcessing(validator: { val, obj ->
            !val || obj?.pipeline?.name in Pipeline.Name.getAlignmentPipelineNames()
        })
        pipeline(validator: { pipeline ->
            pipeline.type == Pipeline.Type.ALIGNMENT &&
                    pipeline.name != Pipeline.Name.EXTERNALLY_PROCESSED
        })

        libraryPreparationKit validator: { val, obj ->
            SeqTypeNames seqTypeName = obj.seqType?.seqTypeName
            if (seqTypeName == SeqTypeNames.EXOME) {
                return val != null
            } else if (seqTypeName?.isWgbs()) {
                /*
                    WGBS can, for experimental reasons, have lanes with different libPrepKits, that still need to be
                    merged. This is OK as long as they all end up using the same Adapter File. The WGBS-alignment for
                    unique(MWP*.seqTracks*.libraryPreparationKit*.adapterfile)
                */
                return val == null
            } else {
                return true
            }
        }

        statSizeFileName nullable: true, blank: false, matches: ReferenceGenomeProjectSeqType.TAB_FILE_PATTERN, validator: { val, obj ->
            switch (obj.pipeline?.name) {
                case Pipeline.Name.CELL_RANGER:
                case Pipeline.Name.DEFAULT_OTP:
                case Pipeline.Name.EXTERNALLY_PROCESSED:
                    return val == null
                case Pipeline.Name.PANCAN_ALIGNMENT:
                    return val != null && OtpPath.isValidPathComponent(val)
                case Pipeline.Name.RODDY_RNA_ALIGNMENT:
                    return val == null
                default:
                    throw new RuntimeException("Pipeline name is unknown: ${obj.pipeline?.name}")
            }
        }

        seqTracks(validator: { val, obj ->
            val.each {
                if (!obj.satisfiesCriteria(it)) {
                    return false
                }
            }
            return true
        })
    }

    static Map getMergingProperties(SeqTrack seqTrack) {
        Map<String, Entity> properties = [
                sample          : seqTrack.sample,
                seqType         : seqTrack.seqType,
                seqPlatformGroup: seqTrack.seqPlatformGroup,
        ]
        if (atMostOneElement(MergingCriteria.findAllByProjectAndSeqType(seqTrack.project, seqTrack.seqType)).useLibPrepKit) {
            properties += [libraryPreparationKit: seqTrack.libraryPreparationKit]
        }
        if (seqTrack.seqType.isChipSeq()) {
            properties += [antibodyTarget: ((ChipSeqSeqTrack) seqTrack).antibodyTarget]
        }
        return properties
    }

    boolean satisfiesCriteria(SeqTrack seqTrack) {
        return getMergingProperties(seqTrack).every { key, value -> value?.id == this."${key}"?.id }
    }

    boolean satisfiesCriteria(final AbstractBamFile bamFile) {
        return bamFile.mergingWorkPackage.id == id
    }

    @Override
    AbstractMergedBamFile getBamFileThatIsReadyForFurtherAnalysis() {
        AbstractMergedBamFile bamFile = getProcessableBamFileInProjectFolder()
        if (bamFile && bamFile.containedSeqTracks == seqTracks) {
            return bamFile
        } else {
            return null
        }
    }

    static mapping = {
        needsProcessing index: "merging_work_package_needs_processing_idx"
        // partial index: WHERE needs_processing = true
        alignmentProperties cascade: "all-delete-orphan"
    }

    String toStringWithoutIdAndPipeline() {
        return "${sample} ${seqType} ${libraryPreparationKit ?: ''} ${referenceGenome}"
    }

    @Override
    String toString() {
        return "MWP ${id}: ${toStringWithoutIdAndPipeline()} ${pipeline?.name}"
    }
}
