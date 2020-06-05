databaseChangeLog = {

    changeSet(author: "m139l (generated)", id: "1591619620683-1") {
        addColumn(tableName: "workflow_artefact") {
            column(name: "individual_id", type: "int8") {
                constraints(nullable: "false")
            }
        }
    }

    changeSet(author: "m139l (generated)", id: "1591619620683-2") {
        addColumn(tableName: "workflow_artefact") {
            column(name: "seq_type_id", type: "int8") {
                constraints(nullable: "false")
            }
        }
    }

    changeSet(author: "m139l (generated)", id: "1591694120353-5") {
        createIndex(indexName: "workflow_artefact_individual_idx", tableName: "workflow_artefact") {
            column(name: "individual_id")
        }
    }

    changeSet(author: "m139l (generated)", id: "1591694120353-6") {
        createIndex(indexName: "workflow_artefact_seq_type_idx", tableName: "workflow_artefact") {
            column(name: "seq_type_id")
        }
    }

    changeSet(author: "m139l (generated)", id: "1591619620683-5") {
        addForeignKeyConstraint(baseColumnNames: "seq_type_id", baseTableName: "workflow_artefact", constraintName: "FKcjpthxyd3d9mm1l5xarbq4r0m", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "seq_type")
    }

    changeSet(author: "m139l (generated)", id: "1591619620683-6") {
        addForeignKeyConstraint(baseColumnNames: "individual_id", baseTableName: "workflow_artefact", constraintName: "FKetm7rqu0oe7usgh7ffrosjsi7", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "individual")
    }
}
