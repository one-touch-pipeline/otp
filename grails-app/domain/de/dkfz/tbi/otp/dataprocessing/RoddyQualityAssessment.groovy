package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.ngsdata.*
import org.hibernate.*

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

    Double genomeWithoutNCoverageQcBases

    static def nullIfAndOnlyIfPerChromosomeQc = { val, RoddyQualityAssessment obj ->
        if (obj.chromosome == ALL && val == null) {
            return "value must be set for all chromosomes, but is null"
        } else if (obj.chromosome != ALL && val != null) {
            return "value must be null for single chromosome ${obj.chromosome}, but is ${val}"
        }
    }

    static constraints = {
        qualityAssessmentMergedPass(validator: {
            RoddyBamFile.isAssignableFrom(Hibernate.getClass(it.abstractMergedBamFile))
        })

        chromosome blank: false

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
        // not available for RNA
        genomeWithoutNCoverageQcBases nullable: true, validator: { it != null }
    }

    static mapping = {
        // An index on qualityAssessmentMergedPass is defined in OverallQualityAssessmentMerged
    }

    RoddyBamFile getRoddyBamFile() {
        return (RoddyBamFile)qualityAssessmentMergedPass.abstractMergedBamFile
    }

    // Created to have an identical way to receive the chromosome identifier as in ChromosomeQualityAssessmentMerged
    String getChromosomeName() {
        return chromosome
    }

    ReferenceGenome getReferenceGenome() {
        return qualityAssessmentMergedPass.referenceGenome
    }

}
