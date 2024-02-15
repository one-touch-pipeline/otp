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
package de.dkfz.tbi.otp.workflowTest.roddy

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.dataprocessing.RoddyBamFile
import de.dkfz.tbi.otp.infrastructure.alignment.RnaAlignmentLinkFileService
import de.dkfz.tbi.otp.infrastructure.alignment.RnaAlignmentWorkFileService
import de.dkfz.tbi.otp.ngsdata.SeqTrack

import java.nio.file.Path

@Component
class RnaRoddyFileAssertHelper extends RoddyFileAssertHelper {

    @Autowired
    RnaAlignmentWorkFileService rnaAlignmentWorkFileService

    @Autowired
    RnaAlignmentLinkFileService rnaAlignmentLinkFileService

    @Override
    Path getWorkDirectory(RoddyBamFile bamFile) {
        return rnaAlignmentWorkFileService.getDirectoryPath(bamFile)
    }

    @Override
    Path getLinkMergedQADirectory(RoddyBamFile bamFile) {
        return rnaAlignmentLinkFileService.getMergedQADirectory(bamFile)
    }

    @Override
    Path getWorkMergedQADirectory(RoddyBamFile bamFile) {
        return rnaAlignmentWorkFileService.getMergedQADirectory(bamFile)
    }

    @Override
    Path getWorkQADirectory(RoddyBamFile bamFile) {
        return rnaAlignmentWorkFileService.getQADirectory(bamFile)
    }

    @Override
    Map<SeqTrack, Path> getLinkSingleLaneQADirectories(RoddyBamFile bamFile) {
        return rnaAlignmentLinkFileService.getSingleLaneQADirectories(bamFile)
    }

    @Override
    Map<SeqTrack, Path> getWorkSingleLaneQADirectories(RoddyBamFile bamFile) {
        return rnaAlignmentWorkFileService.getSingleLaneQADirectories(bamFile)
    }

    @Override
    Map<SeqTrack, Path> getWorkSingleLaneQAJsonFiles(RoddyBamFile bamFile) {
        return rnaAlignmentWorkFileService.getSingleLaneQAJsonFiles(bamFile)
    }

    @Override
    List<Path> getAdditionalWorkFiles(RoddyBamFile bamFile) {
        return [
                rnaAlignmentWorkFileService.getMergedQAJsonFile(bamFile),
                rnaAlignmentWorkFileService.getCorrespondingChimericBamFile(bamFile),
        ]
    }
}
