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
package migration

import de.dkfz.tbi.otp.dataprocessing.RoddyBamFile
import de.dkfz.tbi.otp.ngsdata.SeqTrack

/**
 * Migrates the seqTracks of all BaseBamFiles of the RoddyBamFiles to the list of seqTracks of the RoddyBamFile it self.
 */

List<RoddyBamFile> allRoddyBamFiles = RoddyBamFile.findAllByBaseBamFileIsNotNull()


RoddyBamFile.withTransaction {
    allRoddyBamFiles.each { roddyBamFile ->
        print("RoddyBamFile with id ${roddyBamFile.id} contained the seqTracks ${roddyBamFile.seqTracks.collect { seqTrack -> seqTrack.id }} ")
        Set<SeqTrack> containedSeqTracks = getContainedSeqTracks(roddyBamFile)
        roddyBamFile.seqTracks = containedSeqTracks
        roddyBamFile.numberOfMergedLanes = containedSeqTracks.size()
        roddyBamFile.baseBamFile = null
        println("and ${containedSeqTracks.collect { seqTrack -> seqTrack.id }} as seqTracks from the baseBamFiles.")
        roddyBamFile.save(flush: true)
    }
}

Set<SeqTrack> getContainedSeqTracks(RoddyBamFile roddyBamFile) {
    def tmpSet = roddyBamFile.baseBamFile ? getContainedSeqTracks(roddyBamFile.baseBamFile) : []
    tmpSet.addAll(roddyBamFile.seqTracks)
    return tmpSet as Set
}
