/*
 * Copyright 2011-2021 The OTP authors
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
package de.dkfz.tbi.otp.dataswap.data

import de.dkfz.tbi.otp.dataswap.Swap
import de.dkfz.tbi.otp.dataswap.parameters.LaneSwapParameters
import de.dkfz.tbi.otp.ngsdata.Run
import de.dkfz.tbi.otp.ngsdata.Sample
import de.dkfz.tbi.otp.ngsdata.SampleType
import de.dkfz.tbi.otp.ngsdata.SeqType
import de.dkfz.tbi.otp.ngsdata.SequencingReadType

class LaneSwapData extends DataSwapData<LaneSwapParameters> {

    static Closure constraints = {
        sequencingReadTypeSwap nullable: false, validator: {
            if (!it.old) {
                return "The old SequencingReadType does not exists"
            }
            if (!it.new) {
                return "The new SequencingReadType does not exists"
            }
        }

        seqTrackList validator: { seqTrackList, obj ->
            if (seqTrackList*.seqType.unique().size() != 1) {
                return "SeqTrack of different SeqTypes found!"
            }
            if (seqTrackList*.seqType.first() != obj.seqTypeSwap.old) {
                return "expected '${obj.seqTypeSwap.old}' but found '${seqTrackList*.seqType.first()}'"
            }
            if (seqTrackList.size() != obj.lanes.size()) {
                return "Given lane(s) ${obj.lanes} and found SeqTracks differ!"
            }
        }
    }

    Run run

    Swap<SampleType> sampleTypeSwap
    Swap<SeqType> seqTypeSwap
    Swap<SequencingReadType> sequencingReadTypeSwap
    Swap<Sample> sampleSwap

    List<String> getLanes() {
        return parameters.lanes
    }

}
