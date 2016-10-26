package de.dkfz.tbi.otp.tracking

import groovy.transform.*

import static de.dkfz.tbi.otp.tracking.ProcessingStatus.Done.*

@TupleConstructor
public class ProcessingStatus {

    @TupleConstructor
    static enum WorkflowProcessingStatus {
        NOTHING_DONE_WONT_DO(NOTHING, false),
        NOTHING_DONE_MIGHT_DO(NOTHING, true),
        PARTLY_DONE_WONT_DO_MORE(PARTLY, false),
        PARTLY_DONE_MIGHT_DO_MORE(PARTLY, true),
        ALL_DONE(ALL, false)

        final Done done
        final boolean mightDoMore
    }

    static enum Done {
        NOTHING,
        PARTLY,
        ALL,
    }

    final Collection<SeqTrackProcessingStatus> seqTrackProcessingStatuses

    Collection<MergingWorkPackageProcessingStatus> getMergingWorkPackageProcessingStatuses() {
        return ((Collection<MergingWorkPackageProcessingStatus>) seqTrackProcessingStatuses*.mergingWorkPackageProcessingStatuses.flatten()).unique()
    }

    Collection<SamplePairProcessingStatus> getSamplePairProcessingStatuses() {
        return ((Collection<SamplePairProcessingStatus>) seqTrackProcessingStatuses*.mergingWorkPackageProcessingStatuses*.samplePairProcessingStatuses.flatten()).unique()
    }

    WorkflowProcessingStatus getInstallationProcessingStatus() {
        return TrackingService.combineStatuses(seqTrackProcessingStatuses, { it.installationProcessingStatus })
    }

    WorkflowProcessingStatus getFastqcProcessingStatus() {
        return TrackingService.combineStatuses(seqTrackProcessingStatuses, { it.fastqcProcessingStatus })
    }

    WorkflowProcessingStatus getAlignmentProcessingStatus() {
        return TrackingService.combineStatuses(seqTrackProcessingStatuses, { it.alignmentProcessingStatus })
    }

    WorkflowProcessingStatus getSnvProcessingStatus() {
        return TrackingService.combineStatuses(seqTrackProcessingStatuses, { it.snvProcessingStatus })
    }

    @Override
    public String toString() {
        return """
Installation: ${installationProcessingStatus}
FastQC:       ${fastqcProcessingStatus}
Alignment:    ${alignmentProcessingStatus}
SNV:          ${snvProcessingStatus}
"""
    }
}
