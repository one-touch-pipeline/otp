package de.dkfz.tbi.otp.domainFactory.pipelines.roddyRna

import de.dkfz.tbi.otp.dataprocessing.Pipeline
import de.dkfz.tbi.otp.dataprocessing.rnaAlignment.RnaRoddyBamFile
import de.dkfz.tbi.otp.domainFactory.pipelines.IsAlignment
import de.dkfz.tbi.otp.domainFactory.pipelines.IsRoddy
import de.dkfz.tbi.otp.ngsdata.LibraryLayout
import de.dkfz.tbi.otp.ngsdata.SeqTypeNames

trait RoddyRnaFactory implements IsAlignment, IsRoddy {

    @Override
    RnaRoddyBamFile createBamFile(Map properties = [:]) {
        return IsRoddy.super.createRoddyBamFile(properties, RnaRoddyBamFile)
    }

    @Override
    Map getSeqTypeProperties() {
        return [
                name         : SeqTypeNames.RNA.seqTypeName,
                displayName  : 'RNA',
                dirName      : 'rna_sequencing',
                roddyName    : 'RNA',
                libraryLayout: LibraryLayout.PAIRED,
                singleCell   : false
        ]
    }

    @Override
    Pipeline findOrCreatePipeline() {
        return findOrCreatePipeline(Pipeline.Name.RODDY_RNA_ALIGNMENT, Pipeline.Type.ALIGNMENT)
    }

    Map getQaValuesProperties() {
        return [
                alternativeAlignments            : 0,
                baseMismatchRate                 : 0.0123456789,
                chimericPairs                    : 0,
                cumulGapLength                   : 123456,
                end1Antisense                    : 12345678,
                end1MappingRate                  : 0.1234567,
                end1MismatchRate                 : 0.0123456789,
                end1PercentageSense              : 0.12345678,
                end1Sense                        : 123456,
                end2Antisense                    : 123456,
                end2MappingRate                  : 0.1234567,
                end2MismatchRate                 : 0.123456789,
                end2PercentageSense              : 12.34567,
                end2Sense                        : 12345678,
                estimatedLibrarySize             : 12345678,
                exonicRate                       : 0.12345678,
                expressionProfilingEfficiency    : 0.1234567,
                failedVendorQCCheck              : 0,
                fivePNorm                        : 0.12345678,
                gapPercentage                    : 0.123456789,
                genesDetected                    : 12345,
                insertSizeMean                   : 123,
                intergenicRate                   : 0.123456789,
                intragenicRate                   : 0.1234567,
                intronicRate                     : 0.12345678,
                mapped                           : 12345678,
                mappedPairs                      : 12345678,
                mappedRead1                      : 12345678,
                mappedRead2                      : 12345678,
                mappedUnique                     : 12345678,
                mappedUniqueRateOfTotal          : 0.12345678,
                mappingRate                      : 0.1234567,
                meanCV                           : 0.12345678,
                meanPerBaseCov                   : 12.34567,
                noCovered5P                      : 123,
                numGaps                          : 123,
                rRNARate                         : 1.234567E-8,
                rRNAReads                        : 123456,
                readLength                       : 123,
                secondaryAlignments              : 0,
                splitReads                       : 12345678,
                supplementaryAlignments          : 0,
                threePNorm                       : 0.12345678,
                totalPurityFilteredReadsSequenced: 123456789,
                transcriptsDetected              : 123456,
                uniqueRateofMapped               : 0.1234567,
                unpairedReads                    : 0,
        ]
    }

    String getQaFileContent(Map properties = [:]) {
        return """\
{
  "all": {
    "alternativeAlignments": 0,
    "baseMismatchRate": 0.0123456789,
    "chimericPairs": 0,
    "cumulGapLength": 123456,
    "duplicates": 12345678,
    "duplicatesRate": 0.1234567,
    "end1Antisense": 12345678,
    "end1MappingRate": 0.1234567,
    "end1MismatchRate": 0.0123456789,
    "end1PercentageSense": 0.12345678,
    "end1Sense": 123456,
    "end2Antisense": 123456,
    "end2MappingRate": 0.1234567,
    "end2MismatchRate": 0.123456789,
    "end2PercentageSense": 12.34567,
    "end2Sense": 12345678,
    "estimatedLibrarySize": 12345678,
    "exonicRate": 0.12345678,
    "expressionProfilingEfficiency": 0.1234567,
    "failedVendorQCCheck": 0,
    "fivePNorm": 0.12345678,
    "gapPercentage": 0.123456789,
    "genesDetected": 12345,
    "insertSizeMean": 123,
    "insertSizeSD": 123,
    "intergenicRate": 0.123456789,
    "intragenicRate": 0.1234567,
    "intronicRate": 0.12345678,
    "mapped": 12345678,
    "mappedPairs": 12345678,
    "mappedRead1": 12345678,
    "mappedRead2": 12345678,
    "mappedUnique": 12345678,
    "mappedUniqueRateOfTotal": 0.12345678,
    "mappingRate": 0.1234567,
    "meanCV": 0.12345678,
    "meanPerBaseCov": 12.34567,
    "noCovered5P": 123,
    "numGaps": 123,
    "pairedInSequencing": 123456789,
    "properlyPaired": 12345678,
    "properlyPairedPercentage": 12.34,
    "qcFailedReads": 0,
    "rRNARate": 1.234567E-8,
    "rRNAReads": 123456,
    "readLength": 123,
    "secondaryAlignments": 0,
    "singletons": 0,
    "singletonsPercentage": 0.12,
    "splitReads": 12345678,
    "supplementaryAlignments": 0,
    "threePNorm": 0.12345678,
    "totalMappedReadCounter": 12345678,
    "totalMappedReadCounterPercentage": 12.34,
    "totalPurityFilteredReadsSequenced": 123456789,
    "totalReadCounter": 123456789,
    "transcriptsDetected": 123456,
    "uniqueRateofMapped": 0.1234567,
    "unpairedReads": 0,
    "withItselfAndMateMapped": 12345678,
    "withMateMappedToDifferentChr": 0,
    "withMateMappedToDifferentChrMaq": 0
  }
}
"""
    }
}
