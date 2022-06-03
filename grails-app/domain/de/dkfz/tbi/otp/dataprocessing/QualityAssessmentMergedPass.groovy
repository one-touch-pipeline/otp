/*
 * Copyright 2011-2020 The OTP authors
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
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.Entity

/**
 * Each execution of the Quality Assessment Merged Workflow on the particular data file (merged bam file) is represented as QualityAssessmentMergedPass.
 */
class QualityAssessmentMergedPass implements ProcessParameterObject, Entity {

    AbstractMergedBamFile abstractMergedBamFile

    int identifier

    String description

    static belongsTo = [
            abstractMergedBamFile: AbstractMergedBamFile,
    ]

    static constraints = {
        identifier(unique: 'abstractMergedBamFile', validator: { int val, QualityAssessmentMergedPass obj ->
            return val == 0 || (ProcessedMergedBamFile.isAssignableFrom(Hibernate.getClass(obj.abstractMergedBamFile)))
        })
        description(nullable: true)
    }

    @Override
    String toString() {
        return "QAMP ${id}: pass ${identifier} " + (latestPass ? "(latest) " : "") + "on ${abstractMergedBamFile}"
    }

    /**
     * @return Whether this is the most recent QA pass on the referenced {@link AbstractMergedBamFile}.
     */
    boolean isLatestPass() {
        return identifier == maxIdentifier(abstractMergedBamFile)
    }

    static Integer maxIdentifier(final AbstractMergedBamFile abstractMergedBamFile) {
        assert abstractMergedBamFile
        return QualityAssessmentMergedPass.createCriteria().get {
            eq("abstractMergedBamFile", abstractMergedBamFile)
            projections {
                max("identifier")
            }
        }
    }

    static int nextIdentifier(final AbstractMergedBamFile abstractMergedBamFile) {
        assert abstractMergedBamFile
        final Integer maxIdentifier = maxIdentifier(abstractMergedBamFile)
        if (maxIdentifier == null) {
            return 0
        }
        return maxIdentifier + 1
    }

    static mapping = {
        abstractMergedBamFile index: "quality_assessment_merged_pass_abstract_merged_bam_file_idx"
    }

    @Override
    Project getProject() {
        return abstractMergedBamFile.project
    }

    @Override
    Individual getIndividual() {
        return abstractMergedBamFile.individual
    }

    Sample getSample() {
        return abstractMergedBamFile.sample
    }

    SampleType getSampleType() {
        return abstractMergedBamFile.sampleType
    }

    MergingSet getMergingSet() {
        if (ProcessedMergedBamFile.isAssignableFrom(Hibernate.getClass(abstractMergedBamFile))) {
            return abstractMergedBamFile.mergingSet
        }
        throw new NotSupportedException("MergingSet exists only for ProcessedMergedBamFiles")
    }

    MergingWorkPackage getMergingWorkPackage() {
        return abstractMergedBamFile.mergingWorkPackage
    }

    MergingPass getMergingPass() {
        if (ProcessedMergedBamFile.isAssignableFrom(Hibernate.getClass(abstractMergedBamFile))) {
            return abstractMergedBamFile.mergingPass
        }
        throw new NotSupportedException("MergingPass exists only for ProcessedMergedBamFiles")
    }

    @Override
    SeqType getSeqType() {
        return abstractMergedBamFile.seqType
    }

    @Override
    Set<SeqTrack> getContainedSeqTracks() {
        return abstractMergedBamFile.containedSeqTracks
    }

    ReferenceGenome getReferenceGenome() {
        return abstractMergedBamFile.referenceGenome
    }
}
