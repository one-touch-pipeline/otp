package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.ngsdata.*

/**
 * Represents a merged bam file stored on the file system
 * and produced by the merging process identified by the
 * given {@link MergingPass}
 *
 *
 */
class ProcessedMergedBamFile extends AbstractMergedBamFile {

    static belongsTo = [
        mergingPass: MergingPass
    ]

    static constraints = {
        mergingPass nullable: false, unique: true
    }

    MergingSet getMergingSet() {
        return mergingPass.mergingSet
    }

    @Override
    MergingWorkPackage getMergingWorkPackage() {
        return mergingPass.mergingWorkPackage
    }

    /**
     * @return <code>true</code>, if this {@link ProcessedMergedBamFile} is from the latest merging
     * @see MergingPass#isLatestPass()
     */
    @Override
    public boolean isMostRecentBamFile() {
        return (mergingPass.isLatestPass() && mergingSet.isLatestSet())
    }

    @Override
    public String toString() {
        MergingWorkPackage mergingWorkPackage = mergingPass.mergingSet.mergingWorkPackage
        return "id: ${id} " +
        "pass: ${mergingPass.identifier} " + (mergingPass.latestPass ? "(latest) " : "") +
        "set: ${mergingSet.identifier} " + (mergingSet.latestSet ? "(latest) " : "") +
        "<br>sample: ${mergingWorkPackage.sample} " +
        "seqType: ${mergingWorkPackage.seqType} " +
        "<br>project: ${mergingWorkPackage.project}"
    }

    static mapping = { mergingPass index: "abstract_bam_file_merging_pass_idx" }

    @Override
    Set<SeqTrack> getContainedSeqTracks() {
        final Set<SeqTrack> seqTracks = mergingSet.containedSeqTracks
        if (seqTracks.empty) {
            throw new IllegalStateException("MergingSet ${mergingSet} is empty.")
        }
        return seqTracks
    }

    @Override
    AbstractQualityAssessment getOverallQualityAssessment() {
        OverallQualityAssessmentMerged.createCriteria().get {
            qualityAssessmentMergedPass {
                eq 'processedMergedBamFile', this
            }
            order 'id', 'desc'
            maxResults 1
        }
    }
}
