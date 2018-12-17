package de.dkfz.tbi.otp.tracking

import groovy.transform.TupleConstructor

import de.dkfz.tbi.otp.dataprocessing.AbstractMergedBamFile
import de.dkfz.tbi.otp.dataprocessing.MergingWorkPackage

import static de.dkfz.tbi.otp.tracking.ProcessingStatus.WorkflowProcessingStatus

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

    WorkflowProcessingStatus getSophiaProcessingStatus() {
        return TrackingService.combineStatuses(samplePairProcessingStatuses, { it.sophiaProcessingStatus })
    }

    WorkflowProcessingStatus getAceseqProcessingStatus() {
        return TrackingService.combineStatuses(samplePairProcessingStatuses, { it.aceseqProcessingStatus })
    }

    WorkflowProcessingStatus getRunYapsaProcessingStatus() {
        return TrackingService.combineStatuses(samplePairProcessingStatuses, { it.runYapsaProcessingStatus })
    }
}
