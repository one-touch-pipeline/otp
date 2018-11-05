package de.dkfz.tbi.otp.egaSubmission

import de.dkfz.tbi.otp.ngsdata.Project
import de.dkfz.tbi.otp.utils.Entity

class Submission implements Entity {

    String egaBox
    String submissionName
    String studyName
    StudyType studyType
    String studyAbstract
    String pubMedId
    State state
    SelectionState selectionState

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
