/*
 * Copyright 2011-2023 The OTP authors
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

/**
 * An immutable set of SeqTracks, containing aggregated values and lists.
 */
class SeqTrackSet {

    final Set<SeqTrack> seqTracks

    final Set<RawSequenceFile> rawSequenceFiles
    final Long totalFileSize
    final Boolean containsWithdrawnData
    final Boolean containsSwappedLane

    final List<SeqPlatform> seqPlatforms
    final List<SeqCenter> seqCenters
    final Integer numberOfLanes
    final Long numberOfBases

    SeqTrackSet(List<SeqTrack> seqTracks) {
        this.seqTracks = seqTracks as Set<SeqTrack>

        this.rawSequenceFiles = seqTracks.collectMany { it.sequenceFiles } as Set<RawSequenceFile>
        this.totalFileSize = this.rawSequenceFiles.sum { it.fileSize } as Long
        this.containsWithdrawnData = this.rawSequenceFiles.any { it.fileWithdrawn }
        this.containsSwappedLane = this.seqTracks.any { it.swapped }

        this.seqPlatforms = this.seqTracks*.seqPlatform.unique()
        this.seqCenters = this.seqTracks*.seqCenter.unique()
        this.numberOfLanes = this.seqTracks.size()
        this.numberOfBases = totalNumberOfBasesOrNull
    }

    @SuppressWarnings('ReturnNullFromCatchBlock') //if the number can not be calculated, null should be return
    private Long getTotalNumberOfBasesOrNull() {
        try {
            return rawSequenceFiles.findAll { !it.indexFile }.sum { RawSequenceFile rawSequenceFile ->
                rawSequenceFile.NBasePairs
            } as Long
        } catch (AssertionError e) {
            return null
        }
    }
}
