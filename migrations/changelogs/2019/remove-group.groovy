databaseChangeLog = {
    changeSet(author: "", id: "1573478252553-7") {
        dropForeignKeyConstraint(baseTableName: "groups", constraintName: "fkb63dd9d4a4e7b28c")
    }

    changeSet(author: "", id: "1573478252553-15") {
        dropUniqueConstraint(constraintName: "groups_name_key", tableName: "groups")
    }

    changeSet(author: "", id: "1573478252553-16") {
        dropUniqueConstraint(constraintName: "groups_role_id_key", tableName: "groups")
    }

    changeSet(author: "", id: "1573478252553-21") {
        dropTable(tableName: "groups")
    }
}
