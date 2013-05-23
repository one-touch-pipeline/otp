package de.dkfz.tbi.ngstools.qualityAssessment

class ChromosomeStatistic {

    String chromosomeName

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

    double insertSizeMedian
    double insertSizeMean
    double insertSizeSD
    double insertSizeRMS

    public ChromosomeStatistic(String chromosomeName, long referenceLength) {
        this.chromosomeName = chromosomeName
        this.referenceLength = referenceLength
    }

    @Override
    public String toString() {
        return chromosomeName
    }
}
