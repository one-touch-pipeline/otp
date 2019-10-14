databaseChangeLog = {

    changeSet(author: "borufka (generated)", id: "1569931306344-3") {
        createIndex(indexName: "bam_file_submission_object_bam_file_idx", tableName: "bam_file_submission_object") {
            column(name: "bam_file_id")
        }
    }

    changeSet(author: "borufka (generated)", id: "1569931306344-4") {
        createIndex(indexName: "bam_file_submission_object_sample_submission_object_idx", tableName: "bam_file_submission_object") {
            column(name: "sample_submission_object_id")
        }
    }

    changeSet(author: "borufka (generated)", id: "1569931306344-5") {
        createIndex(indexName: "data_file_submission_object_data_file_idx", tableName: "data_file_submission_object") {
            column(name: "data_file_id")
        }
    }

    changeSet(author: "borufka (generated)", id: "1569931306344-6") {
        createIndex(indexName: "data_file_submission_object_sample_submission_object_idx", tableName: "data_file_submission_object") {
            column(name: "sample_submission_object_id")
        }
    }

    changeSet(author: "borufka (generated)", id: "1569931306344-7") {
        createIndex(indexName: "ega_submission_project_idx", tableName: "ega_submission") {
            column(name: "project_id")
        }
    }

    changeSet(author: "borufka (generated)", id: "1569931306344-8") {
        createIndex(indexName: "sample_submission_sample_idx", tableName: "sample_submission_object") {
            column(name: "sample_id")
        }
    }

    changeSet(author: "borufka (generated)", id: "1569931306344-9") {
        createIndex(indexName: "sample_submission_seq_type_idx", tableName: "sample_submission_object") {
            column(name: "seq_type_id")
        }
    }

    changeSet(author: "borufka (generated)", id: "1569931306344-10") {
        createIndex(indexName: "ega_submission_sample_submission_object_ega_submission_samples_to_submit_idx", tableName: "ega_submission_sample_submission_object") {
            column(name: "ega_submission_samples_to_submit_id")
        }
    }
    changeSet(author: "borufka (generated)", id: "1569931306344-11") {
        createIndex(indexName: "ega_submission_sample_submission_object_sample_submission_object_idx", tableName: "ega_submission_sample_submission_object") {
            column(name: "sample_submission_object_id")
        }
    }
    changeSet(author: "borufka (generated)", id: "1569931306344-12") {
        createIndex(indexName: "ega_submission_data_file_submission_object_ega_submission_data_files_to_submit_idx", tableName: "ega_submission_data_file_submission_object") {
            column(name: "ega_submission_data_files_to_submit_id")
        }
    }
    changeSet(author: "borufka (generated)", id: "1569931306344-13") {
        createIndex(indexName: "ega_submission_data_file_submission_object_data_file_submission_object_idx", tableName: "ega_submission_data_file_submission_object") {
            column(name: "data_file_submission_object_id")
        }
    }
    changeSet(author: "borufka (generated)", id: "1569931306344-14") {
        createIndex(indexName: "ega_submission_bam_file_submission_object_ega_submission_bam_files_to_submit_idx", tableName: "ega_submission_bam_file_submission_object") {
            column(name: "ega_submission_bam_files_to_submit_id")
        }
    }
    changeSet(author: "borufka (generated)", id: "1569931306344-15") {
        createIndex(indexName: "ega_submission_bam_file_submission_object_bam_file_submission_object_idx", tableName: "ega_submission_bam_file_submission_object") {
            column(name: "bam_file_submission_object_id")
        }
    }
}
