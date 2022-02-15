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
package de.dkfz.tbi.otp.ngsdata

import grails.gorm.hibernate.annotation.ManagedEntity

import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.Entity
import de.dkfz.tbi.otp.workflowExecution.ReferenceGenomeSelector

/**
 * Represents connection between {@link Project}, {@link SeqType}
 * and {@link ReferenceGenome}
 * @deprecated class is part of the old workflow system, use {@link ReferenceGenomeSelector} instead
 */
@Deprecated
@ManagedEntity
class ReferenceGenomeProjectSeqType implements Entity {

    static final String TAB_FILE_PATTERN = /[0-9a-zA-Z-_\.]+\.tab/

    /**
     * Is used to track information about
     * reference genomes used in a project for a seqType.
     * If it is null, this reference genome is used for these
     * project and seqType.
     * If it is not null, it is date when usage
     * of this reference genome for these project and seqType
     * has been deprecated.
     */
    @Deprecated
    Date deprecatedDate = null

    /**
     * File name of file holding the chromosome stat size.
     * The file ends with '.tab' and is located in the stat subdirectory of the reference genome.
     * The value has to be set for alignment with Roddy and has to be null for OTP alignment.
     */
    @Deprecated
    String statSizeFileName

    @Deprecated
    Project project

    @Deprecated
    SeqType seqType

    @Deprecated
    SampleType sampleType

    @Deprecated
    ReferenceGenome referenceGenome

    @Deprecated
    Set<ReferenceGenomeProjectSeqTypeAlignmentProperty> alignmentProperties

    static hasMany = [
            alignmentProperties: ReferenceGenomeProjectSeqTypeAlignmentProperty,
    ]

    static belongsTo = [
            project   : Project,
            seqType   : SeqType,
            sampleType: SampleType,
    ]

    static constraints = {
        // there must be no 2 current (not deprecated) reference genomes
        // defined for the same combination of project and seqType and sampleType
        referenceGenome validator: { val, obj ->
            if (!obj.deprecatedDate) {
                List<ReferenceGenomeProjectSeqType> existingObjects = ReferenceGenomeProjectSeqType
                        .findAllByProjectAndSeqTypeAndSampleTypeAndDeprecatedDateIsNull(obj.project, obj.seqType, obj.sampleType)
                return existingObjects.isEmpty() || existingObjects.size() == 1 && existingObjects.contains(obj)
            }
        }
        sampleType(nullable: true)
        deprecatedDate(nullable: true)
        statSizeFileName nullable: true, blank: false, matches: TAB_FILE_PATTERN, shared: "pathComponent"
    }

    @Override
    @Deprecated
    String toString() {
        return "RGPST ${id}: [${deprecatedDate ? "deprecated ${deprecatedDate}" : "not deprecated"}] " +
                "(${project.name} ${seqType.name} ${seqType.libraryLayout} sampleType ${sampleType?.name}) -> " +
                "${referenceGenome.name} statSizeFileName ${statSizeFileName}"
    }

    static mapping = {
        project index: "reference_genome_project_seq_type_project_idx"
        seqType index: "reference_genome_project_seq_type_seq_type_idx"
        sampleType index: "reference_genome_project_seq_type_sample_type_idx"
        referenceGenome index: "reference_genome_project_seq_type_reference_genome_idx"
        alignmentProperties cascade: "all-delete-orphan"
    }
}
