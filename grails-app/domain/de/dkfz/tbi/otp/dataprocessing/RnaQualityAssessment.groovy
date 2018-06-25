package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.qcTrafficLight.*

class RnaQualityAssessment extends RoddyQualityAssessment implements QcTrafficLightValue {

    @QcThresholdEvaluated
    double threePNorm

    @QcThresholdEvaluated
    double fivePNorm

    @QcThresholdEvaluated
    long alternativeAlignments

    @QcThresholdEvaluated
    double baseMismatchRate

    @QcThresholdEvaluated
    long chimericPairs

    @QcThresholdEvaluated
    long cumulGapLength

    @QcThresholdEvaluated
    Double end1PercentageSense

    @QcThresholdEvaluated
    long end1Antisense

    @QcThresholdEvaluated
    double end1MappingRate

    @QcThresholdEvaluated
    Double end1MismatchRate

    @QcThresholdEvaluated
    double end1Sense

    @QcThresholdEvaluated
    Double end2PercentageSense

    @QcThresholdEvaluated
    long end2Antisense

    @QcThresholdEvaluated
    double end2MappingRate

    @QcThresholdEvaluated
    Double end2MismatchRate

    @QcThresholdEvaluated
    double end2Sense

    @QcThresholdEvaluated
    long estimatedLibrarySize

    @QcThresholdEvaluated
    double exonicRate

    @QcThresholdEvaluated
    double expressionProfilingEfficiency

    @QcThresholdEvaluated
    long failedVendorQCCheck

    @QcThresholdEvaluated
    double insertSizeMean

    @QcThresholdEvaluated
    double gapPercentage

    @QcThresholdEvaluated
    long genesDetected

    @QcThresholdEvaluated
    double intergenicRate

    @QcThresholdEvaluated
    double intragenicRate

    @QcThresholdEvaluated
    double intronicRate

    @QcThresholdEvaluated
    long mapped

    @QcThresholdEvaluated
    long mappedPairs

    @QcThresholdEvaluated
    long  mappedUnique

    @QcThresholdEvaluated
    double mappedUniqueRateOfTotal

    @QcThresholdEvaluated
    double mappingRate

    @QcThresholdEvaluated
    double meanCV

    @QcThresholdEvaluated
    double meanPerBaseCov

    @QcThresholdEvaluated
    long noCovered5P

    @QcThresholdEvaluated
    long numGaps

    @QcThresholdEvaluated
    long readLength

    @QcThresholdEvaluated
    long splitReads

    @QcThresholdEvaluated
    long totalPurityFilteredReadsSequenced

    @QcThresholdEvaluated
    long transcriptsDetected

    @QcThresholdEvaluated
    double uniqueRateofMapped

    @QcThresholdEvaluated
    long unpairedReads

    @QcThresholdEvaluated
    long rRNAReads

    @QcThresholdEvaluated
    double rRNARate

    @QcThresholdEvaluated
    long mappedRead1

    @QcThresholdEvaluated
    long mappedRead2

    @QcThresholdEvaluated
    double duplicatesRate

    @QcThresholdEvaluated
    Double properlyPairedPercentage

    @QcThresholdEvaluated
    long secondaryAlignments

    @QcThresholdEvaluated
    long supplementaryAlignments

    @QcThresholdEvaluated
    double totalMappedReadCounterPercentage

    @QcThresholdEvaluated
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
