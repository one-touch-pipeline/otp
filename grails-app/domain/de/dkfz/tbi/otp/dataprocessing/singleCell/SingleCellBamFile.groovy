package de.dkfz.tbi.otp.dataprocessing.singleCell

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.cellRanger.*
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.*

class SingleCellBamFile extends AbstractMergedBamFile implements HasIdentifier, ProcessParameterObject {

    static final String METRICS_SUMMARY_CSV_FILE_NAME = "metrics_summary.csv"

    Set<SeqTrack> seqTracks

    String workDirectoryName

    static hasMany = [
            seqTracks: SeqTrack,
    ]

    static constraints = {
        workDirectoryName  validator: { val, obj ->
            OtpPath.isValidRelativePath(val) &&
                    !SingleCellBamFile.findAllByWorkDirectoryName(val).any {
                        it != obj && it.workPackage == obj.workPackage
                    }
        }
        seqTracks minSize: 1
        identifier validator: { val, obj ->
            !SingleCellBamFile.findAllByIdentifier(val).any {
                it != obj && it.workPackage == obj.workPackage
            }
        }
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

    File getWorkDirectory() {
        return new File(baseDirectory, workDirectoryName)
    }

    @Override
    protected File getPathForFurtherProcessingNoCheck() {
        return new File(workDirectory, bamFileName)
    }

    File getQualityAssessmentCsvFile() {
        return new File(workDirectory, METRICS_SUMMARY_CSV_FILE_NAME)
    }

    @Override
    CellRangerMergingWorkPackage getMergingWorkPackage() {
        return CellRangerMergingWorkPackage.get(workPackage?.id)
    }

    @Override
    Set<SeqTrack> getContainedSeqTracks() {
        return seqTracks
    }

    QualityAssessmentMergedPass findOrSaveQaPass() {
        return QualityAssessmentMergedPass.findOrSaveWhere(
                abstractMergedBamFile: this,
                identifier: 0,
        )
    }

    @Override
    CellRangerQualityAssessment getOverallQualityAssessment() {
        CellRangerQualityAssessment.createCriteria().get {
            qualityAssessmentMergedPass {
                eq 'abstractMergedBamFile', this
                eq 'identifier', 0
            }
        } as CellRangerQualityAssessment
    }
}
