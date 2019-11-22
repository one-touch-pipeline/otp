databaseChangeLog = {

    changeSet(author: "", id: "1574440722829-1") {
        createTable(tableName: "project_request") {
            column(name: "id", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "version", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "date_created", type: "TIMESTAMP WITHOUT TIME ZONE") {
                constraints(nullable: "false")
            }

            column(name: "grant_id", type: "VARCHAR(255)")

            column(name: "storage_until", type: "date")

            column(name: "last_updated", type: "TIMESTAMP WITHOUT TIME ZONE") {
                constraints(nullable: "false")
            }

            column(name: "pi_id", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "cost_center", type: "VARCHAR(255)")

            column(name: "organizational_unit", type: "VARCHAR(255)") {
                constraints(nullable: "false")
            }

            column(name: "end_date", type: "date")

            column(name: "comments", type: "VARCHAR(255)")

            column(name: "project_type", type: "VARCHAR(255)") {
                constraints(nullable: "false")
            }

            column(name: "further_data_processing", type: "BOOLEAN")

            column(name: "force_copy_files", type: "BOOLEAN")

            column(name: "approx_no_of_samples", type: "INT")

            column(name: "species_with_strain_id", type: "BIGINT")

            column(name: "tumor_entity_id", type: "BIGINT")

            column(name: "sequencing_center", type: "VARCHAR(255)")

            column(name: "name", type: "VARCHAR(255)") {
                constraints(nullable: "false")
            }

            column(name: "status", type: "VARCHAR(255)") {
                constraints(nullable: "false")
            }

            column(name: "funding_body", type: "VARCHAR(255)")

            column(name: "requester_id", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "description", type: "TEXT") {
                constraints(nullable: "false")
            }

            column(name: "project_id", type: "BIGINT")

            column(name: "predecessor_project", type: "VARCHAR(255)")
        }

        createTable(tableName: "project_request_bioinformatician") {
            column(name: "project_request_id", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "bioinformatician_id", type: "BIGINT") {
                constraints(nullable: "false")
            }
        }

        createTable(tableName: "project_request_deputy_pi") {
            column(name: "project_request_id", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "deputy_pi_id", type: "BIGINT") {
                constraints(nullable: "false")
            }
        }

        createTable(tableName: "project_request_keywords") {
            column(name: "project_request_id", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "keywords_string", type: "VARCHAR(255)") {
                constraints(nullable: "false")
            }
        }

        createTable(tableName: "project_request_responsible_bioinformatician") {
            column(name: "project_request_id", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "responsible_bioinformatician_id", type: "BIGINT") {
                constraints(nullable: "false")
            }
        }

        createTable(tableName: "project_request_seq_type") {
            column(name: "project_request_seq_types_id", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "seq_type_id", type: "BIGINT") {
                constraints(nullable: "false")
            }
        }

        createTable(tableName: "project_request_submitter") {
            column(name: "project_request_id", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "submitter_id", type: "BIGINT") {
                constraints(nullable: "false")
            }
        }

        addPrimaryKey(columnNames: "id", constraintName: "project_requestPK", tableName: "project_request")

        addForeignKeyConstraint(baseColumnNames: "tumor_entity_id", baseTableName: "project_request", constraintName: "FK36jpufmnlu5j8082lchis2rha", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "tumor_entity")

        addForeignKeyConstraint(baseColumnNames: "project_request_seq_types_id", baseTableName: "project_request_seq_type", constraintName: "FK8mj8ac9ajfajmghv6aba5e69w", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "project_request")

        addForeignKeyConstraint(baseColumnNames: "seq_type_id", baseTableName: "project_request_seq_type", constraintName: "FK8s9kbr0ptwhtx2go7myalfgbg", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "seq_type")

        addForeignKeyConstraint(baseColumnNames: "bioinformatician_id", baseTableName: "project_request_bioinformatician", constraintName: "FKa3s1rq60vfe9h4jbxqvsdv5v3", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "users")

        addForeignKeyConstraint(baseColumnNames: "deputy_pi_id", baseTableName: "project_request_deputy_pi", constraintName: "FKasisv2ndn2eblmjypp7kken1u", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "users")

        addForeignKeyConstraint(baseColumnNames: "species_with_strain_id", baseTableName: "project_request", constraintName: "FKdxepnfeqyvtv3qldj7na2o9ql", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "species_with_strain")

        addForeignKeyConstraint(baseColumnNames: "pi_id", baseTableName: "project_request", constraintName: "FKe5pt1po4745usehtg1gn6x989", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "users")

        addForeignKeyConstraint(baseColumnNames: "project_request_id", baseTableName: "project_request_responsible_bioinformatician", constraintName: "FKgc3yi6ir045dx66mxtfen85yk", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "project_request")

        addForeignKeyConstraint(baseColumnNames: "project_id", baseTableName: "project_request", constraintName: "FKhkhfpuxdw9l8j2oyo4nqspiy0", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "project")

        addForeignKeyConstraint(baseColumnNames: "project_request_id", baseTableName: "project_request_deputy_pi", constraintName: "FKhpncqm5bof83mpgray4ch2bla", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "project_request")

        addForeignKeyConstraint(baseColumnNames: "project_request_id", baseTableName: "project_request_bioinformatician", constraintName: "FKhwtl9qi8pnmma4rpiecjbb4yf", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "project_request")

        addForeignKeyConstraint(baseColumnNames: "project_request_id", baseTableName: "project_request_keywords", constraintName: "FKiy5k8aohe7a6qylnaejqiu5s9", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "project_request")

        addForeignKeyConstraint(baseColumnNames: "project_request_id", baseTableName: "project_request_submitter", constraintName: "FKjua773syi89ivtnsiohu58dh1", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "project_request")

        addForeignKeyConstraint(baseColumnNames: "responsible_bioinformatician_id", baseTableName: "project_request_responsible_bioinformatician", constraintName: "FKkbw0usd1vicpufjuoeaeqq8lb", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "users")

        addForeignKeyConstraint(baseColumnNames: "requester_id", baseTableName: "project_request", constraintName: "FKnxky2b0avxjdej401wdmhyvt7", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "users")

        addForeignKeyConstraint(baseColumnNames: "submitter_id", baseTableName: "project_request_submitter", constraintName: "FKpq0kvmovyjfm0chawywih53iq", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "users")
    }
}
