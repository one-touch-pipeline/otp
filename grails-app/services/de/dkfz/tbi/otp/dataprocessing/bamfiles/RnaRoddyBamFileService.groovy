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
package de.dkfz.tbi.otp.dataprocessing.bamfiles

import grails.gorm.transactions.Transactional

import de.dkfz.tbi.otp.dataprocessing.RoddyBamFile
import de.dkfz.tbi.otp.dataprocessing.rnaAlignment.RnaRoddyBamFile
import de.dkfz.tbi.otp.ngsdata.SeqTrack

import java.nio.file.Path

@Transactional
class RnaRoddyBamFileService extends RoddyBamFileService {

    static final String CHIMERIC_BAM_SUFFIX = "chimeric_merged.mdup.bam"
    static final String ARRIBA_FOLDER = "fusions_arriba"
    static final String ARRIBA_PLOT_SUFFIX = ".fusions.pdf"

    @Override
    Path getFinalMergedQADirectory(RoddyBamFile bamFile) {
        return getFinalQADirectory(bamFile)
    }

    @Override
    Path getWorkMergedQADirectory(RoddyBamFile bamFile) {
        return getWorkQADirectory(bamFile)
    }

    @Override
    Map<SeqTrack, Path> getWorkSingleLaneQADirectories(RoddyBamFile bamFile) {
        return [:]
    }

    @Override
    Map<SeqTrack, Path> getFinalSingleLaneQADirectories(RoddyBamFile bamFile) {
        return [:]
    }

    Path getCorrespondingWorkChimericBamFile(RnaRoddyBamFile bamFile) {
        return getWorkDirectory(bamFile).resolve("${bamFile.sampleType.dirName}_${bamFile.individual.pid}_${CHIMERIC_BAM_SUFFIX}")
    }

    Path getWorkArribaFusionPlotPdf(RnaRoddyBamFile bamFile) {
        return getWorkDirectory(bamFile).resolve(ARRIBA_FOLDER).resolve("${bamFile.sampleType.dirName}_${bamFile.individual.pid}${ARRIBA_PLOT_SUFFIX}")
    }
}
