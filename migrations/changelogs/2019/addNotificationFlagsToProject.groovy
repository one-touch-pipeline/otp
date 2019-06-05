databaseChangeLog = {

    changeSet(author: "borufka (generated)", id: "1559743795865-1") {
        addColumn(tableName: "project") {
            column(name: "processing_notification", type: "boolean", value: "true")
        }
        addNotNullConstraint(columnDataType: "boolean", columnName: "processing_notification", tableName: "project")
    }

    changeSet(author: "borufka (generated)", id: "1559743795865-2") {
        addColumn(tableName: "project") {
            column(name: "qc_traffic_light_notification", type: "boolean", value: "true")
        }
        addNotNullConstraint(columnDataType: "boolean", columnName: "qc_traffic_light_notification", tableName: "project")
    }
}
