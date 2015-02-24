package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.ngsdata.SeqTrack

/**
 * An alignment decider decides how a {@link SeqTrack} will be aligned and merged, and triggers the workflow(s) which
 * will do the alignment and merging.
 */
interface AlignmentDecider {

    /**
     * Decides how the specified {@link SeqTrack} will be aligned, and triggers the workflow(s) which will do the
     * alignment.
     *
     * Finds and/or creates zero, one or more {@link MergingWorkPackage}s which specify how the {@link SeqTrack} will be
     * aligned.
     *
     * @param forceRealign If {@code false}, the method will trigger the alignment only for {@link MergingWorkPackage}s
     * which the alignment of the {@link SeqTrack} has not been triggered for. If {@code true}, the method will trigger
     * the alignment of the {@link SeqTrack} for all found or created {@link MergingWorkPackage}s.
     *
     * @return All found or created {@link MergingWorkPackage}s for the {@link SeqTrack}, regardless of whether the
     * method triggered an alignment for it.
     */
    Collection<MergingWorkPackage> decideAndPrepareForAlignment(SeqTrack seqTrack, boolean forceRealign)
}
