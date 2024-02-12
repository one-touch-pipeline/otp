/*
 * Copyright 2011-2024 The OTP authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package de.dkfz.tbi.otp.tracking

import groovy.transform.TupleConstructor

import de.dkfz.tbi.otp.dataprocessing.aceseq.AceseqInstance
import de.dkfz.tbi.otp.dataprocessing.indelcalling.IndelCallingInstance
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
        return NotificationCreator.combineStatuses([
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
