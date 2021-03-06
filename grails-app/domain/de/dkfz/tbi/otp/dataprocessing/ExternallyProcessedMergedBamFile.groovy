/*
 * Copyright 2011-2019 The OTP authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package de.dkfz.tbi.otp.dataprocessing

import org.hibernate.Hibernate

import de.dkfz.tbi.otp.ngsdata.MergedAlignmentDataFileService
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.utils.CollectionUtils

/**
 * Represents a merged bam file stored on the file system
 * which was processed externally (not in OTP)
 */
@SuppressWarnings('JavaIoPackageAccess')
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
    Integer maximumReadLength

    Set<String> furtherFiles = [] as Set<String>


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
        return CollectionUtils.exactlyOneElement(ExternalProcessedMergedBamFileQualityAssessment.createCriteria().list {
            qualityAssessmentMergedPass {
                abstractMergedBamFile {
                    eq 'id', this.id
                }
            }
        })
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

    File getBamMaxReadLengthFile() {
        return new File(importFolder, "${bamFileName}.maxReadLength")
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
        return maximumReadLength
    }


    static constraints = {
        importedFrom nullable: true, blank: false, shared: "absolutePath"
        fileName blank: false, shared: "pathComponent"
        workPackage validator: { val, obj ->
            List<ExternallyProcessedMergedBamFile> epmbfs = ExternallyProcessedMergedBamFile.findAllByFileName(obj.fileName).findAll {
                it.sample == val.sample && it.seqType == val.seqType && it.referenceGenome == val.referenceGenome
            }
            if (epmbfs && (epmbfs.size() != 1 || CollectionUtils.exactlyOneElement(epmbfs) != obj)) {
                return "exists"
            }

            val && val.pipeline?.name == Pipeline.Name.EXTERNALLY_PROCESSED &&
                    ExternalMergingWorkPackage.isAssignableFrom(Hibernate.getClass(val))
        }
        fileOperationStatus validator: { val, obj ->
            return (val == AbstractMergedBamFile.FileOperationStatus.PROCESSED) ? (obj.md5sum != null) : true
        }
        furtherFiles nullable: true
        insertSizeFile nullable: true, blank: false, maxSize: 1000, shared: "relativePath"
        maximumReadLength nullable: true, min: 0
    }

    @Override
    List<AbstractBamFile.BamType> getAllowedTypes() {
        return AbstractBamFile.BamType.values() + [null]
    }
}
