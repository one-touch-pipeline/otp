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
package de.dkfz.tbi.otp.dataprocessing

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext

import de.dkfz.tbi.otp.ngsdata.Individual
import de.dkfz.tbi.otp.ngsdata.SeqTrack

/**
 * This service implements alignment files organization convention
 */
class ProcessedAlignmentFileService {

    /**
     * deleteOldAlignmentProcessingFiles and deleteProcessingFiles should not be executed transactionally because if the
     * deletion of one processing file fails, the successfully deleted files should still be marked as deleted in the
     * database.
     */
    static transactional = false

    @Autowired
    ApplicationContext applicationContext
    DataProcessingFilesService dataProcessingFilesService

    String getDirectory(AlignmentPass alignmentPass) {
        Individual ind = alignmentPass.seqTrack.sample.individual
        def dirType = DataProcessingFilesService.OutputDirectories.ALIGNMENT
        String baseDir = dataProcessingFilesService.getOutputDirectory(ind, dirType)
        String middleDir = getRunLaneDirectory(alignmentPass.seqTrack)
        return "${baseDir}/${middleDir}/${alignmentPass.directory}"
    }

    String getRunLaneDirectory(SeqTrack seqTrack) {
        String runName = seqTrack.run.name
        String lane = seqTrack.laneId
        return "${runName}_${lane}"
    }
}
