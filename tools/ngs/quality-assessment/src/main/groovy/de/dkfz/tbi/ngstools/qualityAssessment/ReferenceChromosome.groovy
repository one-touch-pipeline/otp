package de.dkfz.tbi.ngstools.qualityAssessment

import net.sf.samtools.SAMRecord

/**
 * This class mirror database structure and holds quality assessments for single chromosome
 */
class ReferenceChromosome {

    private String chromosomeName
    private long referenceLength
    private long duplicateR1
    private long duplicateR2
    private long properPairStrandConflict
    private long referenceAgreement
    private long referenceAgreementStrandConflict
    private long mappedQualityLongR1
    private long mappedQualityLongR2
    private long qcBasesMapped
    private long mappedLowQualityR1
    private long mappedLowQualityR2
    private long mappedShortR1
    private long mappedShortR2
    private long notMappedR1
    private long notMappedR2

    private long endReadAberration
    private long totalReadCounter
    private long qcFailedReads
    private long duplicates
    private long totalMappedReadCounter
    private long pairedInSequencing
    private long pairedRead2
    private long pairedRead1
    private long properlyPaired
    private long withItselfAndMateMapped
    private long withMateMappedToDifferentChr
    private long withMateMappedToDifferentChrMaq
    private long singletons

    private double insertSizeMedian
    private double insertSizeMean
    private double insertSizeSD
    private double insertSizeRMS

    /**
     * Creates chromosome with provided name and initializes values.
     * @param referenceName
     */
    ReferenceChromosome(String referenceName) {
        this.chromosomeName = referenceName
    }

    ReferenceChromosome(String referenceName, long length) {
        this.chromosomeName = referenceName
        this.referenceLength = length
    }

    void incrementEndReadAberration() {
        endReadAberration++
    }

    void incrementTotalReadCounter() {
        totalReadCounter++
    }

    void incrementQCFailure() {
        qcFailedReads++
    }

    void incrementDuplicates() {
        duplicates++
    }

    void incrementTotalMappedReadCounter() {
        totalMappedReadCounter++
    }

    void incrementPairedInSequencing() {
        pairedInSequencing++
    }

    void incrementPairedRead2() {
        pairedRead2++
    }

    void incrementPairedRead1() {
        pairedRead1++
    }

    void incrementProperlyPaired() {
        properlyPaired++
    }

    void incrementWithItselfAndMateMapped() {
        withItselfAndMateMapped++
    }

    void incrementWithMateMappedToDifferentChr() {
        withMateMappedToDifferentChr++
    }

    void incrementWithMateMappedToDifferentChrMaq() {
        withMateMappedToDifferentChrMaq++
    }

    void incrementSingletons() {
        singletons++
    }

    void incrementDuplicate(SAMRecord rec) {
        if (rec.getFirstOfPairFlag()) {
            duplicateR1++
        } else {
            duplicateR2++
        }
    }

    void incrementProperPairStrandConflict() {
        properPairStrandConflict++
    }

    void incrementReferenceAgreement() {
        referenceAgreement++
    }

    void incrementReferenceAgreementStrandConflict() {
        referenceAgreementStrandConflict++
    }

    void incrementMappedQualityLong(SAMRecord rec) {
        if (rec.getSecondOfPairFlag()) {
            mappedQualityLongR2++
        } else {
            mappedQualityLongR1++
        }
    }

    void increaseQcBases(long value) {
        qcBasesMapped = qcBasesMapped + value
    }

    void incrementMappedLowQuality(SAMRecord rec) {
        if (rec.getSecondOfPairFlag()) {
            mappedLowQualityR2++
        } else {
            mappedLowQualityR1++
        }
    }

    void incrementMappedShort(SAMRecord rec) {
        if (rec.getSecondOfPairFlag()) {
            mappedShortR2++
        } else {
            mappedShortR1++
        }
    }

    void incrementNotMapped(SAMRecord rec) {
        if (rec.getSecondOfPairFlag()) {
            notMappedR2++
        } else {
            notMappedR1++
        }
    }
}
