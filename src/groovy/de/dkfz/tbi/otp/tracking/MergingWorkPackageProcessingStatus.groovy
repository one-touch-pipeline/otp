package de.dkfz.tbi.otp.tracking

import de.dkfz.tbi.otp.dataprocessing.*
import groovy.transform.*

import static de.dkfz.tbi.otp.tracking.ProcessingStatus.*

@TupleConstructor
class MergingWorkPackageProcessingStatus {

    final MergingWorkPackage mergingWorkPackage

    final WorkflowProcessingStatus alignmentProcessingStatus

    final AbstractMergedBamFile completeProcessableBamFileInProjectFolder

    final Collection<SamplePairProcessingStatus> samplePairProcessingStatuses

    WorkflowProcessingStatus getSnvProcessingStatus() {
        return TrackingService.combineStatuses(samplePairProcessingStatuses, { it.snvProcessingStatus })
    }

    WorkflowProcessingStatus getIndelProcessingStatus() {
        return TrackingService.combineStatuses(samplePairProcessingStatuses, { it.indelProcessingStatus })
    }

    WorkflowProcessingStatus getAceseqProcessingStatus() {
        return  ProcessingStatus.WorkflowProcessingStatus.NOTHING_DONE_WONT_DO
    }
}
