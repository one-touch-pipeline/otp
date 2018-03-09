package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.ngsdata.*
import org.hibernate.*

/**
 * Represents a merged bam file stored on the file system
 * which was processed externally (not in OTP)
 *
 */
class ExternallyProcessedMergedBamFile extends AbstractMergedBamFile {

    /**
     * Name of the bam file
     */
    String fileName

    /**
     * Absolute path of the imported file.
     */
    String importedFrom

    /**
     * The relative path of insert sizeFile.
     * The file is needed for sophia workflow.
     */
    String insertSizeFile

    /**
     * The maximal read length, needed for sophia
     */
    Integer meanSequenceLength


    static hasMany = [
            furtherFiles: String,
    ]

    @Override
    String toString() {
        return "id: ${id} (external) " +
                "<br>sample: ${sample} seqType: ${seqType} " +
                "<br>project: ${project}"
    }

    @Override
    boolean isMostRecentBamFile() {
        return true
    }

    @Override
    String getBamFileName() {
        return fileName
    }

    @Override
    String getBaiFileName() {
        return "${bamFileName}.bai"
    }

    @Override
    ExternalMergingWorkPackage getMergingWorkPackage() {
        return ExternalMergingWorkPackage.get(workPackage.id)
    }

    @Override
    protected File getPathForFurtherProcessingNoCheck() {
        return getBamFile()
    }

    @Override
    Set<SeqTrack> getContainedSeqTracks() {
        return []
    }

    @Override
    AbstractQualityAssessment getOverallQualityAssessment() {
        throw new MissingPropertyException('Quality assessment is not implemented for externally imported BAM files')
    }

    @Override
    AlignmentConfig getAlignmentConfig() {
        throw new MissingPropertyException('AlignmentConfig is not implemented for externally imported BAM files')
    }

    File getBamFile() {
        return new File(importFolder, bamFileName)
    }

    File getBaiFile() {
        return new File(importFolder, baiFileName)
    }

    File getNonOtpFolder() {
        String relative = MergedAlignmentDataFileService.buildRelativePath(seqType, sample)
        return new OtpPath(project, relative, "nonOTP").absoluteDataManagementPath
    }

    File getImportFolder() {
        return new File(nonOtpFolder, "analysisImport_${referenceGenome}")
    }

    @Override
    File getFinalInsertSizeFile() {
        return new File(importFolder, insertSizeFile)
    }

    @Override
    Integer getMaximalReadLength() {
        return meanSequenceLength
    }


    static constraints = {
        type validator: { true }
        importedFrom nullable: true, blank: false, validator: { it == null || OtpPath.isValidAbsolutePath(it) }
        fileName blank: false, validator: { OtpPath.isValidPathComponent(it) }
        workPackage validator: { val ->
            val && val.pipeline?.name == Pipeline.Name.EXTERNALLY_PROCESSED &&
                    ExternalMergingWorkPackage.isAssignableFrom(Hibernate.getClass(val))
        }
        md5sum nullable: true, validator: { val, obj ->
            return true
        }
        fileOperationStatus validator: { val, obj ->
            return (val == AbstractMergedBamFile.FileOperationStatus.PROCESSED) ? (obj.md5sum != null) : true
        }
        furtherFiles nullable: true
        insertSizeFile nullable: true, blank: false, maxSize: 1000, validator: { val ->
            val == null || OtpPath.isValidRelativePath(val)
        }
        meanSequenceLength nullable: true, min: 0
    }

}
