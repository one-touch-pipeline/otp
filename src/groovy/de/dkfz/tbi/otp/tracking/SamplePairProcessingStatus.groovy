package de.dkfz.tbi.otp.tracking

import groovy.transform.TupleConstructor

import de.dkfz.tbi.otp.dataprocessing.AceseqInstance
import de.dkfz.tbi.otp.dataprocessing.IndelCallingInstance
import de.dkfz.tbi.otp.dataprocessing.runYapsa.RunYapsaInstance
import de.dkfz.tbi.otp.dataprocessing.snvcalling.AbstractSnvCallingInstance
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair
import de.dkfz.tbi.otp.dataprocessing.sophia.SophiaInstance

import static de.dkfz.tbi.otp.tracking.ProcessingStatus.WorkflowProcessingStatus

@TupleConstructor
class SamplePairProcessingStatus {

    final SamplePair samplePair

    final WorkflowProcessingStatus snvProcessingStatus
    final AbstractSnvCallingInstance completeSnvCallingInstance

    final WorkflowProcessingStatus indelProcessingStatus
    final IndelCallingInstance completeIndelCallingInstance

    final WorkflowProcessingStatus sophiaProcessingStatus
    final SophiaInstance completeSophiaInstance

    final WorkflowProcessingStatus aceseqProcessingStatus
    final AceseqInstance completeAceseqInstance

    final WorkflowProcessingStatus runYapsaProcessingStatus
    final RunYapsaInstance completeRunYapsaInstance

    WorkflowProcessingStatus getVariantCallingProcessingStatus() {
        return TrackingService.combineStatuses([
                snvProcessingStatus,
                indelProcessingStatus,
                sophiaProcessingStatus,
                aceseqProcessingStatus,
                runYapsaProcessingStatus,
        ], Closure.IDENTITY)
    }

    List<String> variantCallingWorkflowNames() {
        return [
                SNV: snvProcessingStatus,
                Indel: indelProcessingStatus,
                'SV (from SOPHIA)': sophiaProcessingStatus,
                'CNV (from ACEseq)': aceseqProcessingStatus,
                RunYapsa: runYapsaProcessingStatus,
        ].findAll { it ->
            it.value != WorkflowProcessingStatus.NOTHING_DONE_WONT_DO
        }.keySet().toList()
    }
}
