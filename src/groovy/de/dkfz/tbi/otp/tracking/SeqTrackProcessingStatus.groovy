package de.dkfz.tbi.otp.tracking

import groovy.transform.TupleConstructor

import de.dkfz.tbi.otp.ngsdata.SeqTrack

import static de.dkfz.tbi.otp.tracking.ProcessingStatus.WorkflowProcessingStatus

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

    WorkflowProcessingStatus getIndelProcessingStatus() {
        return TrackingService.combineStatuses(mergingWorkPackageProcessingStatuses, { it.indelProcessingStatus })
    }

    WorkflowProcessingStatus getSophiaProcessingStatus() {
        return TrackingService.combineStatuses(mergingWorkPackageProcessingStatuses, { it.sophiaProcessingStatus })
    }

    WorkflowProcessingStatus getAceseqProcessingStatus() {
        return TrackingService.combineStatuses(mergingWorkPackageProcessingStatuses, { it.aceseqProcessingStatus } )
    }

    WorkflowProcessingStatus getRunYapsaProcessingStatus() {
        return TrackingService.combineStatuses(mergingWorkPackageProcessingStatuses, { it.runYapsaProcessingStatus } )
    }
}
