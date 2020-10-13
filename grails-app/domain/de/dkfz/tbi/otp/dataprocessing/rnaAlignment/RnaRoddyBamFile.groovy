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
package de.dkfz.tbi.otp.dataprocessing.rnaAlignment

import de.dkfz.tbi.otp.dataprocessing.RoddyBamFile
import java.nio.file.Paths

class RnaRoddyBamFile extends RoddyBamFile {

    static final String CHIMERIC_BAM_SUFFIX = "chimeric_merged.mdup.bam"
    static final String ARRIBA_FOLDER = "fusions_arriba"
    static final String ARRIBA_PLOT_SUFFIX = ".fusions.pdf"

    @Override
    File getWorkMergedQADirectory() {
        return workQADirectory
    }

    @Override
    File getFinalMergedQADirectory() {
        return finalQADirectory
    }

    File getCorrespondingWorkChimericBamFile() {
        return new File(workDirectory, "${sampleType.dirName}_${individual.pid}_${CHIMERIC_BAM_SUFFIX}")
    }

    String getWorkArribaFusionPlotPdf() {
        String file = Paths.get(workDirectory as String, ARRIBA_FOLDER,
                "${sampleType.dirName}_${individual.pid}${ARRIBA_PLOT_SUFFIX}")
        return file
    }
}
