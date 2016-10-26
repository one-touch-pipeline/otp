package de.dkfz.tbi.otp.tracking

import de.dkfz.tbi.otp.ngsdata.*
import groovy.transform.*

import static de.dkfz.tbi.otp.tracking.ProcessingStatus.*

@TupleConstructor
class SeqTrackProcessingStatus {

    final SeqTrack seqTrack

    final WorkflowProcessingStatus installationProcessingStatus
    final WorkflowProcessingStatus fastqcProcessingStatus

    final Collection<MergingWorkPackageProcessingStatus> mergingWorkPackageProcessingStatuses

    WorkflowProcessingStatus getAlignmentProcessingStatus() {
        return TrackingService.combineStatuses(mergingWorkPackageProcessingStatuses, { it.alignmentProcessingStatus })
    }

    WorkflowProcessingStatus getSnvProcessingStatus() {
        return TrackingService.combineStatuses(mergingWorkPackageProcessingStatuses, { it.snvProcessingStatus })
    }
}
