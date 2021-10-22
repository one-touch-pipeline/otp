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

import de.dkfz.tbi.otp.job.processing.ProcessParameterObject
import de.dkfz.tbi.otp.ngsdata.SeqTrack
import de.dkfz.tbi.otp.workflowExecution.ExternalWorkflowConfigFragment

/**
 * Represents a merged bam file stored on the file system
 * and produced by the merging process identified by the
 * given {@link MergingPass}
 */
@Deprecated
class ProcessedMergedBamFile extends AbstractMergedBamFile implements ProcessParameterObject {

    MergingPass mergingPass

    static belongsTo = [
        mergingPass: MergingPass,
    ]

    static constraints = {
        mergingPass nullable: false, unique: true
        workPackage validator: { val, obj ->
            val.id == obj.mergingSet.mergingWorkPackage.id &&
                    val?.pipeline?.name == Pipeline.Name.DEFAULT_OTP &&
                    MergingWorkPackage.isAssignableFrom(Hibernate.getClass(val))
        }
        md5sum validator: { val, obj ->
            return (!val || (val && obj.fileOperationStatus == FileOperationStatus.PROCESSED && obj.fileSize > 0))
        }
        fileOperationStatus validator: { val, obj ->
            return (val == FileOperationStatus.PROCESSED) == (obj.md5sum != null)
        }
    }

    @Override
    List<AbstractBamFile.BamType> getAllowedTypes() {
        return [AbstractBamFile.BamType.MDUP]
    }

    MergingSet getMergingSet() {
        return mergingPass.mergingSet
    }

    /**
     * @return <code>true</code>, if this {@link ProcessedMergedBamFile} is from the latest merging
     * @see MergingPass#isLatestPass()
     */
    @Override
    boolean isMostRecentBamFile() {
        return (mergingPass.latestPass && mergingSet.latestSet)
    }

    @Override
    String toString() {
        MergingWorkPackage mergingWorkPackage = mergingPass.mergingSet.mergingWorkPackage
        return "PMBF ${id}: " +
        "pass: ${mergingPass.identifier} " + (mergingPass.latestPass ? "(latest) " : "") +
        "set: ${mergingSet.identifier} " + (mergingSet.latestSet ? "(latest) " : "") +
        "<br>sample: ${mergingWorkPackage.sample} " +
        "seqType: ${mergingWorkPackage.seqType} " +
        "<br>project: ${mergingWorkPackage.project}"
    }

    static mapping = { mergingPass index: "abstract_bam_file_merging_pass_idx" }

    @Override
    MergingWorkPackage getMergingWorkPackage() {
        return MergingWorkPackage.get(workPackage.id)
    }

    @Override
    Set<SeqTrack> getContainedSeqTracks() {
        Set<SeqTrack> seqTracks = mergingSet.containedSeqTracks
        if (seqTracks.empty) {
            throw new IllegalStateException("MergingSet ${mergingSet} is empty.")
        }
        return seqTracks
    }

    @Override
    AbstractQualityAssessment getOverallQualityAssessment() {
        OverallQualityAssessmentMerged.createCriteria().get {
            qualityAssessmentMergedPass {
                eq 'abstractMergedBamFile', this
            }
            order 'id', 'desc'
            maxResults 1
        }
    }

    String fileNameNoSuffix() {
        String seqTypeName = "${this.seqType.name}_${this.seqType.libraryLayout}"
        return "${this.sampleType.name}_${this.individual.pid}_${seqTypeName}_merged.mdup"
    }

    @Override
    String getBamFileName() {
        String body = this.fileNameNoSuffix()
        return "${body}.bam"
    }

    @Override
    String getBaiFileName() {
        String body = this.fileNameNoSuffix()
        return "${body}.bai"
    }

    @SuppressWarnings('JavaIoPackageAccess')
    @Override
    protected File getPathForFurtherProcessingNoCheck() {
        return new File(baseDirectory, bamFileName)
    }

    /**
     * @deprecated method is part of the old workflow system, use {@link ExternalWorkflowConfigFragment} instead
     */
    @Override
    @Deprecated
    AlignmentConfig getAlignmentConfig() {
        throw new MissingPropertyException('AlignmentConfig is not implemented for processed merged BAM files')
    }

    @Override
    File getFinalInsertSizeFile() {
        throw new UnsupportedOperationException('not available for ProcessedMergedBamFile')
    }

    @Override
    Integer getMaximalReadLength() {
        throw new UnsupportedOperationException('not used for ProcessedMergedBamFile')
    }

    @Override
    void withdraw() {
        withTransaction {
            super.withdraw()

            withdrawDownstreamBamFiles()
        }
    }
}
