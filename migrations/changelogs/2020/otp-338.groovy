databaseChangeLog = {

    changeSet(author: "", id: "1597598968643-1") {
        createTable(tableName: "project_request_project_request_user") {
            column(name: "project_request_users_id", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "project_request_user_id", type: "BIGINT") {
                constraints(nullable: "false")
            }
        }
    }

    changeSet(author: "", id: "1597598968643-2") {
        createTable(tableName: "project_request_user") {
            column(name: "id", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "user_id", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "access_to_files", type: "BOOLEAN") {
                constraints(nullable: "false")
            }

            column(name: "manage_users", type: "BOOLEAN") {
                constraints(nullable: "false")
            }

            column(name: "manage_users_and_delegate", type: "BOOLEAN") {
                constraints(nullable: "false")
            }

            column(name: "approval_state", type: "VARCHAR(255)") {
                constraints(nullable: "false")
            }

            column(name: "version", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "date_created", type: "TIMESTAMP WITHOUT TIME ZONE") {
                constraints(nullable: "false")
            }

            column(name: "last_updated", type: "TIMESTAMP WITHOUT TIME ZONE") {
                constraints(nullable: "false")
            }
        }
    }

    changeSet(author: "", id: "1597598968643-3") {
        createTable(tableName: "project_request_user_project_role") {
            column(name: "project_request_user_project_roles_id", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "project_role_id", type: "BIGINT") {
                constraints(nullable: "false")
            }
        }
    }

    changeSet(author: "", id: "1597598968643-4") {
        addPrimaryKey(columnNames: "id", constraintName: "project_request_userPK", tableName: "project_request_user")
    }

    changeSet(author: "", id: "1597598968643-7") {
        createIndex(indexName: "project_request_requester_idx", tableName: "project_request") {
            column(name: "requester_id")
        }
    }

    changeSet(author: "", id: "1598262540366-7") {
        createIndex(indexName: "project_request_keywords_idx", tableName: "project_request_keywords") {
            column(name: "project_request_id")
        }
    }

    changeSet(author: "", id: "1598262540366-8") {
        createIndex(indexName: "project_request_seqTypes_idx", tableName: "project_request_seq_type") {
            column(name: "project_request_seq_types_id")
        }
    }

    changeSet(author: "", id: "1598262540366-9") {
        createIndex(indexName: "project_request_user_project_roles_idx", tableName: "project_request_user_project_role") {
            column(name: "project_request_user_project_roles_id")
        }
    }

    changeSet(author: "", id: "1598262540366-10") {
        createIndex(indexName: "project_request_users_idx", tableName: "project_request_project_request_user") {
            column(name: "project_request_users_id")
        }
    }

    changeSet(author: "", id: "1598262540366-11") {
        addForeignKeyConstraint(baseColumnNames: "project_request_user_id", baseTableName: "project_request_project_request_user", constraintName: "FK494vryt8ugycu0d7ceryaqo5w", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "project_request_user")
    }

    changeSet(author: "", id: "1597598968643-8") {
        addForeignKeyConstraint(baseColumnNames: "project_role_id", baseTableName: "project_request_user_project_role", constraintName: "FKaoqfokobihuwwl8fifq3a9syn", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "project_role")
    }

    changeSet(author: "", id: "1597598968643-9") {
        addForeignKeyConstraint(baseColumnNames: "project_request_user_project_roles_id", baseTableName: "project_request_user_project_role", constraintName: "FKp311t51nd2umdgshf83nhwcdk", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "project_request_user")
    }

    changeSet(author: "", id: "1597598968643-10") {
        addForeignKeyConstraint(baseColumnNames: "project_request_users_id", baseTableName: "project_request_project_request_user", constraintName: "FKp6pmp49c8ksnty0kvx7aywdo", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "project_request")
    }

    changeSet(author: "", id: "1597598968643-11") {
        addForeignKeyConstraint(baseColumnNames: "user_id", baseTableName: "project_request_user", constraintName: "FKqsmw0ti8bgqvt5q102vgk2h7c", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "users")
    }

    changeSet(author: "", id: "1597598968643-13") {
        dropForeignKeyConstraint(baseTableName: "project_request", constraintName: "FKe5pt1po4745usehtg1gn6x989")
    }

    changeSet(author: "", id: "1597598968643-37") {
        dropTable(tableName: "project_request_bioinformatician")
    }

    changeSet(author: "", id: "1597598968643-38") {
        dropTable(tableName: "project_request_lead_bioinformatician")
    }

    changeSet(author: "", id: "1597598968643-39") {
        dropTable(tableName: "project_request_submitter")
    }

    changeSet(author: "", id: "1597598968643-57") {
        dropColumn(columnName: "pi_id", tableName: "project_request")
    }
}
