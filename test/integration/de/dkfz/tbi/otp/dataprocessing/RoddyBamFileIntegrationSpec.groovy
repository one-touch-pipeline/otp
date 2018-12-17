package de.dkfz.tbi.otp.dataprocessing

import spock.lang.Specification

import de.dkfz.tbi.otp.ngsdata.DomainFactory

class RoddyBamFileIntegrationSpec extends Specification {

    static final Long ARBITRARY_UNUSED_VALUE = 1

    static final Map ARBITRARY_QA_VALUES = [
            qcBasesMapped                  : ARBITRARY_UNUSED_VALUE,
            totalReadCounter               : ARBITRARY_UNUSED_VALUE,
            qcFailedReads                  : ARBITRARY_UNUSED_VALUE,
            duplicates                     : ARBITRARY_UNUSED_VALUE,
            totalMappedReadCounter         : ARBITRARY_UNUSED_VALUE,
            pairedInSequencing             : ARBITRARY_UNUSED_VALUE,
            pairedRead2                    : ARBITRARY_UNUSED_VALUE,
            pairedRead1                    : ARBITRARY_UNUSED_VALUE,
            properlyPaired                 : ARBITRARY_UNUSED_VALUE,
            withItselfAndMateMapped        : ARBITRARY_UNUSED_VALUE,
            withMateMappedToDifferentChr   : ARBITRARY_UNUSED_VALUE,
            withMateMappedToDifferentChrMaq: ARBITRARY_UNUSED_VALUE,
            singletons                     : ARBITRARY_UNUSED_VALUE,
            insertSizeMedian               : ARBITRARY_UNUSED_VALUE,
            insertSizeSD                   : ARBITRARY_UNUSED_VALUE,
            referenceLength                : ARBITRARY_UNUSED_VALUE,
    ].asImmutable()

    static final Map NULL_QA_VALUES = [
            qcBasesMapped                  : null,
            totalReadCounter               : null,
            qcFailedReads                  : null,
            duplicates                     : null,
            totalMappedReadCounter         : null,
            pairedInSequencing             : null,
            pairedRead2                    : null,
            pairedRead1                    : null,
            properlyPaired                 : null,
            withItselfAndMateMapped        : null,
            withMateMappedToDifferentChr   : null,
            withMateMappedToDifferentChrMaq: null,
            singletons                     : null,
            insertSizeMedian               : null,
            insertSizeSD                   : null,
            referenceLength                : null,
            percentageMatesOnDifferentChr  : null,
            insertSizeCV  : null,
    ].asImmutable()

    void "test getNumberOfReadsFromQa"() {
        given:
        long pairedRead = DomainFactory.counter++
        long numberOfReads = 2 * pairedRead
        RoddyMergedBamQa qa = DomainFactory.createRoddyMergedBamQa([
                pairedRead1    : pairedRead,
                pairedRead2    : pairedRead,
                referenceLength: 0,
        ])
        RoddyBamFile roddyBamFile = qa.roddyBamFile

        expect:
        numberOfReads == roddyBamFile.numberOfReadsFromQa
    }

    void "test getOverallQualityAssessment"() {
        given:
        RoddyBamFile bamFile = DomainFactory.createRoddyBamFile()
        QualityAssessmentMergedPass qaPass = DomainFactory.createQualityAssessmentMergedPass(
                abstractMergedBamFile: bamFile,
        )
        DomainFactory.createRoddyMergedBamQa(
                NULL_QA_VALUES + [
                        qualityAssessmentMergedPass  : qaPass,
                        chromosome                   : '12',
                        referenceLength              : 1,
                        genomeWithoutNCoverageQcBases: 1,
                ]
        )

        RoddyMergedBamQa mergedQa = DomainFactory.createRoddyMergedBamQa(
                ARBITRARY_QA_VALUES + [
                        qualityAssessmentMergedPass  : qaPass,
                        chromosome                   : RoddyQualityAssessment.ALL,
                        insertSizeCV                 : 123,
                        percentageMatesOnDifferentChr: 0.123,
                        genomeWithoutNCoverageQcBases: 1,
                ]
        )

        expect:
        mergedQa == bamFile.overallQualityAssessment
    }
}
