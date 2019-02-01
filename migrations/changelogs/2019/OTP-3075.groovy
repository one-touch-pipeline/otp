databaseChangeLog = {

    changeSet(author: "borufka (generated)", id: "1550484372005-1") {
        addColumn(tableName: "cluster_job") {
            column(name: "check_status", type: "varchar(255)") {
                constraints(nullable: "true")
            }
        }
    }
}
