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

import grails.gorm.transactions.Transactional
import org.springframework.util.Assert

import de.dkfz.tbi.otp.dataprocessing.singleCell.SingleCellBamFile

@Transactional
class ChromosomeQualityAssessmentMergedService {

    @SuppressWarnings('Instanceof')
    List<AbstractQualityAssessment> qualityAssessmentMergedForSpecificChromosomes(
            List<String> chromosomes, List<QualityAssessmentMergedPass> qualityAssessmentMergedPasses) {
        Assert.notNull(chromosomes, 'Parameter "chromosomes" may not be null')
        Assert.notNull(qualityAssessmentMergedPasses, 'Parameter "qualityAssessmentMergedPasses" may not be null')

        qualityAssessmentMergedPasses*.abstractMergedBamFile.each { AbstractMergedBamFile abstractMergedBamFile ->
            assert (abstractMergedBamFile instanceof RoddyBamFile || abstractMergedBamFile instanceof ProcessedMergedBamFile ||
                    abstractMergedBamFile instanceof SingleCellBamFile)
        }

        //For SingleCellBamFile no chromosome specific values available, so it can be ignored

        List<QualityAssessmentMergedPass> roddyFilePasses = qualityAssessmentMergedPasses.findAll {
            it.abstractMergedBamFile instanceof RoddyBamFile
        }
        List<QualityAssessmentMergedPass> bamFilePasses = qualityAssessmentMergedPasses.findAll {
            it.abstractMergedBamFile instanceof ProcessedMergedBamFile
        }

        if (chromosomes) {
            List<AbstractQualityAssessment> roddyQAPerChromosome = []
            List<AbstractQualityAssessment> bamFileQAPerChromosome = []

            if (bamFilePasses) {
                bamFileQAPerChromosome = ChromosomeQualityAssessmentMerged.createCriteria().list {
                    'in'("chromosomeName", chromosomes)
                    'in'('qualityAssessmentMergedPass', bamFilePasses)
                }
            }

            if (roddyFilePasses) {
                roddyQAPerChromosome = RoddyMergedBamQa.createCriteria().list {
                    'in'("chromosome", chromosomes)
                    'in'('qualityAssessmentMergedPass', roddyFilePasses)
                }
            }
            return bamFileQAPerChromosome + roddyQAPerChromosome
        }
        return []
    }
}
