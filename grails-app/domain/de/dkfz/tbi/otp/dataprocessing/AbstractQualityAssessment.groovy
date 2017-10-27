package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.*

/**
 * Be aware of semantic differences between the values produced by
 * qa.jar and by Roddy QC. See the comments of OTP-1731 for details.
 */
class AbstractQualityAssessment implements Entity, QcTrafficLightValue {

    /**
     * length of the chromosome/genome of the reference
     */
    Long referenceLength

    /**
     * length of alignment >= minAlignedRecordLength
     * meanBaseQuality >= minMeanBaseQuality
     * sum of all mapped bases including gaps which pass both criteria above
     */
    Long qcBasesMapped

    /**
     * all bases mapped to the reference genome no filtering applied
     */
    Long allBasesMapped
    /**
     * bases, which were mapped to the specified target regions
     */
    Long onTargetMappedBases

    /**
     * all reads (flagstat QC-passed reads)
     */
    Long totalReadCounter
    /**
     * FailsVendorQualityCheckFlag = true (flagstat QC-failed reads)
     * should be 0
     */
    Long qcFailedReads
    /**
     * duplicateFlag = true, (flagstat value duplicates)
     */
    Long duplicates
    /**
     * every mapped read (flagstat mapped)
     */
    Long totalMappedReadCounter
    /**
     * every paired read (flagstat paired in sequencing)
     */
    Long pairedInSequencing
    /**
     * count of read 2 (flagstat read2)
     */
    Long pairedRead2
    /**
     * count of read 1 (flagstat read1)
     */
    Long pairedRead1
    /**
     * read is mapped in a proper pair (flagstat properly paired)
     */
    Long properlyPaired
    /**
     * read and mate are mapped (flagstat with itself and mate mapped)
     */
    Long withItselfAndMateMapped
    /**
     * read and mate map to different chromosome (flagstat with mate mapped to a different chr)
     */
    Long withMateMappedToDifferentChr
    /**
     * read and mate map to different chromosome and MappingQuality >= 5 (flagstat with mate mapped to a different chr (mapQ>=5))
     */
    Long withMateMappedToDifferentChrMaq
    /**
     * mate or read unmapped but corresponding read mapped (flagstat singletons)
     */
    Long singletons

    /**
     * median over all insert sizes of proper paired mapped reads
     */
    Double insertSizeMedian

    /**
     * standard deviation over all insert sizes of proper paired mapped reads
     */
    Double insertSizeSD


    Double getPercentDuplicates() {
        calculatePercentage(duplicates , totalReadCounter)
    }

    Double getPercentProperlyPaired() {
        calculatePercentage(properlyPaired, pairedInSequencing)
    }

    Double getPercentDiffChr() {
        calculatePercentage(withMateMappedToDifferentChr, totalReadCounter)
    }

    Double getPercentMappedReads() {
        calculatePercentage(totalMappedReadCounter, totalReadCounter)
    }

    Double getPercentSingletons() {
        calculatePercentage(singletons, totalReadCounter)
    }

    Double getOnTargetRatio() {
        calculatePercentage(onTargetMappedBases, allBasesMapped)
    }

    private static Double calculatePercentage(Number numerator, Number denominator) {
        numerator != null && denominator ?
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
