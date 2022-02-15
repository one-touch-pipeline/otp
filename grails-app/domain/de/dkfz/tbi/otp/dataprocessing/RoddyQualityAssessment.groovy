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

import grails.gorm.hibernate.annotation.ManagedEntity
import org.hibernate.Hibernate

import de.dkfz.tbi.otp.qcTrafficLight.QcThresholdEvaluated

@ManagedEntity
abstract class RoddyQualityAssessment extends AbstractQualityAssessment implements QualityAssessmentWithMergedPass {

    // We could also link directly to RoddyBamFile, but then Grails fails to start up the application context for an
    // unclear reason. So as a workaround we have an indirect link via QualityAssessmentMergedPass.

    static final String ALL = "all"

    /**
     * This property holds the name of the chromosome, these QC values belong to.
     * In case the QC values are for the complete genome the name will be {@link #ALL}.
     */
    String chromosome

    @QcThresholdEvaluated
    Double insertSizeCV

    @QcThresholdEvaluated
    Double percentageMatesOnDifferentChr

    @QcThresholdEvaluated
    Double genomeWithoutNCoverageQcBases

    static def nullIfAndOnlyIfPerChromosomeQc = { val, RoddyQualityAssessment obj ->
        if (obj.chromosome == ALL && val == null) {
            return "required"
        } else if (obj.chromosome != ALL && val != null) {
            return "not.allowed"
        }
    }

    static belongsTo = [
            qualityAssessmentMergedPass: QualityAssessmentMergedPass,
    ]

    static constraints = {
        qualityAssessmentMergedPass(validator: {
            RoddyBamFile.isAssignableFrom(Hibernate.getClass(it.abstractMergedBamFile))
        })

        chromosome blank: false

        insertSizeCV(nullable: true, validator: RoddyQualityAssessment.nullIfAndOnlyIfPerChromosomeQc)
        percentageMatesOnDifferentChr(nullable: true)

        totalReadCounter(validator: RoddyQualityAssessment.nullIfAndOnlyIfPerChromosomeQc)
        qcFailedReads(validator: RoddyQualityAssessment.nullIfAndOnlyIfPerChromosomeQc)
        duplicates(validator: RoddyQualityAssessment.nullIfAndOnlyIfPerChromosomeQc)
        totalMappedReadCounter(validator: RoddyQualityAssessment.nullIfAndOnlyIfPerChromosomeQc)
        pairedInSequencing(validator: RoddyQualityAssessment.nullIfAndOnlyIfPerChromosomeQc)
        pairedRead2(validator: RoddyQualityAssessment.nullIfAndOnlyIfPerChromosomeQc)
        pairedRead1(validator: RoddyQualityAssessment.nullIfAndOnlyIfPerChromosomeQc)
        properlyPaired(validator: RoddyQualityAssessment.nullIfAndOnlyIfPerChromosomeQc)
        withItselfAndMateMapped(validator: RoddyQualityAssessment.nullIfAndOnlyIfPerChromosomeQc)
        withMateMappedToDifferentChr(validator: RoddyQualityAssessment.nullIfAndOnlyIfPerChromosomeQc)
        withMateMappedToDifferentChrMaq(validator: RoddyQualityAssessment.nullIfAndOnlyIfPerChromosomeQc)
        singletons(validator: RoddyQualityAssessment.nullIfAndOnlyIfPerChromosomeQc)
        insertSizeMedian(validator: RoddyQualityAssessment.nullIfAndOnlyIfPerChromosomeQc)
        insertSizeSD(validator: RoddyQualityAssessment.nullIfAndOnlyIfPerChromosomeQc)
        // not available for RNA
        genomeWithoutNCoverageQcBases nullable: true, validator: { it != null }
    }

    static mapping = {
        // An index on qualityAssessmentMergedPass is defined in OverallQualityAssessmentMerged
    }

    RoddyBamFile getRoddyBamFile() {
        return (RoddyBamFile)qualityAssessmentMergedPass.abstractMergedBamFile
    }

    // Created to have an identical way to receive the chromosome identifier as in ChromosomeQualityAssessmentMerged
    String getChromosomeName() {
        return chromosome
    }

    RoddyBamFile getBamFile() {
        return roddyBamFile
    }
}
