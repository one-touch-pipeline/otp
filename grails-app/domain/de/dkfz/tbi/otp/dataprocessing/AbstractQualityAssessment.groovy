package de.dkfz.tbi.otp.dataprocessing

class AbstractQualityAssessment {

    /**
     * length of the chromosome/genome of the reference
     */
    long referenceLength

    /**
     * length of alignment >= minAlignedRecordLength
     * meanBaseQuality >= minMeanBaseQuality
     * sum of all mapped bases including gaps which pass both criteria above
     */
    long qcBasesMapped

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
     * duplicateFlag = true, (flagtstat value duplicates)
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

    static constraints = {
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
