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
package de.dkfz.tbi.otp.job.jobs.rnaAlignment

import groovy.util.logging.Slf4j
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.rnaAlignment.RnaRoddyBamFile
import de.dkfz.tbi.otp.job.jobs.roddyAlignment.AbstractRoddyAlignmentStartJob
import de.dkfz.tbi.otp.ngsdata.*

@Component('rnaAlignmentStartJob')
@Scope('singleton')
@Slf4j
class RnaAlignmentStartJob extends AbstractRoddyAlignmentStartJob {
    @Override
    List<SeqType> getSeqTypes() {
        return SeqTypeService.rnaAlignableSeqTypes
    }

    /**
     * For RNA alignment the incremental merging is not implemented yet, therefore null is returned.
     */
    @Override
    RoddyBamFile findUsableBaseBamFile(MergingWorkPackage mergingWorkPackage) {
        return null
    }

    @Override
    AbstractMergedBamFile reallyCreateBamFile(
            MergingWorkPackage mergingWorkPackage,
            int identifier,
            Set<SeqTrack> seqTracks,
            ConfigPerProjectAndSeqType config,
            AbstractMergedBamFile baseBamFile = null
    ) {
        new RnaRoddyBamFile (
                workPackage: mergingWorkPackage,
                identifier: identifier,
                workDirectoryName: "${RoddyBamFile.WORK_DIR_PREFIX}_${identifier}",
                seqTracks: seqTracks,
                config: config,
        )
    }
}
