databaseChangeLog = {

    changeSet(author: "borufka (generated)", id: "1557759949602-1") {
        createIndex(indexName: "individual_pid_idx", tableName: "individual") {
            column(name: "pid")
        }
    }

    changeSet(author: "borufka (generated)", id: "1557759949602-3") {
        createIndex(indexName: "individual_mock_full_name_idx", tableName: "individual") {
            column(name: "mock_full_name")
        }
    }

    changeSet(author: "borufka (generated)", id: "1557759949602-4") {
        createIndex(indexName: "individual_mock_pid_idx", tableName: "individual") {
            column(name: "mock_pid")
        }
    }

    changeSet(author: "borufka (generated)", id: "1557759949602-5") {
        createIndex(indexName: "sample_individual_idx", tableName: "sample") {
            column(name: "individual_id")
        }
    }

    changeSet(author: "borufka (generated)", id: "1557759949602-6") {
        createIndex(indexName: "project_name_idx", tableName: "project") {
            column(name: "name")
        }
    }

    changeSet(author: "borufka (generated)", id: "1557759949602-7") {
        createIndex(indexName: "sample_type_name_idx", tableName: "sample_type") {
            column(name: "name")
        }
    }
}
