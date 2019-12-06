databaseChangeLog = {

    changeSet(author: "kerssema", id: "otp-215-0-fix-old-empty-should-be-null") {
        sql("""UPDATE project_info SET dta_id = NULL WHERE length(dta_id) = 0;
               UPDATE project_info SET comment = NULL WHERE length(comment) = 0;""")
    }

    changeSet(author: "kerssema", id: "otp-215-1-create-new-data-transfer-domain") {
        createTable(tableName: "data_transfer") {
            column(name: "id", type: "BIGINT") {
                constraints(nullable: "false")
            }
            column(name: "version", type: "BIGINT") {
                constraints(nullable: "false")
            }
            column(name: "transfer_mode", type: "VARCHAR(10)") {
                constraints(nullable: "false")
            }
            column(name: "date_created", type: "TIMESTAMP WITHOUT TIME ZONE") {
                constraints(nullable: "false")
            }
            column(name: "last_updated", type: "TIMESTAMP WITHOUT TIME ZONE") {
                constraints(nullable: "false")
            }

            column(name: "completion_date", type: "TIMESTAMP WITHOUT TIME ZONE")

            column(name: "project_info_id", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "direction", type: "varchar(16)") {
                constraints(nullable: "false")
            }

            column(name: "comment", type: "TEXT")
            column(name: "ticketid", type: "VARCHAR(255)")
            column(name: "performing_user_id", type: "BIGINT") {
                constraints(nullable: "false")
            }
            column(name: "transfer_date", type: "TIMESTAMP WITHOUT TIME ZONE") {
                constraints(nullable: "false")
            }

            column(name: "requester", type: "VARCHAR(255)") {
                constraints(nullable: "false")
            }

            column(name: "peer_person", type: "VARCHAR(255)") {
                constraints(nullable: "false")
            }
            column(name: "peer_account", type: "VARCHAR(255)")
        }
        addPrimaryKey(columnNames: "id", constraintName: "data_transferPK", tableName: "data_transfer")
        addForeignKeyConstraint(baseColumnNames: "project_info_id", baseTableName: "data_transfer", constraintName: "data_transfer_project_info_id_fkey", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "project_info")
        addForeignKeyConstraint(baseColumnNames: "performing_user_id", baseTableName: "data_transfer", constraintName: "data_transfer_performing_user_id_fkey", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "users")
    }

    changeSet(author: "kerssema", id: "otp-215-2-migrate-existing-transfers-from-project-info") {
        sql("""
            -- copy moved column to the new domain class
            -- in HD, all ProjectInfo with additional details/transfer info have their legal basis set.
            INSERT INTO data_transfer (
                id,
                version,
                project_info_id,
                performing_user_id,
                direction,
                peer_person,
                peer_account,
                requester,
                ticketid,
                transfer_mode,
                transfer_date,
                date_created,
                last_updated,
                completion_date,
                comment
            )
            SELECT
                nextval('hibernate_sequence'),
                0,
                id,
                performing_user_id,
                'OUTGOING',
                recipient_person,
                recipient_account,
                requester,
                ticketid,
                transfer_mode,
                transfer_date,
                date_created,
                now(),
                deletion_date,
                comment
            FROM project_info
            WHERE legal_basis IS NOT NULL;

            -- manually checked: all existing comments on ProjectInfo documents apply to the transfer
            -- (in HD)
            UPDATE project_info SET comment = NULL;
        """)
    }

    changeSet(author: "kerssema", id: "otp-215-3-delete-moved-columns-from-project-info") {
        dropForeignKeyConstraint(baseTableName: "project_info", constraintName: "project_info_performing_user_id_fkey")
        renameColumn(oldColumnName: "recipient_institution", newColumnName: "peer_institution", tableName: "project_info")
        dropColumn(columnName: "performing_user_id", tableName: "project_info")
        dropColumn(columnName: "deletion_date", tableName: "project_info")
        dropColumn(columnName: "recipient_account", tableName: "project_info")
        dropColumn(columnName: "recipient_person", tableName: "project_info")
        dropColumn(columnName: "requester", tableName: "project_info")
        dropColumn(columnName: "ticketid", tableName: "project_info")
        dropColumn(columnName: "transfer_date", tableName: "project_info")
        dropColumn(columnName: "transfer_mode", tableName: "project_info")
    }
}
