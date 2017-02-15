package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.logging.*
import org.hibernate.Hibernate

/**
 * Represents a merged bam file stored on the file system
 * which was processed externally (not in OTP)
 *
 */
class ExternallyProcessedMergedBamFile extends AbstractMergedBamFile {

    /** source of the file, eg. workflow or import name; used to construct the path of the file */
    String source
    String fileName
    String importedFrom

    @Override
    public String toString() {
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
    public String getBaiFileName() {
        return "${bamFileName}.bai"
    }

    @Override
    void withdraw() {
        withTransaction {

            assert LogThreadLocal.threadLog: 'This method produces relevant log messages. Thread log must be set.'
            LogThreadLocal.threadLog.info "Execute WithdrawnFilesRename.groovy script afterwards"
            LogThreadLocal.threadLog.info "Withdrawing ${this}"
            withdrawn = true
            assert save(flush: true)
        }
    }

    @Override
    ExternalMergingWorkPackage getMergingWorkPackage() {
        return ExternalMergingWorkPackage.get(workPackage.id)
    }

    @Override
    protected File getPathForFurtherProcessingNoCheck() {
        return getFilePath().absoluteDataManagementPath
    }

    @Override
    Set<SeqTrack> getContainedSeqTracks() {
        return []
    }

    @Override
    public AbstractQualityAssessment getOverallQualityAssessment() {
        throw new MissingPropertyException('Quality assessment is not implemented for externally imported BAM files')
    }

    @Override
    public AlignmentConfig getAlignmentConfig() {
        throw new MissingPropertyException('AlignmentConfig is not implemented for externally imported BAM files')
    }

    public OtpPath getFilePath() {
        return new OtpPath(nonOtpFolder,
                "${source}_${referenceGenome}", fileName)
    }

    public OtpPath getNonOtpFolder() {
        String relative = MergedAlignmentDataFileService.buildRelativePath(seqType, sample)
        return new OtpPath(project, relative, "nonOTP")
    }

    static constraints = {
        importedFrom nullable: true, blank: false, validator: { it == null || OtpPath.isValidAbsolutePath(it) }
        source blank: false, validator: { OtpPath.isValidPathComponent(it) }
        fileName blank: false, validator: { OtpPath.isValidPathComponent(it) }
        workPackage validator: { val ->
            val.pipeline.name == Pipeline.Name.EXTERNALLY_PROCESSED &&
                    ExternalMergingWorkPackage.isAssignableFrom(Hibernate.getClass(val))
        }
    }
}
