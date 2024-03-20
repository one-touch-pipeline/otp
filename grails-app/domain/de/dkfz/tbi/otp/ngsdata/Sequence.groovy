/*
 * Copyright 2011-2024 The OTP authors
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

import de.dkfz.tbi.otp.ngsdata.taxonomy.*
import de.dkfz.tbi.otp.project.Project

/**
 * This domain class represents a database view called "sequences".
 *
 * It is mostly a join of the fields from:
 * <ul>
 * <li>SeqTrack</li>
 * <li>SeqType</li>
 * <li>SeqPlatform</li>
 * <li>SeqPlatformModelLabel</li>
 * <li>SequencingKitLabel</li>
 * <li>Sample</li>
 * <li>Run</li>
 * <li>PipelineVersion</li>
 * <li>SeqCenter</li>
 * <li>SampleType</li>
 * <li>Individual</li>
 * <li>Project</li>
 * <li>SpeciesCommonName</li>
 * <li>Species</li>
 * <li>Strain</li>
 * <li>SpeciesWithStrain</li>
 * </ul>
 */
@ManagedEntity
class Sequence implements Serializable {
    /** IDs (primary keys) */
    long seqTrackId
    long seqTypeId
    long seqPlatformId
    Long seqPlatformModelLabelId
    Long sequencingKitLabelId
    long sampleId
    long runId
    long pipelineVersionId
    long seqCenterId
    long sampleTypeId
    long individualId
    long projectId

    /** fields from {@link SeqTrack} */
    String laneId
    String libraryName
    boolean hasOriginalBam
    Long nBasePairs
    int insertSize
    SeqTrack.QualityEncoding qualityEncoding
    SeqTrack.DataProcessingState fastqcState
    SeqTrack.Problem problem
    String singleCellWellLabel
    String sampleName

    /** fields from {@link IlseSubmission} */
    /** actually ilseNumber */
    Integer ilseId

    /** fields from {@link Run} */
    /** actually run name */
    String name
    Date dateExecuted
    Date dateCreated
    boolean blacklisted

    /** fields from {@link SeqPlatform} */
    String seqPlatformName

    /** fields from {@link SeqPlatformModelLabel} */
    String seqPlatformModelLabelName

    /** fields from {@link SequencingKitLabel} */
    String sequencingKitLabelName

    /** fields from {@link SeqType} */
    String seqTypeName
    String seqTypeDisplayName
    boolean singleCell
    String libraryLayout
    String dirName

    /** fields from {@link SampleType} */
    String sampleTypeName

    /** fields from {@link Individual} */
    String pid
    Individual.Type type

    /** fields from {@link SpeciesCommonName} */
    String speciesCommonName

    /** fields from {@link Species} */
    String scientificName

    /** fields from {@link Strain} */
    String strain

    /** fields from {@link Project} */
    String projectName
    String projectDirName
    boolean fileArchived

    /** fields from {@link SeqCenter} */
    String seqCenterName
    String seqCenterDirName

    /** fields from {@link LibraryPreparationKit} */
    String libraryPreparationKit

    /** fields from {@link AntibodyTarget} */
    String antibodyTarget

    /** fields from {@link RawSequenceFile} */
    boolean fileExists
    boolean fileWithdrawn

    // derived property for calculating the creation date without time
    Date dayCreated

    /** contains accumulated and compound information of {@link Sample#mixedInSpecies} */
    String mixedInSpecies

    static mapping = {
        table 'sequences'
        version false
        cache 'read-only'
        seqTrackId column: 'seq_track_id'
        seqTypeId column: 'seq_type_id'
        seqPlatformId column: 'seq_platform_id'
        seqPlatformModelLabelId column: 'seq_platform_model_label_id'
        sequencingKitLabelId column: 'sequencing_kit_label_id'
        sampleId column: 'sample_id'
        runId column: 'run_id'
        pipelineVersionId column: 'pipeline_version_id'
        seqCenterId column: 'seq_center_id'
        sampleTypeId column: 'sample_type_id'
        individualId column: 'individual_id'
        projectId column: 'project_id'
        ilseId column: 'ilse_id'
        libraryPreparationKit column: 'library_preparation_kit'
        id composite: [
                'seqTrackId',
                'seqTypeId',
                'seqPlatformId',
                'sampleId',
                'runId',
                'pipelineVersionId',
                'seqCenterId',
                'sampleTypeId',
                'individualId',
                'projectId',
        ]
        dayCreated formula: 'DATE(date_created)'
    }

    static constraints = {
        // nullable constraints from Run
        dateExecuted(nullable: true)
        // nullable constraints from SeqPlatform
        seqPlatformModelLabelId(nullable: true)
        sequencingKitLabelId(nullable: true)
        // nullable constraints from SeqPlatformModelLabel
        seqPlatformModelLabelName(nullable: true)
        // nullable constraints from SequencingKitLabel
        sequencingKitLabelName(nullable: true)
        nBasePairs(nullable: true)
        problem nullable: true
        libraryPreparationKit nullable: true
        speciesCommonName nullable: true
        scientificName nullable: true
        strain nullable: true
        mixedInSpecies nullable: true
    }

    String getLibraryLayoutDirName() {
        return libraryLayout.toLowerCase()
    }
}
