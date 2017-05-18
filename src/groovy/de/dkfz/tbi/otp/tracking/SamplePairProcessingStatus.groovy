package de.dkfz.tbi.otp.tracking

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import de.dkfz.tbi.otp.dataprocessing.sophia.*
import groovy.transform.*

import static de.dkfz.tbi.otp.tracking.ProcessingStatus.*

@TupleConstructor
class SamplePairProcessingStatus {

    final SamplePair samplePair

    final WorkflowProcessingStatus snvProcessingStatus
    final SnvCallingInstance completeSnvCallingInstance

    final WorkflowProcessingStatus indelProcessingStatus
    final IndelCallingInstance completeIndelCallingInstance

    final WorkflowProcessingStatus sophiaProcessingStatus
    final SophiaInstance completeSophiaInstance

    final WorkflowProcessingStatus aceseqProcessingStatus
    final AceseqInstance completeAceseqInstance

    WorkflowProcessingStatus getVariantCallingProcessingStatus() {
        return TrackingService.combineStatuses([snvProcessingStatus, indelProcessingStatus, sophiaProcessingStatus, aceseqProcessingStatus], Closure.IDENTITY)
    }

    List<String> variantCallingWorkflowNames() {
        return [
                SNV: snvProcessingStatus,
                Indel: indelProcessingStatus,
                'SV (from SOPHIA)': sophiaProcessingStatus,
                'CNV (from ACEseq)': aceseqProcessingStatus,
        ].findAll { it ->
            it.value != WorkflowProcessingStatus.NOTHING_DONE_WONT_DO
        }.keySet().toList()
    }
}
