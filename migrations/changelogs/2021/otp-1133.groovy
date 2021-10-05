databaseChangeLog = {

    changeSet(author: "", id: "1633426477098-3") {
        addUniqueConstraint(columnNames: "workflow_version, workflow_id", constraintName: "UKcb8bd959752a92ddf76fef03121d", tableName: "workflow_version")
    }
}
