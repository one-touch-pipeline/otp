databaseChangeLog = {

    changeSet(author: "", id: "1661340700209-171") {
        createIndex(indexName: "seq_track_ilse_submission_idx", tableName: "seq_track") {
            column(name: "ilse_submission_id")
        }
    }
}
