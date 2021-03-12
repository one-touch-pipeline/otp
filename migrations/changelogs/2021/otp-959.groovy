databaseChangeLog = {
    changeSet(author: "", id: "1615311081568-2") {
        addColumn(tableName: "workflow_artefact") {
            column(name: "artefact_type", type: "varchar(255)") {
                constraints(nullable: "false")
            }
        }
    }
}
