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

import org.hibernate.Hibernate

import de.dkfz.tbi.otp.ngsdata.ReferenceGenomeEntry

/**
 * Class to represent chromosomes that will be considered independently (1 to 22, X, Y and M)
 * for single lane
 */
class ChromosomeQualityAssessment extends QaJarQualityAssessment {

    /**
     * Name of the {@link ReferenceGenomeEntry} used in the BAM file where the data to identify the chromosome is read from
     * (depends on the reference used to align the reads)
     */
    String chromosomeName

    QualityAssessmentPass qualityAssessmentPass

    static belongsTo = [
        qualityAssessmentPass: QualityAssessmentPass,
    ]

    static constraints = {
        qualityAssessmentPass(validator: {
            ProcessedBamFile.isAssignableFrom(Hibernate.getClass(it.processedBamFile))
        })
    }
}
