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

import de.dkfz.tbi.otp.dataprocessing.OtpPath
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.utils.Entity

/**
 * Represents connection between {@link Project}, {@link SeqType}
 * and {@link ReferenceGenome}
 */
class ReferenceGenomeProjectSeqType implements Entity {

    static final String TAB_FILE_PATTERN = /[0-9a-zA-Z-_\.]+\.tab/

    /**
     * Date when the object has been created.
     * To be filled in automatically by GORM.
     */
    Date dateCreated
    /**
     * Is used to track information about
     * reference genomes used in a project for a seqType.
     * If it is null, this reference genome is used for these
     * project and seqType.
     * If it is not null, it is date when usage
     * of this reference genome for these project and seqType
     * has been deprecated.
     */
    Date deprecatedDate = null

    /**
     * File name of file holding the chromosome stat size.
     * The file ends with '.tab' and is located in the stat subdirectory of the reference genome.
     * The value has to be set for alignment with Roddy and has to be null for OTP alignment.
     */
    String statSizeFileName

    Project project
    SeqType seqType
    SampleType sampleType

    ReferenceGenome referenceGenome

    Set<ReferenceGenomeProjectSeqTypeAlignmentProperty> alignmentProperties

    static hasMany = [
            alignmentProperties: ReferenceGenomeProjectSeqTypeAlignmentProperty,
    ]

    static belongsTo = [
            project: Project,
            seqType: SeqType,
            sampleType: SampleType,
    ]

    static constraints = {
        // there must be no 2 current (not deprecated) reference genomes
        // defined for the same combination of project and seqType and sampleType
        referenceGenome validator: { val, obj ->
            if (!obj.deprecatedDate) {
                List<ReferenceGenomeProjectSeqType> existingObjects = ReferenceGenomeProjectSeqType
                            .findAllByProjectAndSeqTypeAndSampleTypeAndDeprecatedDateIsNull(obj.project, obj.seqType, obj.sampleType)
                if (existingObjects.isEmpty()) {
                    return true
                } else if ((existingObjects.size() == 1) && (existingObjects.contains(obj))) {
                    return true
                } else {
                    return false
                }
            }
        }
        sampleType(nullable: true)
        deprecatedDate(nullable: true)
        statSizeFileName nullable: true, blank: false, matches: TAB_FILE_PATTERN, validator: { it == null || OtpPath.isValidPathComponent(it) }
    }

    @Override
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


    static ReferenceGenomeProjectSeqType getConfiguredReferenceGenomeProjectSeqType(SeqTrack seqTrack) {
        assert seqTrack
        getConfiguredReferenceGenomeProjectSeqType(seqTrack.project, seqTrack.seqType, seqTrack.sampleType)
    }

    static ReferenceGenomeProjectSeqType getConfiguredReferenceGenomeProjectSeqType(Project project, SeqType seqType) {
        return CollectionUtils.atMostOneElement(
                ReferenceGenomeProjectSeqType.findAllByProjectAndSeqTypeAndSampleTypeIsNullAndDeprecatedDateIsNull(project, seqType)
        )
    }

    static ReferenceGenomeProjectSeqType getConfiguredReferenceGenomeProjectSeqType(Project project, SeqType seqType, SampleType sampleType) {
        assert project
        assert seqType
        assert sampleType
        switch (sampleType.specificReferenceGenome) {
            case SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT:
                return getConfiguredReferenceGenomeProjectSeqTypeUsingProjectDefault(project, seqType, sampleType)
            case SampleType.SpecificReferenceGenome.USE_SAMPLE_TYPE_SPECIFIC:
                return getConfiguredReferenceGenomeProjectSeqTypeUsingSampleTypeSpecific(project, seqType, sampleType)
            case SampleType.SpecificReferenceGenome.UNKNOWN:
                throw new RuntimeException("For sample type '${sampleType} the way to fetch the reference genome is not defined.")
            default:
                throw new RuntimeException("The value ${sampleType.specificReferenceGenome} for specific reference genome is not known")
        }
    }

    static private ReferenceGenomeProjectSeqType getConfiguredReferenceGenomeProjectSeqTypeUsingProjectDefault(
            Project project, SeqType seqType, SampleType sampleType) {
        assert SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT == sampleType.specificReferenceGenome
        try {
            return CollectionUtils.atMostOneElement(
                    ReferenceGenomeProjectSeqType.findAllByProjectAndSeqTypeAndSampleTypeIsNullAndDeprecatedDateIsNull(project, seqType)
            )
        } catch (AssertionError e) {
            throw new RuntimeException("Could not find a reference genome for project '${project}' and '${seqType}'", e)
        }
    }

    static private ReferenceGenomeProjectSeqType getConfiguredReferenceGenomeProjectSeqTypeUsingSampleTypeSpecific(
            Project project, SeqType seqType, SampleType sampleType) {
        assert SampleType.SpecificReferenceGenome.USE_SAMPLE_TYPE_SPECIFIC == sampleType.specificReferenceGenome
        try {
            return CollectionUtils.atMostOneElement(
                    ReferenceGenomeProjectSeqType.findAllByProjectAndSeqTypeAndSampleTypeAndDeprecatedDateIsNull(project, seqType, sampleType)
            )
        } catch (AssertionError e) {
            throw new RuntimeException("Could not find a reference genome for project '${project}' and '${seqType}' and '${sampleType}'", e)
        }
    }
}
