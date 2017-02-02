package de.dkfz.tbi.otp.dataprocessing

import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.utils.logging.LogThreadLocal

/**
 * Represents a merged bam file stored on the file system
 * which was processed externally (not in OTP)
 *
 */
class ExternallyProcessedMergedBamFile extends AbstractMergedBamFile {

    static belongsTo = [
        fastqSet: FastqSet
    ]

    /** source of the file, eg. workflow or import name; used to construct the path of the file */
    String source
    String fileName

    ExternallyProcessedMergedBamFile externallyProcessedMergedBamFile

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

// To implement...
    @Override
    public String getBaiFileName() {
        return "${baiFileName}.bai"
    }

    @Override
    void withdraw() {
        withTransaction {
            //get later bam files
            ExternallyProcessedMergedBamFile.findAllByExternallyProcessedMergedBamFile(this).each {  // better static?
                it.withdraw()
            }

            assert LogThreadLocal.threadLog : 'This method produces relevant log messages. Thread log must be set.'
            LogThreadLocal.threadLog.info "Execute WithdrawnFilesRename.groovy script afterwards"
            LogThreadLocal.threadLog.info "Withdrawing ${this}"
            withdrawn = true
            assert save(flush: true)
        }
    }

    @Override
    protected File getPathForFurtherProcessingNoCheck() {
        return new File(baseDirectory, this.bamFileName)
    }
//***************
    @Override
    Set<SeqTrack> getContainedSeqTracks() {
        return fastqSet.seqTracks
    }

    @Override
    public AbstractQualityAssessment getOverallQualityAssessment() {
        throw new MissingPropertyException('Quality assessment is not implemented for externally imported BAM files')
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
        referenceGenome nullable: false
        source blank: false, validator: { OtpPath.isValidPathComponent(it) }
        fileName blank: false, validator: { OtpPath.isValidPathComponent(it) }
        workPackage unique: true, validator: { workPackage, epmb ->
            workPackage.pipeline.name == Pipeline.Name.EXTERNALLY_PROCESSED &&
                    epmb.containedSeqTracks.every { workPackage.satisfiesCriteria(it) }
        }
        withdrawn validator: { withdrawn, epmb ->
            if (withdrawn) {
                return true
            } else {
                withNewSession {
                    return !epmb.containedSeqTracks.any { it.withdrawn }
                }
            }
        }
        numberOfMergedLanes validator: { numberOfMergedLanes, epmb ->
            numberOfMergedLanes == epmb.containedSeqTracks.size()
        }
    }

    @Override
    public ReferenceGenome getReferenceGenome() {
        return workPackage.referenceGenome
    }


    static mapping = {
        fastqSet index: "abstract_bam_file_fastq_set_idx"
    }
}
