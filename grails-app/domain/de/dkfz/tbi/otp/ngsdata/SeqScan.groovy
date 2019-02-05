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

package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.utils.Entity

@Deprecated
class SeqScan implements Entity {

    int nLanes = 0
    Long nBasePairs       // calculated from seqTracks
    double coverage = 0.0     // from somewhere

    @Deprecated
    enum State {
        DECLARED, PROCESSING, FINISHED, OBSOLETE
    }
    State state = State.DECLARED

    String seqCenters = ""
    String insertSize = ""
    Sample sample
    SeqPlatform seqPlatform
    SeqType seqType

    // quality control
    @Deprecated
    enum QCState {
        NON, PASS, BLOCK
    }
    QCState qcState = QCState.NON

    Date dateCreated = new Date()

    static belongsTo = [
            sample     : Sample,
            seqType    : SeqType,
            seqPlatform: SeqPlatform,
    ]

    static constraints = {
        insertSize(nullable: true)
        nBasePairs(nullable: true)
    }

    @Deprecated
    @Override
    String toString() {
        "${sample} ${seqType}"
    }

    @Deprecated
    String basePairsString() {
        return nBasePairs ? String.format("%.1f G", (nBasePairs / 1e9)) : "N/A"
    }

    @Deprecated
    boolean isMerged() {
        return (MergingLog.countBySeqScan(this) != 0)
    }

    static mapping = {
        sample index: "seq_scan_sample_idx"
        seqType index: "seq_scan_seq_type_idx"
        seqPlatform index: "seq_scan_seq_platform_idx"
    }
}
