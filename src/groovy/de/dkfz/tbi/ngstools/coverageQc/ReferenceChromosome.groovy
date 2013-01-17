package de.dkfz.tbi.ngstools.coverageQc

import net.sf.samtools.SAMRecord


/**
 * This class mirror database structure and holds quality assessments for single chromosome
 */
 class ReferenceChromosome {

    private String referenceName
    private long duplicateR1
    private long duplicateR2
    private long properPairStrandConflict
    private long referenceAgreement
    private long referenceAgreementStrandConflict
    private long mappedQualityLongR1
    private long mappedQualityLongR2
    private long qcBases
    private long mappedLowQualityR1
    private long mappedLowQualityR2
    private long mappedShortR1
    private long mappedShortR2
    private long notMappedR1
    private long notMappedR2

    /**
     * Creates chromosome with provided name and initializes values.
     * @param referenceName
     */
     ReferenceChromosome(String referenceName) {
        this.referenceName = referenceName
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
        qcBases = qcBases+value
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
