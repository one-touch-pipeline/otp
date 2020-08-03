databaseChangeLog = {

    changeSet(author: "", id: "1596450991771-1") {
        addColumn(tableName: "workflow_artefact") {
            column(name: "display_name", type: "varchar(255)") {
                constraints(nullable: "false")
            }
        }
    }
    changeSet(author: "", id: "1596450991771-2") {
        addColumn(tableName: "workflow_run") {
            column(name: "display_name", type: "varchar(255)") {
                constraints(nullable: "false")
            }
        }
    }
}
