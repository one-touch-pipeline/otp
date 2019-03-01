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

package de.dkfz.tbi.otp.egaSubmission

import de.dkfz.tbi.otp.ngsdata.Project
import de.dkfz.tbi.otp.utils.Entity

class EgaSubmission implements Entity {

    String egaBox
    String submissionName
    String studyName
    StudyType studyType
    String studyAbstract
    String pubMedId
    State state
    SelectionState selectionState

    Set<SampleSubmissionObject> samplesToSubmit = [] as Set<SampleSubmissionObject>
    Set<BamFileSubmissionObject> bamFilesToSubmit = [] as Set<BamFileSubmissionObject>
    Set<DataFileSubmissionObject> dataFilesToSubmit = [] as Set<DataFileSubmissionObject>

    static belongsTo = [
            project: Project,
    ]

    static hasMany = [
            dataFilesToSubmit : DataFileSubmissionObject,
            bamFilesToSubmit  : BamFileSubmissionObject,
            samplesToSubmit   : SampleSubmissionObject,
    ]

    static constraints = {
        pubMedId nullable: true
        dataFilesToSubmit validator: { val, obj ->
            return (val || obj.state == State.SELECTION || !obj.bamFilesToSubmit?.isEmpty())
        }
        bamFilesToSubmit validator: { val, obj ->
            return (val || obj.state == State.SELECTION || !obj.dataFilesToSubmit?.isEmpty())
        }
        samplesToSubmit validator: { val, obj ->
            return (val || obj.state == State.SELECTION)
        }
    }

    static mapping = {
        studyAbstract type: 'text'
    }

    enum State {
        SELECTION,
        FILE_UPLOAD_STARTED,
        FILE_UPLOAD_FINISHED,
        METADATA_UPLOAD_FINISHED,
        PUBLISHED,
    }

    enum SelectionState {
        SELECT_SAMPLES,
        SAMPLE_INFORMATION,
        SELECT_FASTQ_FILES,
        SELECT_BAM_FILES,
    }

    enum StudyType {
        WHOLE_GENOME_SEQUENCING,
        METAGENOMICS,
        TRANSCRIPTOME,
        RESEQUENCING,
        EPIGENETICS,
        SYNTHETIC_GENOMICS,
        FORENSIC_OR_PALEO_GENOMICS,
        GENE_REGULATION_STUDY,
        CANCER_GENOMICS,
        POPULATION_GENOMICS,
        RNA_SEQ,
        EXOME_SEQUENCING,
        POOLED_CLONE_SEQUENCING,
        TRANSCRIPTOME_SEQUENCING,
        OTHER,
    }

}
