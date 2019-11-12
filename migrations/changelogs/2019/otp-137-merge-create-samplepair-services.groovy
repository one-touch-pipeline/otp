databaseChangeLog = {

    changeSet(author: "borufka (generated)", id: "1572871703568-15") {
        dropUniqueConstraint(constraintName: "uc_sample_pairrelative_path_col", tableName: "sample_pair")
    }

    changeSet(author: "borufka (generated)", id: "1572871703568-33") {
        dropColumn(columnName: "relative_path", tableName: "sample_pair")
    }

    changeSet(author: "borufka (generated)", id: "1572875134305-3") {
        createIndex(indexName: "sample_pair_runyapsa_idx1", tableName: "sample_pair") {
            column(name: "run_yapsa_processing_status")
            column(name: "merging_work_package1_id")
            column(name: "merging_work_package2_id")
        }
    }

    changeSet(author: "borufka (generated)", id: "1572965648500-3") {
        createIndex(indexName: "sample_type_per_project__category_idx", tableName: "sample_type_per_project") {
            column(name: "category")
        }
    }

    changeSet(author: "borufka (generated)", id: "1572965648500-174") {
        dropIndex(indexName: "project_idx", tableName: "sample_type_per_project")

        createIndex(indexName: "sample_type_per_project__project__sample_type__category_idx", tableName: "sample_type_per_project") {
            column(name: "project_id")
            column(name: "sample_type_id")
            column(name: "category")
        }
    }

    changeSet(author: "borufka (generated)", id: "1572965648500-175") {
        dropIndex(indexName: "sample_type_idx", tableName: "sample_type_per_project")

        createIndex(indexName: "sample_type_per_project__sample_type_idx", tableName: "sample_type_per_project") {
            column(name: "sample_type_id")
        }
    }
}
