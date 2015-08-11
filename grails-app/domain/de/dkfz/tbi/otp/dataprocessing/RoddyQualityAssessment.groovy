package de.dkfz.tbi.otp.dataprocessing

class RoddyQualityAssessment extends AbstractQualityAssessment {

    // We could also link directly to RoddyBamFile, but then Grails fails to start up the application context for an
    // unclear reason. So as a workaround we have an indirect link via QualityAssessmentMergedPass.
    QualityAssessmentMergedPass qualityAssessmentMergedPass

    static belongsTo = QualityAssessmentMergedPass

    static final String ALL = 'all'

    /**
     * This property holds the name of the chromosome, these QC values belong to.
     * In case the QC values are for the complete genome the name will be {@link #ALL}.
     */
    String chromosome

    Double insertSizeCV

    Double percentageMatesOnDifferentChr

    double genomeWithoutNCoverageQcBases

    static def nullIfAndOnlyIfPerChromosomeQc = { val, RoddyQualityAssessment obj ->
        return (val == null) == (obj.chromosome != ALL)
    }

    static constraints = {
        qualityAssessmentMergedPass(validator: { it.processedMergedBamFile instanceof RoddyBamFile })

        chromosome(blank: false)

        insertSizeCV(nullable: true, validator: nullIfAndOnlyIfPerChromosomeQc)
        percentageMatesOnDifferentChr(nullable: true, validator: nullIfAndOnlyIfPerChromosomeQc)

        totalReadCounter(validator: nullIfAndOnlyIfPerChromosomeQc)
        qcFailedReads(validator: nullIfAndOnlyIfPerChromosomeQc)
        duplicates(validator: nullIfAndOnlyIfPerChromosomeQc)
        totalMappedReadCounter(validator: nullIfAndOnlyIfPerChromosomeQc)
        pairedInSequencing(validator: nullIfAndOnlyIfPerChromosomeQc)
        pairedRead2(validator: nullIfAndOnlyIfPerChromosomeQc)
        pairedRead1(validator: nullIfAndOnlyIfPerChromosomeQc)
        properlyPaired(validator: nullIfAndOnlyIfPerChromosomeQc)
        withItselfAndMateMapped(validator: nullIfAndOnlyIfPerChromosomeQc)
        withMateMappedToDifferentChr(validator: nullIfAndOnlyIfPerChromosomeQc)
        withMateMappedToDifferentChrMaq(validator: nullIfAndOnlyIfPerChromosomeQc)
        singletons(validator: nullIfAndOnlyIfPerChromosomeQc)
        insertSizeMedian(validator: nullIfAndOnlyIfPerChromosomeQc)
        insertSizeSD(validator: nullIfAndOnlyIfPerChromosomeQc)
    }

    static mapping = {
        // An index on qualityAssessmentMergedPass is defined in OverallQualityAssessmentMerged
    }

    RoddyBamFile getRoddyBamFile() {
        return (RoddyBamFile)qualityAssessmentMergedPass.processedMergedBamFile
    }

    // Created to have an identical way to receive the chromosome identifier as in ChromosomeQualityAssessmentMerged
    String getChromosomeName() {
        return chromosome
    }
}
