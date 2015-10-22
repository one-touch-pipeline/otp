package de.dkfz.tbi.otp.dataprocessing


class QaJarQualityAssessment extends AbstractQualityAssessment {

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
     * mean over all insert sizes of proper paired mapped reads
     */
    double insertSizeMean
    /**
     * root mean square over all insert sizes of proper paired mapped reads
     */
    double insertSizeRMS

    // values not retrieved from the json but calculated afterwards
    /**
     * (referenceAgreementStrandConflict / referenceAgreement) * 100
     */
    double percentIncorrectPEorientation
    /**
     * (endReadAberration / totalMappedReadCounter) * 100
     */
    double percentReadPairsMapToDiffChrom


    static constraints = {
        qcBasesMapped(nullable: false)
        totalReadCounter(nullable: false)
        qcFailedReads(nullable: false)
        duplicates(nullable: false)
        totalMappedReadCounter(nullable: false)
        pairedInSequencing(nullable: false)
        pairedRead2(nullable: false)
        pairedRead1(nullable: false)
        properlyPaired(nullable: false)
        withItselfAndMateMapped(nullable: false)
        withMateMappedToDifferentChr(nullable: false)
        withMateMappedToDifferentChrMaq(nullable: false)
        singletons(nullable: false)
        insertSizeMedian(nullable: false)
        insertSizeSD(nullable: false)
    }
}
