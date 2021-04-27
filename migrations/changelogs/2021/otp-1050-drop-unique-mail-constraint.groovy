databaseChangeLog = {

    changeSet(author: "", id: "1618584587545-21") {
        dropUniqueConstraint(constraintName: "users_email_key", tableName: "users")
    }
}
