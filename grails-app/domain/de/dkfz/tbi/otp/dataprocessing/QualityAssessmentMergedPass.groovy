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

import grails.gorm.hibernate.annotation.ManagedEntity

import de.dkfz.tbi.otp.job.processing.ProcessParameterObject
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.Entity

@ManagedEntity
class QualityAssessmentMergedPass implements ProcessParameterObject, Entity {

    AbstractBamFile abstractBamFile

    String description

    static belongsTo = [
            abstractBamFile: AbstractBamFile,
    ]

    static constraints = {
        abstractBamFile(unique: true)
        description(nullable: true)
    }

    @Override
    String toString() {
        return "QAMP ${id}: on ${abstractBamFile}"
    }

    static mapping = {
        abstractBamFile index: "quality_assessment_merged_pass_abstract_merged_bam_file_idx"
    }

    @Override
    Project getProject() {
        return abstractBamFile.project
    }

    @Override
    Individual getIndividual() {
        return abstractBamFile.individual
    }

    Sample getSample() {
        return abstractBamFile.sample
    }

    SampleType getSampleType() {
        return abstractBamFile.sampleType
    }

    MergingWorkPackage getMergingWorkPackage() {
        return abstractBamFile.mergingWorkPackage
    }

    @Override
    SeqType getSeqType() {
        return abstractBamFile.seqType
    }

    @Override
    Set<SeqTrack> getContainedSeqTracks() {
        return abstractBamFile.containedSeqTracks
    }

    ReferenceGenome getReferenceGenome() {
        return abstractBamFile.referenceGenome
    }
}
