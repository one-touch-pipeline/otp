databaseChangeLog = {
    changeSet(author: "", id: "1589990553223-2") {
        addColumn(tableName: "workflow_artefact") {
            column(name: "output_role", type: "varchar(255)")
        }
    }

    changeSet(author: "", id: "1589990553223-16") {
        dropForeignKeyConstraint(baseTableName: "workflow_run_output_artefacts", constraintName: "workflow_run_output_artefacts_output_artefacts_id_fkey")
    }

    changeSet(author: "", id: "1589990553223-17") {
        dropForeignKeyConstraint(baseTableName: "workflow_run_output_artefacts", constraintName: "workflow_run_output_artefacts_produced_by_id_fkey")
    }

    changeSet(author: "", id: "1589990553223-24") {
        dropTable(tableName: "workflow_run_output_artefacts")
    }
}
