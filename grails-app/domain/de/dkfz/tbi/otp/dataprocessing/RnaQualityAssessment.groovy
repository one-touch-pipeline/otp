package de.dkfz.tbi.otp.dataprocessing

class RnaQualityAssessment extends AbstractQualityAssessment {

    double threePNorm

    double fivePNorm

    long alternativeAlignments

    double baseMismatchRate

    long chimericPairs

    long cumulGapLength

    double end1PercentageSense

    long end1Antisense

    double end1MappingRate

    double end1MismatchRate

    double end1Sense

    double end2PercentageSense

    long end2Antisense

    double end2MappingRate

    double end2MismatchRate

    double end2Sense

    long estimatedLibrarySize

    double exonicRate

    double expressionProfilingEfficiency

    long failedVendorQCCheck

    double insertSizeMean

    double gapPercentage

    long genesDetected

    double intergenicRate

    double intragenicRate

    double intronicRate

    long mapped

    long mappedPairs

    long  mappedUnique

    double mappedUniqueRateOfTotal

    double mappingRate

    double meanCV

    double meanPerBaseCov

    long noCovered5P

    long numGaps

    long readLength

    long splitReads

    long totalPurityFilteredReadsSequenced

    long transcriptsDetected

    double uniqueRateofMapped

    long unpairedReads

    long rRNAReads

    double rRNARate

    long mappedRead1

    long mappedRead2

    double duplicatesRate

    double properlyPairedPercentage

    long secondaryAlignments

    long supplementaryAlignments

    double totalMappedReadCounterPercentage

    double singletonsPercentage

    static constraints = {
        referenceLength(nullable: true)
    }

}
