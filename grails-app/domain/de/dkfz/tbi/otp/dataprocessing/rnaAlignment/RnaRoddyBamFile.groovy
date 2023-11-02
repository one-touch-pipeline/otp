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

import grails.gorm.hibernate.annotation.ManagedEntity

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.bamfiles.RnaRoddyBamFileService
import de.dkfz.tbi.otp.utils.CollectionUtils

import java.nio.file.Paths

@ManagedEntity
class RnaRoddyBamFile extends RoddyBamFile {

    /**
     * @deprecated use {@link RnaRoddyBamFileService#CHIMERIC_BAM_SUFFIX} instead
     */
    @Deprecated
    static final String CHIMERIC_BAM_SUFFIX = "chimeric_merged.mdup.bam"
    /**
     * @deprecated use {@link RnaRoddyBamFileService#ARRIBA_FOLDER} instead
     */
    @Deprecated
    static final String ARRIBA_FOLDER = "fusions_arriba"
    /**
     * @deprecated use {@link RnaRoddyBamFileService#ARRIBA_PLOT_SUFFIX} instead
     */
    @Deprecated
    static final String ARRIBA_PLOT_SUFFIX = ".fusions.pdf"

    /**
     * @deprecated use {@link RnaRoddyBamFileService#getWorkMergedQADirectory} instead
     */
    @Deprecated
    @Override
    File getWorkMergedQADirectory() {
        return workQADirectory
    }

    /**
     * @deprecated use {@link RnaRoddyBamFileService#getFinalMergedQADirectory} instead
     */
    @Deprecated
    @Override
    File getFinalMergedQADirectory() {
        return finalQADirectory
    }

    /**
     * @deprecated use {@link RnaRoddyBamFileService#getCorrespondingWorkChimericBamFile} instead
     */
    @Deprecated
    File getCorrespondingWorkChimericBamFile() {
        return new File(workDirectory, "${sampleType.dirName}_${individual.pid}_${CHIMERIC_BAM_SUFFIX}")
    }

    /**
     * @deprecated use {@link RnaRoddyBamFileService#getWorkArribaFusionPlotPdf} instead
     */
    @Deprecated
    String getWorkArribaFusionPlotPdf() {
        String file = Paths.get(workDirectory as String, ARRIBA_FOLDER,
                "${sampleType.dirName}_${individual.pid}${ARRIBA_PLOT_SUFFIX}")
        return file
    }

    @Override
    AbstractQualityAssessment getQualityAssessment() {
        return CollectionUtils.exactlyOneElement(RnaQualityAssessment.createCriteria().list {
            eq 'chromosome', RoddyQualityAssessment.ALL
            qualityAssessmentMergedPass {
                abstractBamFile {
                    eq 'id', this.id
                }
            }
        })
    }
}
