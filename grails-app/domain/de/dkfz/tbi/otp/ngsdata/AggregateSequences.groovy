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

import de.dkfz.tbi.otp.project.Project

/**
 * This domain class represents a database view called "aggregate_sequences".
 */
class AggregateSequences implements Serializable {
    /** IDs (primary keys) */
    long seqTypeId
    long seqPlatformId
    Long seqPlatformModelLabelId
    Long sequencingKitLabelId
    long sampleId
    long seqCenterId
    long sampleTypeId
    long individualId
    long projectId

    /** aggregated fields from {@link SeqTrack} */
    long laneCount

    @SuppressWarnings("PropertyName")
    Long sum_N_BasePairs

    @SuppressWarnings("PropertyName")
    Long sum_N_BasePairsGb

    /** fields from {@link SeqPlatform} */
    String seqPlatformName

    /** fields from {@link SeqPlatformModelLabel} */
    String seqPlatformModelLabelName

    /** fields from {@link SequencingKitLabel} */
    String sequencingKitLabelName

    /** fields from {@link SeqType} */
    String seqTypeName
    String seqTypeDisplayName
    SequencingReadType libraryLayout
    String dirName
    boolean singleCell

    /** fields from {@link SampleType} */
    String sampleTypeName

    /** fields from {@link Individual} */
    String pid
    Individual.Type type

    /** fields from {@link Project} */
    String projectName
    String projectDirName

    /** fields from {@link SeqCenter} */
    String seqCenterName
    String seqCenterDirName

    static mapping = {
        table 'aggregate_sequences'
        version false
        cache 'read-only'
        seqTypeId column: 'seq_type_id'
        seqPlatformId column: 'seq_platform_id'
        seqPlatformModelLabelId column: 'seq_platform_model_label_id'
        sequencingKitLabelId column: 'sequencing_kit_label_id'
        sampleId column: 'sample_id'
        seqCenterId column: 'seq_center_id'
        sampleTypeId column: 'sample_type_id'
        individualId column: 'individual_id'
        projectId column: 'project_id'
        id composite: ['seqTypeId', 'seqPlatformId', 'sampleId', 'seqCenterId', 'sampleTypeId', 'individualId', 'projectId']
    }

    // needed for tests
    static constraints = {
        // nullable constraints from SeqPlatform
        seqPlatformModelLabelId(nullable: true)
        sequencingKitLabelId(nullable: true)
        // nullable constraints from SeqPlatformModelLabel
        seqPlatformModelLabelName(nullable: true)
        // nullable constraints from SequencingKitLabel
        sequencingKitLabelName(nullable: true)
    }
}
