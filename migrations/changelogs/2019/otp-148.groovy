databaseChangeLog = {

    changeSet(author: "kosnac (generated)", id: "1576741624784-1") {
        addColumn(tableName: "users") {
            column(name: "planned_deactivation_date", type: "timestamp")
        }
    }
}
