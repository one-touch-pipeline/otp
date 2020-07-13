/*
 * Copyright 2011-2019 The OTP authors
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

import static de.dkfz.tbi.otp.tracking.ProcessingStatus.Done.*

@TupleConstructor
class ProcessingStatus {

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
        return NotificationCreator.combineStatuses(seqTrackProcessingStatuses) {
            it.installationProcessingStatus
        }
    }

    WorkflowProcessingStatus getFastqcProcessingStatus() {
        return NotificationCreator.combineStatuses(seqTrackProcessingStatuses) {
            it.fastqcProcessingStatus
        }
    }

    WorkflowProcessingStatus getAlignmentProcessingStatus() {
        return NotificationCreator.combineStatuses(seqTrackProcessingStatuses) {
            it.alignmentProcessingStatus
        }
    }

    WorkflowProcessingStatus getSnvProcessingStatus() {
        return NotificationCreator.combineStatuses(seqTrackProcessingStatuses) {
            it.snvProcessingStatus
        }
    }

    WorkflowProcessingStatus getIndelProcessingStatus() {
        return NotificationCreator.combineStatuses(seqTrackProcessingStatuses) {
            it.indelProcessingStatus
        }
    }

    WorkflowProcessingStatus getSophiaProcessingStatus() {
        return NotificationCreator.combineStatuses(seqTrackProcessingStatuses) {
            it.sophiaProcessingStatus
        }
    }

    WorkflowProcessingStatus getAceseqProcessingStatus() {
        return NotificationCreator.combineStatuses(seqTrackProcessingStatuses) {
            it.aceseqProcessingStatus
        }
    }

    WorkflowProcessingStatus getRunYapsaProcessingStatus() {
        return NotificationCreator.combineStatuses(seqTrackProcessingStatuses) {
            it.runYapsaProcessingStatus
        }
    }


    Map<OtrsTicket.ProcessingStep, WorkflowProcessingStatus> getWorkflowProcessingStatusPerProcessingStep() {
        return [
                (OtrsTicket.ProcessingStep.INSTALLATION): installationProcessingStatus,
                (OtrsTicket.ProcessingStep.FASTQC)      : fastqcProcessingStatus,
                (OtrsTicket.ProcessingStep.ALIGNMENT)   : alignmentProcessingStatus,
                (OtrsTicket.ProcessingStep.SNV)         : snvProcessingStatus,
                (OtrsTicket.ProcessingStep.INDEL)       : indelProcessingStatus,
                (OtrsTicket.ProcessingStep.SOPHIA)      : sophiaProcessingStatus,
                (OtrsTicket.ProcessingStep.ACESEQ)      : aceseqProcessingStatus,
                (OtrsTicket.ProcessingStep.RUN_YAPSA)   : runYapsaProcessingStatus,
        ]
    }

    @Override
    String toString() {
        return workflowProcessingStatusPerProcessingStep.collect { OtrsTicket.ProcessingStep step, WorkflowProcessingStatus status ->
            String.format("%-13s %s", "${step.name()}:", status)
        }.join("\n")
    }
}
