databaseChangeLog = {
    changeSet(author: "m139l", id: "1575881251281-3") {
        addUniqueConstraint(columnNames: "type, program_version, program_name", constraintName: "UK79e9db6b093294dfe0036ab264a4", tableName: "software_tool")
    }

    changeSet(author: "m139l", id: "1575879903329-50") {
        addNotNullConstraint(columnDataType: "varchar(255)", columnName: "program_version", tableName: "software_tool")
    }
}