package de.dkfz.tbi.otp.tracking

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.*
import groovy.transform.*

import static de.dkfz.tbi.otp.tracking.ProcessingStatus.*

@TupleConstructor
class SamplePairProcessingStatus {

    final SamplePair samplePair

    final WorkflowProcessingStatus snvProcessingStatus
    final SnvCallingInstance completeSnvCallingInstance

    final WorkflowProcessingStatus indelProcessingStatus
    final IndelCallingInstance completeIndelCallingInstance
}
