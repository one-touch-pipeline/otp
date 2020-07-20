databaseChangeLog = {

    changeSet(author: "jpa (generated)", id: "1591368785203-4") {
        addUniqueConstraint(columnNames: "user_id, project_id", constraintName: "UK324f84fe9e2682e71201316081b9", tableName: "user_project_role")
    }

    changeSet(author: "jpa (generated)", id: "1591368785203-9") {
        dropForeignKeyConstraint(baseTableName: "user_project_role", constraintName: "project_role_fk")
    }

    changeSet(author: "jpa (generated)", id: "1591368785203-40") {
        dropColumn(columnName: "project_role_id", tableName: "user_project_role")
    }
}
