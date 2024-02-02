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
package de.dkfz.tbi.otp.withdraw

import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.ngsdata.SeqTrackWithComment

class WithdrawParameters {

    /**
     * The seqTracks as keys with the corresponding comments as values
     */
    List<SeqTrackWithComment> seqTracksWithComments = []

    /**
     * Name of the generated bash file. It should have the extension '.sh'
     * The file is created in the default directory
     */
    String fileName = ''

    /**
     * indicate, if the bam files should be deleted (true) or set to withdrawn (false).
     */
    boolean deleteBamFile = false

    /**
     * indicate, if the analysis files should be deleted (true) or set to withdrawn (false).
     * The selection is only possible, if the bam files are not deleted.
     */
    boolean deleteAnalysis = false

    /**
     * Should withdrawing stop if files are not existing in file system (using cached value 'sequenceFile.fileExists')
     */
    boolean stopOnMissingFiles = true

    /**
     * Should withdrawing stop if files are already withdrawn
     */
    boolean stopOnAlreadyWithdrawnData = true

    /**
     * The seqTracks to withdraw
     */
    List<SeqTrack> getSeqTracks() {
        return this.seqTracksWithComments*.seqTrack
    }

}
