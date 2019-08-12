databaseChangeLog = {

    changeSet(author: "borufka (generated)", id: "1565009981983-1") {
        addColumn(tableName: "project_info") {
            column(name: "comment", type: "text")
        }
    }

    changeSet(author: "borufka (generated)", id: "1565009981983-2") {
        addColumn(tableName: "project_info") {
            column(name: "deletion_date", type: "timestamp")
        }
    }

    changeSet(author: "borufka (generated)", id: "1565009981983-3") {
        addColumn(tableName: "project_info") {
            column(name: "recipient_account", type: "varchar(255)")
        }
    }

    changeSet(author: "borufka (generated)", id: "1565009981983-4") {
        addColumn(tableName: "project_info") {
            column(name: "recipient_institution", type: "varchar(255)")
        }
    }

    changeSet(author: "borufka (generated)", id: "1565009981983-5") {
        addColumn(tableName: "project_info") {
            column(name: "recipient_person", type: "varchar(255)")
        }
    }

    changeSet(author: "borufka (generated)", id: "1565009981983-6") {
        addColumn(tableName: "project_info") {
            column(name: "requester", type: "varchar(255)")
        }
    }

    changeSet(author: "borufka (generated)", id: "1565009981983-7") {
        addColumn(tableName: "project_info") {
            column(name: "ticketid", type: "varchar(255)")
        }
    }

    changeSet(author: "borufka (generated)", id: "1565009981983-12") {
        dropForeignKeyConstraint(baseTableName: "project_info", constraintName: "project_info_commissioning_user_id_fkey")
    }

    changeSet(author: "borufka (generated)", id: "1565009981983-40") {
        dropColumn(columnName: "commissioning_user_id", tableName: "project_info")
    }

    changeSet(author: "borufka (generated)", id: "1565009981983-41") {
        dropColumn(columnName: "recipient", tableName: "project_info")
    }

    changeSet(author: "borufka (generated)", id: "1565274903649-1") {
        addColumn(tableName: "project_info") {
            column(name: "dta_id", type: "varchar(255)")
        }
    }
}
