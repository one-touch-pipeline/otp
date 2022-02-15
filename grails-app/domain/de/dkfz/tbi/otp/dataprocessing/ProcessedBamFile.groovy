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

import grails.gorm.hibernate.annotation.ManagedEntity

import de.dkfz.tbi.otp.job.processing.ProcessParameterObject
import de.dkfz.tbi.otp.ngsdata.SeqTrack

@Deprecated
@ManagedEntity
class ProcessedBamFile extends AbstractFileSystemBamFile implements ProcessParameterObject {

    AlignmentPass alignmentPass

    static belongsTo = [
        alignmentPass: AlignmentPass,
    ]

    static constraints = {
        alignmentPass nullable: false, unique: true, validator: { AlignmentPass alignmentPass ->
            return alignmentPass.referenceGenome != null
        }
    }

    @Override
    List<AbstractBamFile.BamType> getAllowedTypes() {
        return [AbstractBamFile.BamType.SORTED]
    }

    SeqTrack getSeqTrack() {
        return alignmentPass.seqTrack
    }

    @Override
    String toString() {
        return "PBF ${id} of ${alignmentPass}"
    }

    /**
     * @return <code>true</code>, if this {@link ProcessedBamFile} is from the latest alignment
     * @see AlignmentPass#isLatestPass()
     */
    boolean isMostRecentBamFile() {
        return alignmentPass.isLatestPass()
    }

    @Override
    MergingWorkPackage getMergingWorkPackage() {
        return alignmentPass.workPackage
    }

    @Override
    Set<SeqTrack> getContainedSeqTracks() {
        return new HashSet<SeqTrack>([alignmentPass.seqTrack])
    }

    @Override
    AbstractQualityAssessment getOverallQualityAssessment() {
        OverallQualityAssessment.createCriteria().get {
            qualityAssessmentPass {
                eq 'processedBamFile', this
            }
            order 'id', 'desc'
            maxResults 1
        }
    }

    static mapping = {
        alignmentPass index: "abstract_bam_file_alignment_pass_idx"
    }

    @Override
    void withdraw() {
        withTransaction {
            super.withdraw()

            withdrawDownstreamBamFiles()
        }
    }
}
