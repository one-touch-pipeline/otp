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
package de.dkfz.tbi.otp.dataswap.data

import groovy.transform.CompileDynamic
import groovy.transform.TupleConstructor

import de.dkfz.tbi.otp.dataprocessing.ExternallyProcessedBamFile
import de.dkfz.tbi.otp.dataswap.Swap
import de.dkfz.tbi.otp.dataswap.parameters.SampleSwapParameters
import de.dkfz.tbi.otp.ngsdata.*

@CompileDynamic
@TupleConstructor
class SampleSwapData extends DataSwapData<SampleSwapParameters> {

    static Closure constraints = {
        seqTrackList validator: { seqTrackList, obj ->
            List<ExternallyProcessedBamFile> externallyProcessedBamFiles = obj.seqTrackService.returnExternallyProcessedBamFiles(seqTrackList)
            if (!externallyProcessedBamFiles.empty) {
                return "There are ExternallyProcessedBamFiles attached: ${externallyProcessedBamFiles}"
            }
            int linkedSeqTracks = seqTrackList.findAll { SeqTrack seqTrack -> seqTrack.linkedExternally
            }.size()
            if (!obj.linkedFilesVerified && linkedSeqTracks) {
                return "There are ${linkedSeqTracks} seqTracks only linked"
            }
        }
    }

    Swap<SampleType> sampleTypeSwap

    Sample sample

    SeqTrackService seqTrackService
}
