package de.dkfz.tbi.otp.dataprocessing.singleCell

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.*

class SingleCellBamFile extends AbstractMergedBamFile implements HasIdentifier {

    Set<SeqTrack> seqTracks

    String workDirectoryName

    static hasMany = [
            seqTracks: SeqTrack,
    ]

    static constraints = {
        workDirectoryName unique: 'workPackage', validator: {
            OtpPath.isValidRelativePath(it)
        }
        seqTracks minSize: 1
        identifier unique: 'workPackage'
    }

    @Override
    boolean isMostRecentBamFile() {
        return identifier == maxIdentifier(mergingWorkPackage)
    }

    @Override
    String getBamFileName() {
        String antiBodyTarget = seqType.isChipSeq() ? "-${((MergingWorkPackage) mergingWorkPackage).antibodyTarget.name}" : ''
        return "${sampleType.dirName}${antiBodyTarget}_${individual.pid}_merged.mdup.bam"
    }

    @Override
    String getBaiFileName() {
        return "${bamFileName}.bai"
    }

    @Override
    AlignmentConfig getAlignmentConfig() {
        return mergingWorkPackage.config
    }

    @Override
    File getFinalInsertSizeFile() {
        throw new MissingPropertyException("Final insert size file is not implemented for single cell BAM files")
    }

    @Override
    Integer getMaximalReadLength() {
        throw new MissingPropertyException("Maximal read length is not implemented for single cell BAM files")
    }

    @Override
    protected File getPathForFurtherProcessingNoCheck() {
        return new File(workDirectoryName, bamFileName)
    }

    @Override
    CellRangerMergingWorkPackage getMergingWorkPackage() {
        return CellRangerMergingWorkPackage.get(workPackage?.id)
    }

    @Override
    Set<SeqTrack> getContainedSeqTracks() {
        return seqTracks
    }

    @SuppressWarnings("DeadCode")
    @Override
    SingleCellQualityAssessment getOverallQualityAssessment() {
        throw new UnsupportedOperationException("This Method is not yet implemented.") //This has to be removed once all required Domain Classes are implemented
        SingleCellQualityAssessment.createCriteria().get {
            qualityAssessmentMergedPass {
                eq 'abstractMergedBamFile', this
            }
            order 'id', 'desc'
            maxResults 1
        }
    }
}
