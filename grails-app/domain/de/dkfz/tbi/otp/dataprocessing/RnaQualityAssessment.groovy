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
package de.dkfz.tbi.otp.dataprocessing

import grails.gorm.hibernate.annotation.ManagedEntity
import org.hibernate.Hibernate

import de.dkfz.tbi.otp.dataprocessing.rnaAlignment.RnaRoddyBamFile
import de.dkfz.tbi.otp.ngsdata.SequencingReadType
import de.dkfz.tbi.otp.qcTrafficLight.QcThresholdEvaluated
import de.dkfz.tbi.otp.qcTrafficLight.QcTrafficLightValue

@ManagedEntity
class RnaQualityAssessment extends RoddyQualityAssessment implements QcTrafficLightValue {

    @QcThresholdEvaluated
    double threePNorm

    @QcThresholdEvaluated
    double fivePNorm

    @QcThresholdEvaluated
    long alternativeAlignments

    @QcThresholdEvaluated
    double baseMismatchRate

    @QcThresholdEvaluated
    long chimericPairs

    @QcThresholdEvaluated
    long cumulGapLength

    @QcThresholdEvaluated
    Double end1PercentageSense

    @QcThresholdEvaluated
    long end1Antisense

    @QcThresholdEvaluated
    double end1MappingRate

    @QcThresholdEvaluated
    Double end1MismatchRate

    @QcThresholdEvaluated
    double end1Sense

    @QcThresholdEvaluated
    Double end2PercentageSense

    @QcThresholdEvaluated
    long end2Antisense

    @QcThresholdEvaluated
    double end2MappingRate

    @QcThresholdEvaluated
    Double end2MismatchRate

    @QcThresholdEvaluated
    double end2Sense

    @QcThresholdEvaluated
    long estimatedLibrarySize

    @QcThresholdEvaluated
    double exonicRate

    @QcThresholdEvaluated
    double expressionProfilingEfficiency

    @QcThresholdEvaluated
    long failedVendorQCCheck

    @QcThresholdEvaluated
    double insertSizeMean

    @QcThresholdEvaluated
    double gapPercentage

    @QcThresholdEvaluated
    long genesDetected

    @QcThresholdEvaluated
    double intergenicRate

    @QcThresholdEvaluated
    double intragenicRate

    @QcThresholdEvaluated
    double intronicRate

    @QcThresholdEvaluated
    long mapped

    @QcThresholdEvaluated
    long mappedPairs

    @QcThresholdEvaluated
    long  mappedUnique

    @QcThresholdEvaluated
    double mappedUniqueRateOfTotal

    @QcThresholdEvaluated
    double mappingRate

    @QcThresholdEvaluated
    double meanCV

    @QcThresholdEvaluated
    double meanPerBaseCov

    @QcThresholdEvaluated
    long noCovered5P

    @QcThresholdEvaluated
    long numGaps

    @QcThresholdEvaluated
    long readLength

    @QcThresholdEvaluated
    long splitReads

    @QcThresholdEvaluated
    long totalPurityFilteredReadsSequenced

    @QcThresholdEvaluated
    long transcriptsDetected

    @QcThresholdEvaluated
    double uniqueRateofMapped

    @QcThresholdEvaluated
    long unpairedReads

    @QcThresholdEvaluated
    long rRNAReads

    @QcThresholdEvaluated
    double rRNARate

    @QcThresholdEvaluated
    long mappedRead1

    @QcThresholdEvaluated
    long mappedRead2

    @QcThresholdEvaluated
    double duplicatesRate

    @QcThresholdEvaluated
    Double properlyPairedPercentage

    @QcThresholdEvaluated
    long secondaryAlignments

    @QcThresholdEvaluated
    long supplementaryAlignments

    @QcThresholdEvaluated
    double totalMappedReadCounterPercentage

    @QcThresholdEvaluated
    Double singletonsPercentage

    static constraints = {
        abstractBamFile(validator: {
            RnaRoddyBamFile.isAssignableFrom(Hibernate.getClass(it))
        })

        chromosome validator: { String chromosome, RnaQualityAssessment qa ->
            if (RnaQualityAssessment.countByIdNotEqualAndAbstractBamFile(qa.id, qa.abstractBamFile)) {
                return 'unique'
            }
            return chromosome == ALL
        }

        genomeWithoutNCoverageQcBases validator: { it == null }
        insertSizeCV validator: { it == null }
        insertSizeMedian validator: { it == null }
        pairedRead1 validator: { it == null }
        pairedRead2 validator: { it == null }
        percentageMatesOnDifferentChr validator: { it == null }
        referenceLength validator: { it == null }
        cumulGapLength nullable: true
        fivePNorm nullable: true
        gapPercentage nullable: true
        meanCV nullable: true
        meanPerBaseCov nullable: true
        noCovered5P nullable: true
        numGaps nullable: true
        threePNorm nullable: true
        end2PercentageSense nullable: true, validator: RnaQualityAssessment.nullIfAndOnlyIfLayoutIsSingle
        end2MismatchRate nullable: true, validator: RnaQualityAssessment.nullIfAndOnlyIfLayoutIsSingle
        end1PercentageSense nullable: true, validator: RnaQualityAssessment.nullIfAndOnlyIfLayoutIsSingle
        end1MismatchRate nullable: true, validator: RnaQualityAssessment.nullIfAndOnlyIfLayoutIsSingle
        properlyPaired nullable: true, validator: RnaQualityAssessment.nullIfAndOnlyIfLayoutIsSingle
        properlyPairedPercentage nullable: true, validator: RnaQualityAssessment.nullIfAndOnlyIfLayoutIsSingle
        singletons nullable: true, validator: RnaQualityAssessment.nullIfAndOnlyIfLayoutIsSingle
        singletonsPercentage nullable: true, validator: RnaQualityAssessment.nullIfAndOnlyIfLayoutIsSingle
    }

    static def nullIfAndOnlyIfLayoutIsSingle = { val, RnaQualityAssessment obj ->
        if (obj.roddyBamFile.seqType.libraryLayout == SequencingReadType.PAIRED && val == null) {
            return "required"
        } else if (obj.roddyBamFile.seqType.libraryLayout == SequencingReadType.SINGLE && val != null) {
            return "not.allowed"
        }
    }
}
