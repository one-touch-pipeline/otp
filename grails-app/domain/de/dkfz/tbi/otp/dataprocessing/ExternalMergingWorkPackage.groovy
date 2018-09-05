package de.dkfz.tbi.otp.dataprocessing

/**
 * Represents all generations of one merged BAM file (whereas an {@link AbstractMergedBamFile} represents a single
 * generation), which was externally processed (not in OTP).
 */
class ExternalMergingWorkPackage extends AbstractMergingWorkPackage {

    static constraints = {
        pipeline validator: { pipeline -> pipeline.name == Pipeline.Name.EXTERNALLY_PROCESSED }
    }

    @Override
    String toString() {
        return "EMWP ${id}: ${sample} ${seqType} ${referenceGenome}"
    }

    @Override
    AbstractMergedBamFile getBamFileThatIsReadyForFurtherAnalysis() {
        return getProcessableBamFileInProjectFolder()
    }
}
