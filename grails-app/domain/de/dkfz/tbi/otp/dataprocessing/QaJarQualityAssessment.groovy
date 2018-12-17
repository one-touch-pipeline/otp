package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.qcTrafficLight.QcThresholdEvaluated

abstract class QaJarQualityAssessment extends AbstractQualityAssessment {

    /**
     * duplicates count for read 1 and single reads
     */
    @QcThresholdEvaluated
    long duplicateR1
    /**
     * duplicates count for read 2
     */
    @QcThresholdEvaluated
    long duplicateR2
    /**
     * corresponds to incorrectProperPairs in coverageQc.py
     * proper paired flag = 1, same chromosome mapped, pairs to the same strand
     * should always be 0
     */
    @QcThresholdEvaluated
    long properPairStrandConflict
    /**
     * corresponds to totalOrientationCounter in coverageQc.py
     * read and mate have been mapped to the same chromosome
     */
    @QcThresholdEvaluated
    long referenceAgreement
    /**
     * corresponds to badOrientationCounter in coverageQc.py
     * counts cases in which read and mate have been mapped to the same chromosome and strand
     */
    @QcThresholdEvaluated
    long referenceAgreementStrandConflict
    /**
     * length of alignment >= minAlignedRecordLength, meanBaseQuality >= minMeanBaseQuality, read 1
     */
    @QcThresholdEvaluated
    long mappedQualityLongR1
    /**
     * length of alignment >= minAlignedRecordLength, meanBaseQuality >= minMeanBaseQuality, read 2
     */
    @QcThresholdEvaluated
    long mappedQualityLongR2

    /**
     * same as mappedQualityLongR1 but meanBaseQuality < minMeanBaseQuality
     */
    @QcThresholdEvaluated
    long mappedLowQualityR1
    /**
     * same as mappedQualityLongR2 but meanBaseQuality < minMeanBaseQuality
     */
    @QcThresholdEvaluated
    long mappedLowQualityR2
    /**
     * same as mappedLowQualityR1 but length of alignment < minAlignedRecordLength
     */
    @QcThresholdEvaluated
    long mappedShortR1
    /**
     * same as mappedLowQualityR2 but length of alignment < minAlignedRecordLength
     */
    @QcThresholdEvaluated
    long mappedShortR2
    /**
     * all unmappded read 1 not including duplicates
     */
    @QcThresholdEvaluated
    long notMappedR1
    /**
     * all unmappded read 2 not including duplicates
     */
    @QcThresholdEvaluated
    long notMappedR2
    /**
     * properPairFlag=false, read and mate mapped, reads map to different chromosome
     */
    @QcThresholdEvaluated
    long endReadAberration
    /**
     * mean over all insert sizes of proper paired mapped reads
     */
    @QcThresholdEvaluated
    double insertSizeMean
    /**
     * root mean square over all insert sizes of proper paired mapped reads
     */
    @QcThresholdEvaluated
    double insertSizeRMS

    // values not retrieved from the json but calculated afterwards
    /**
     * (referenceAgreementStrandConflict / referenceAgreement) * 100
     */
    @QcThresholdEvaluated
    double percentIncorrectPEorientation
    /**
     * (endReadAberration / totalMappedReadCounter) * 100
     */
    @QcThresholdEvaluated
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
