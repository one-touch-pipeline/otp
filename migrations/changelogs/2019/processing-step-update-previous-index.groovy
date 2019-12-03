databaseChangeLog = {
    changeSet(author: "kosnac (generated)", id: "1575297962444-3") {
        createIndex(indexName: "processing_step_update_previous_idx", tableName: "processing_step_update") {
            column(name: "previous_id")
        }
    }
}
