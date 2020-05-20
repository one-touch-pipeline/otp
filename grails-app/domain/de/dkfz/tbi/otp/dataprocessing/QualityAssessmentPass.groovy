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

import de.dkfz.tbi.otp.job.processing.ProcessParameterObject
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.Entity

/**
 * Each execution of the Quality Assessment Workflow on the particular data file is represented as QualityAssessmentPass.
 */
class QualityAssessmentPass implements ProcessParameterObject, Entity {

    int identifier
    String description
    ProcessedBamFile processedBamFile

    static belongsTo = [
            processedBamFile: ProcessedBamFile,
    ]

    static constraints = {
        identifier(unique: 'processedBamFile')
        description(nullable: true)
    }

    @Override
    String toString() {
        return "id: ${processedBamFile.id} " +
                "pass: ${identifier} " + (latestPass ? "(latest) " : "") +
                "alignmentPass: ${processedBamFile.alignmentPass.identifier} " +
                (processedBamFile.alignmentPass.latestPass ? "(latest) " : "") +
                "<br>sample: ${processedBamFile.sample} " +
                "seqType: ${processedBamFile.seqType} " +
                "<br>project: ${processedBamFile.project}"
    }

    /**
     * @return Whether this is the most recent QA pass on the referenced {@link ProcessedBamFile}.
     */
    boolean isLatestPass() {
        return identifier == maxIdentifier(processedBamFile)
    }

    static Integer maxIdentifier(final ProcessedBamFile processedBamFile) {
        assert processedBamFile
        return QualityAssessmentPass.createCriteria().get {
            eq("processedBamFile", processedBamFile)
            projections {
                max("identifier")
            }
        }
    }

    static int nextIdentifier(final ProcessedBamFile processedBamFile) {
        assert processedBamFile
        final Integer maxIdentifier = maxIdentifier(processedBamFile)
        if (maxIdentifier == null) {
            return 0
        } else {
            return maxIdentifier + 1
        }
    }

    AlignmentPass getAlignmentPass() {
        return processedBamFile.alignmentPass
    }

    @Override
    Project getProject() {
        return processedBamFile.project
    }

    SeqTrack getSeqTrack() {
        return processedBamFile.seqTrack
    }

    Sample getSample() {
        return processedBamFile.sample
    }

    @Override
    SeqType getSeqType() {
        return processedBamFile.seqType
    }

    @Override
    Individual getIndividual() {
        return processedBamFile.individual
    }

    ReferenceGenome getReferenceGenome() {
        return processedBamFile.referenceGenome
    }

    @Override
    Set<SeqTrack> getContainedSeqTracks() {
        return processedBamFile.containedSeqTracks
    }

    static mapping = {
        processedBamFile index: "quality_assessment_pass_processed_bam_file_idx"
    }
}
