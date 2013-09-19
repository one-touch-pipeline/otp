package de.dkfz.tbi.otp.dataprocessing

class AbstractQualityAssessment {

    long referenceLength
    long duplicateR1
    long duplicateR2
    long properPairStrandConflict
    long referenceAgreement
    long referenceAgreementStrandConflict
    long mappedQualityLongR1
    long mappedQualityLongR2
    long qcBasesMapped
    long mappedLowQualityR1
    long mappedLowQualityR2
    long mappedShortR1
    long mappedShortR2
    long notMappedR1
    long notMappedR2
    long endReadAberration

    //flagstats
    long totalReadCounter
    long qcFailedReads
    long duplicates
    long totalMappedReadCounter
    long pairedInSequencing
    long pairedRead2
    long pairedRead1
    long properlyPaired
    long withItselfAndMateMapped
    long withMateMappedToDifferentChr
    long withMateMappedToDifferentChrMaq
    long singletons

    double insertSizeMean
    double insertSizeSD
    double insertSizeMedian
    double insertSizeRMS

    // values not retrieved from the json but calculated afterwards
    double percentIncorrectPEorientation
    double percentReadPairsMapToDiffChrom
}
