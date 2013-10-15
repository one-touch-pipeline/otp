package de.dkfz.tbi.ngstools.qualityAssessment

class ChromosomeStatistic {

    String chromosomeName
    /**
     * length of the chromosome/genome of the reference
     */
    long referenceLength
    /**
     * duplicates count for read 1 and single reads
     */
    long duplicateR1
    /**
     * duplicates count for read 2
     */
    long duplicateR2
    /**
     * corresponds to incorrectProperPairs in coverageQc.py
     * proper paired flag = 1, same chromosome mapped, pairs to the same strand
     * should always be 0
     */
    long properPairStrandConflict
    /**
     * corresponds to totalOrientationCounter in coverageQc.py
     * read and mate have been mapped to the same chromosome
     */
    long referenceAgreement
    /**
     * corresponds to badOrientationCounter in coverageQc.py
     * counts cases in which read and mate have been mapped to the same chromosome and strand
     */
    long referenceAgreementStrandConflict
    /**
     * length of alignment >= minAlignedRecordLength, meanBaseQuality >= minMeanBaseQuality, read 1
     */
    long mappedQualityLongR1
    /**
     * length of alignment >= minAlignedRecordLength, meanBaseQuality >= minMeanBaseQuality, read 2
     */
    long mappedQualityLongR2
    /**
     * length of alignment >= minAlignedRecordLength
     * meanBaseQuality >= minMeanBaseQuality
     * sum of all mapped bases including gaps which pass both criteria above
     */
    long qcBasesMapped
    /**
     * same as mappedQualityLongR1 but meanBaseQuality < minMeanBaseQuality
     */
    long mappedLowQualityR1
    /**
     * same as mappedQualityLongR2 but meanBaseQuality < minMeanBaseQuality
     */
    long mappedLowQualityR2
    /**
     * same as mappedLowQualityR1 but length of alignment < minAlignedRecordLength
     */
    long mappedShortR1
    /**
     * same as mappedLowQualityR2 but length of alignment < minAlignedRecordLength
     */
    long mappedShortR2
    /**
     * all unmappded read 1 not including duplicates
     */
    long notMappedR1
    /**
     * all unmappded read 2 not including duplicates
     */
    long notMappedR2
    /**
     * properPairFlag=false, read and mate mapped, reads map to different chromosome
     */
    long endReadAberration
    /**
     * all reads (flagstat QC-passed reads)
     */
    long totalReadCounter
    /**
     * FailsVendorQualityCheckFlag = true (flagstat QC-failed reads)
     * should be 0
     */
    long qcFailedReads
    /**
     * duplicateFlag = true, (flagtstat value duplicates)
     */
    long duplicates
    /**
     * every mapped read (flagstat mapped)
     */
    long totalMappedReadCounter
    /**
     * every paired read (flagstat paired in sequencing)
     */
    long pairedInSequencing
    /**
     * count of read 2 (flagstat read2)
     */
    long pairedRead2
    /**
     * count of read 1 (flagstat read1)
     */
    long pairedRead1
    /**
     * read is mapped in a proper pair (flagstat properly paired)
     */
    long properlyPaired
    /**
     * read and mate are mapped (flagstat with itself and mate mapped)
     */
    long withItselfAndMateMapped
    /**
     * read and mate map to different chromosome (flagstat with mate mapped to a different chr)
     */
    long withMateMappedToDifferentChr
    /**
     * read and mate map to different chromosome and MappingQuality >= 5 (flagstat with mate mapped to a different chr (mapQ>=5))
     */
    long withMateMappedToDifferentChrMaq
    /**
     * mate or read unmapped but corresponding read mapped (flagstat singletons)
     */
    long singletons

    /**
     * median over all insert sizes of proper paired mapped reads
     */
    double insertSizeMedian
    /**
     * mean over all insert sizes of proper paired mapped reads
     */
    double insertSizeMean
    /**
     * standard deviation over all insert sizes of proper paired mapped reads
     */
    double insertSizeSD
    /**
     * root mean square over all insert sizes of proper paired mapped reads
     */
    double insertSizeRMS
    /**
     * all bases mapped to the reference genome no filtering applied
     */
    long allBasesMapped
    /**
     * bases, which were mapped to the specified target regions
     */
    long onTargetMappedBases

    public ChromosomeStatistic(String chromosomeName, long referenceLength) {
        this.chromosomeName = chromosomeName
        this.referenceLength = referenceLength
    }

    @Override
    public String toString() {
        return chromosomeName
    }
}
