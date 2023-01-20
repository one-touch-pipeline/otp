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

import groovy.transform.CompileDynamic
import groovy.transform.TupleConstructor

import de.dkfz.tbi.otp.ngsdata.SeqTrack

import static de.dkfz.tbi.otp.tracking.ProcessingStatus.WorkflowProcessingStatus

@CompileDynamic
@TupleConstructor
class SeqTrackProcessingStatus {

    final SeqTrack seqTrack

    final WorkflowProcessingStatus installationProcessingStatus
    final WorkflowProcessingStatus fastqcProcessingStatus

    final Collection<MergingWorkPackageProcessingStatus> mergingWorkPackageProcessingStatuses

    WorkflowProcessingStatus getAlignmentProcessingStatus() {
        return NotificationCreator.combineStatuses(mergingWorkPackageProcessingStatuses) {
            it.alignmentProcessingStatus
        }
    }

    WorkflowProcessingStatus getSnvProcessingStatus() {
        return NotificationCreator.combineStatuses(mergingWorkPackageProcessingStatuses) {
            it.snvProcessingStatus
        }
    }

    WorkflowProcessingStatus getIndelProcessingStatus() {
        return NotificationCreator.combineStatuses(mergingWorkPackageProcessingStatuses) {
            it.indelProcessingStatus
        }
    }

    WorkflowProcessingStatus getSophiaProcessingStatus() {
        return NotificationCreator.combineStatuses(mergingWorkPackageProcessingStatuses) {
            it.sophiaProcessingStatus
        }
    }

    WorkflowProcessingStatus getAceseqProcessingStatus() {
        return NotificationCreator.combineStatuses(mergingWorkPackageProcessingStatuses) {
            it.aceseqProcessingStatus
        }
    }

    WorkflowProcessingStatus getRunYapsaProcessingStatus() {
        return NotificationCreator.combineStatuses(mergingWorkPackageProcessingStatuses) {
            it.runYapsaProcessingStatus
        }
    }
}
