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

import de.dkfz.tbi.otp.qcTrafficLight.QcThresholdEvaluated
import de.dkfz.tbi.otp.utils.Entity

/**
 * Be aware of semantic differences between the values produced by
 * qa.jar and by Roddy QC. See the comments of OTP-1731 for details.
 */
@ManagedEntity
abstract class AbstractQualityAssessment implements Entity {

    /**
     * length of the chromosome/genome of the reference
     */
    @QcThresholdEvaluated
    Long referenceLength

    /**
     * length of alignment >= minAlignedRecordLength
     * meanBaseQuality >= minMeanBaseQuality
     * sum of all mapped bases including gaps which pass both criteria above
     */
    @QcThresholdEvaluated
    Long qcBasesMapped

    /**
     * all bases mapped to the reference genome no filtering applied
     */
    @QcThresholdEvaluated
    Long allBasesMapped
    /**
     * bases, which were mapped to the specified target regions
     */
    @QcThresholdEvaluated
    Long onTargetMappedBases

    /**
     * all reads (flagstat QC-passed reads)
     */
    @QcThresholdEvaluated
    Long totalReadCounter
    /**
     * FailsVendorQualityCheckFlag = true (flagstat QC-failed reads)
     * should be 0
     */
    @QcThresholdEvaluated
    Long qcFailedReads
    /**
     * duplicateFlag = true, (flagstat value duplicates)
     */
    @QcThresholdEvaluated
    Long duplicates
    /**
     * every mapped read (flagstat mapped)
     */
    @QcThresholdEvaluated
    Long totalMappedReadCounter
    /**
     * every paired read (flagstat paired in sequencing)
     */
    @QcThresholdEvaluated
    Long pairedInSequencing
    /**
     * count of read 2 (flagstat read2)
     */
    @QcThresholdEvaluated
    Long pairedRead2
    /**
     * count of read 1 (flagstat read1)
     */
    @QcThresholdEvaluated
    Long pairedRead1
    /**
     * read is mapped in a proper pair (flagstat properly paired)
     */
    @QcThresholdEvaluated
    Long properlyPaired
    /**
     * read and mate are mapped (flagstat with itself and mate mapped)
     */
    @QcThresholdEvaluated
    Long withItselfAndMateMapped
    /**
     * read and mate map to different chromosome (flagstat with mate mapped to a different chr)
     */
    @QcThresholdEvaluated
    Long withMateMappedToDifferentChr
    /**
     * read and mate map to different chromosome and MappingQuality >= 5 (flagstat with mate mapped to a different chr (mapQ>=5))
     */
    @QcThresholdEvaluated
    Long withMateMappedToDifferentChrMaq
    /**
     * mate or read unmapped but corresponding read mapped (flagstat singletons)
     */
    @QcThresholdEvaluated
    Long singletons

    /**
     * median over all insert sizes of proper paired mapped reads
     */
    @QcThresholdEvaluated
    Double insertSizeMedian

    /**
     * standard deviation over all insert sizes of proper paired mapped reads
     */
    @QcThresholdEvaluated
    Double insertSizeSD

    @QcThresholdEvaluated
    Double getPercentDuplicates() {
        return calculatePercentage(duplicates , totalReadCounter)
    }

    @QcThresholdEvaluated
    Double getPercentProperlyPaired() {
        return calculatePercentage(properlyPaired, pairedInSequencing)
    }

    @QcThresholdEvaluated
    Double getPercentDiffChr() {
        return calculatePercentage(withMateMappedToDifferentChr, totalReadCounter)
    }

    @QcThresholdEvaluated
    Double getPercentMappedReads() {
        return calculatePercentage(totalMappedReadCounter, totalReadCounter)
    }

    @QcThresholdEvaluated
    Double getPercentSingletons() {
        return calculatePercentage(singletons, totalReadCounter)
    }

    @QcThresholdEvaluated
    Double getOnTargetRatio() {
        return calculatePercentage(onTargetMappedBases, allBasesMapped)
    }

    private static Double calculatePercentage(Number numerator, Number denominator) {
        return numerator != null && denominator ?
                numerator / denominator * 100.0 : null
    }

    static constraints = {
        // not available for RNA
        referenceLength nullable: true, validator: { it != null }
        // This value is not available for exome QC of RoddyBamFiles
        qcBasesMapped(nullable: true)

        // This value is only available for exome
        allBasesMapped(nullable: true)

        // This value is only available for exome
        // It is not available for exome QC of single lanes in RoddyBamFiles
        onTargetMappedBases(nullable: true)

        // These values are not available in the per chromosome QC of RoddyBamFiles
        totalReadCounter(nullable: true)
        qcFailedReads(nullable: true)
        duplicates(nullable: true)
        totalMappedReadCounter(nullable: true)
        pairedInSequencing(nullable: true)
        pairedRead2(nullable: true)
        pairedRead1(nullable: true)
        properlyPaired(nullable: true)
        withItselfAndMateMapped(nullable: true)
        withMateMappedToDifferentChr(nullable: true)
        withMateMappedToDifferentChrMaq(nullable: true)
        singletons(nullable: true)
        insertSizeMedian(nullable: true)
        insertSizeSD(nullable: true)
    }

    static mapping = {
        'class' index: "abstract_quality_assessment_class_idx"
    }
}
