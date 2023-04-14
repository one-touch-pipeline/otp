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

import de.dkfz.tbi.otp.CommentableWithProject
import de.dkfz.tbi.otp.ngsdata.*
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.Entity

import static de.dkfz.tbi.otp.utils.CollectionUtils.exactlyOneElement

@ManagedEntity
abstract class AbstractBamFile implements CommentableWithProject, Entity {

    enum BamType {
        SORTED,
        MDUP,
        RMDUP
    }

    enum QaProcessingStatus {
        UNKNOWN,
        NOT_STARTED,
        IN_PROGRESS,
        FINISHED
    }

    BamType type = null
    boolean hasIndexFile = false
    boolean hasCoveragePlot = false
    boolean hasInsertSizePlot = false
    boolean hasMetricsFile = false
    boolean withdrawn = false

    /**
     * Coverage without N of the BamFile
     */
    // Has to be from Type Double so that it can be nullable
    Double coverage

    /**
     * Coverage with N of the BAM file.
     * In case of sequencing types that need a BED file this value stays 'null' since there is no differentiation between 'with N' and 'without N'.
     */
    Double coverageWithN

    QaProcessingStatus qualityAssessmentStatus = QaProcessingStatus.UNKNOWN

    abstract AbstractMergingWorkPackage getMergingWorkPackage()
    abstract Set<SeqTrack> getContainedSeqTracks()
    abstract AbstractQualityAssessment getQualityAssessment()

    static constraints = {
        // Type is not nullable for BamFiles except RoddyBamFile,
        // Grails does not create the SQL schema correctly when using simple nullable constraints,
        // therefore this workaround with validator constraints is used
        type nullable: true, validator: { val, obj ->
            // validator doesn't work correctly with subclasses
            return val in obj.allowedTypes
        }
        hasMetricsFile validator: { val, obj ->
            if (obj.type == BamType.SORTED) {
                return !val
            }
            return true
        }
        coverage(nullable: true)
        coverageWithN(nullable: true)
        comment nullable: true
    }

    List<BamType> getAllowedTypes() {
        return [null]
    }

    static mapping = {
        'class' index: "abstract_bam_file_class_idx"
        withdrawn index: "abstract_bam_file_withdrawn_idx"
        qualityAssessmentStatus index: "abstract_bam_file_quality_assessment_status_idx"
        comment cascade: "all-delete-orphan"
    }

    boolean isQualityAssessed() {
        qualityAssessmentStatus == QaProcessingStatus.FINISHED
    }

    BedFile getBedFile() {
        assert seqType.needsBedFile : "A BED file is only available when needed."

        List<BedFile> bedFiles = BedFile.findAllWhere(
                referenceGenome: referenceGenome,
                libraryPreparationKit: mergingWorkPackage.libraryPreparationKit
        )
        return exactlyOneElement(bedFiles, "Wrong BedFile count, found: ${bedFiles}")
    }

    @Override
    Project getProject() {
        return individual?.project
    }

    Individual getIndividual() {
        return sample?.individual
    }

    Sample getSample() {
        return mergingWorkPackage?.sample
    }

    SampleType getSampleType() {
        return  sample?.sampleType
    }

    SeqType getSeqType() {
        return mergingWorkPackage?.seqType
    }

    Pipeline getPipeline() {
        return mergingWorkPackage?.pipeline
    }

    /**
     * @return The reference genome which was used to produce this BAM file.
     */
    ReferenceGenome getReferenceGenome() {
        return mergingWorkPackage?.referenceGenome
    }

    void withdraw() {
        withTransaction {
            withdrawn = true
            assert AbstractBamFileService.saveBamFile(this)
        }
    }
}
