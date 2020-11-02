databaseChangeLog = {

    changeSet(author: "", id: "1604072343163-1") {
        addColumn(tableName: "workflow_run") {
            column(name: "job_can_be_restarted", type: "boolean") {
                constraints(nullable: "false")
            }
        }
    }
}
