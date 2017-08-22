package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.ngsdata.*

class RnaQualityAssessment extends RoddyQualityAssessment {

    double threePNorm

    double fivePNorm

    long alternativeAlignments

    double baseMismatchRate

    long chimericPairs

    long cumulGapLength

    Double end1PercentageSense

    long end1Antisense

    double end1MappingRate

    Double end1MismatchRate

    double end1Sense

    Double end2PercentageSense

    long end2Antisense

    double end2MappingRate

    Double end2MismatchRate

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

    Double properlyPairedPercentage

    long secondaryAlignments

    long supplementaryAlignments

    double totalMappedReadCounterPercentage

    Double singletonsPercentage

    static constraints = {
        genomeWithoutNCoverageQcBases validator: { it == null }
        insertSizeCV validator: { it == null }
        insertSizeMedian validator: { it == null }
        pairedRead1 validator: { it == null }
        pairedRead2 validator: { it == null }
        percentageMatesOnDifferentChr validator: { it == null }
        referenceLength validator: { it == null }
        cumulGapLength nullable: true
        fivePNorm nullable: true
        gapPercentage nullable: true
        meanCV nullable: true
        meanPerBaseCov nullable: true
        noCovered5P nullable: true
        numGaps nullable: true
        threePNorm nullable: true
        end2PercentageSense nullable: true, validator: nullIfAndOnlyIfLayoutIsSingle
        end2MismatchRate nullable: true, validator: nullIfAndOnlyIfLayoutIsSingle
        end1PercentageSense nullable: true, validator: nullIfAndOnlyIfLayoutIsSingle
        end1MismatchRate nullable: true, validator: nullIfAndOnlyIfLayoutIsSingle
        properlyPaired nullable: true, validator: nullIfAndOnlyIfLayoutIsSingle
        properlyPairedPercentage nullable: true, validator: nullIfAndOnlyIfLayoutIsSingle
        singletons nullable: true, validator: nullIfAndOnlyIfLayoutIsSingle
        singletonsPercentage nullable: true, validator: nullIfAndOnlyIfLayoutIsSingle
    }

    static def nullIfAndOnlyIfLayoutIsSingle = { val, RnaQualityAssessment obj ->
        if (obj.roddyBamFile.seqType.libraryLayout == SeqType.LIBRARYLAYOUT_PAIRED && val == null) {
            return "value must be set if layout is paired, but is null"
        } else if (obj.roddyBamFile.seqType.libraryLayout == SeqType.LIBRARYLAYOUT_SINGLE && val != null) {
            return "value must be null if layout is single"
        }
    }
}
