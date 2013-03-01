package de.dkfz.tbi.otp.dataprocessing

class AbstractQualityAssessment {

//    //0 will be all and 1-24 standard chromosome 1-22, X and Y 25 M and 26... unknown sequences

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

    static belongsTo = [
        abstractBamFile: AbstractBamFile
    ]
}
