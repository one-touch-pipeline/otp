/*
 * Copyright 2011-2021 The OTP authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

databaseChangeLog = {

    changeSet(author: "Julian Rausch", id: "1640100537390-1") {
        createTable(tableName: "project_request_persistent_state") {
            column(name: "id", type: "BIGINT") {
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

            column(name: "bean_name", type: "VARCHAR(255)") {
                constraints(nullable: "false")
            }

            column(name: "current_owner_id", type: "BIGINT")
        }
    }

    changeSet(author: "Julian Rausch", id: "1640100537390-2") {
        createTable(tableName: "project_request_persistent_state_users_that_already_approved") {
            column(name: "project_request_persistent_state_id", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "user_id", type: "BIGINT")
        }
    }

    changeSet(author: "Julian Rausch", id: "1640100537390-3") {
        createTable(tableName: "project_request_persistent_state_users_that_need_to_approve") {
            column(name: "project_request_persistent_state_id", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "user_id", type: "BIGINT")
        }
    }

    changeSet(author: "Julian Rausch", id: "1640100537390-4") {
        addColumn(tableName: "project_request") {
            column(name: "state_id", type: "int8") {
                constraints(nullable: "false")
            }
        }
    }

    changeSet(author: "Julian Rausch", id: "1640100537390-5") {
        addPrimaryKey(columnNames: "id", constraintName: "project_request_persistent_statePK", tableName: "project_request_persistent_state")
    }

    changeSet(author: "Julian Rausch", id: "1640100537390-8") {
        addForeignKeyConstraint(baseColumnNames: "project_request_persistent_state_id", baseTableName: "project_request_persistent_state_users_that_already_approved", constraintName: "FK2dp694rj6xruniv4l89bjmioh", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "project_request_persistent_state")
    }

    changeSet(author: "Julian Rausch", id: "1640100537390-9") {
        addForeignKeyConstraint(baseColumnNames: "user_id", baseTableName: "project_request_persistent_state_users_that_already_approved", constraintName: "FK2r8ylaj7g9er3cgb9qy3dfl3k", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "users")
    }

    changeSet(author: "Julian Rausch", id: "1640100537390-10") {
        addForeignKeyConstraint(baseColumnNames: "state_id", baseTableName: "project_request", constraintName: "FK4h8bj6lbdujxmd9fids6g5yk4", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "project_request_persistent_state")
    }

    changeSet(author: "Julian Rausch", id: "1640100537390-11") {
        addForeignKeyConstraint(baseColumnNames: "user_id", baseTableName: "project_request_persistent_state_users_that_need_to_approve", constraintName: "FKa5um74gr5jox4n1wyq5ejvrfn", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "users")
    }

    changeSet(author: "Julian Rausch", id: "1640100537390-12") {
        addForeignKeyConstraint(baseColumnNames: "project_request_persistent_state_id", baseTableName: "project_request_persistent_state_users_that_need_to_approve", constraintName: "FKe7l1y8pxcv20b8i597jntfsqt", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "project_request_persistent_state")
    }

    changeSet(author: "Julian Rausch", id: "1640100537390-13") {
        addForeignKeyConstraint(baseColumnNames: "current_owner_id", baseTableName: "project_request_persistent_state", constraintName: "FKfh8tyaq15y0e6ogvybg98a7ef", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "users")
    }

    changeSet(author: "Julian Rausch", id: "1640271424727-1") {
        createTable(tableName: "project_request_custom_species_with_strains") {
            column(name: "project_request_id", type: "BIGINT") {
                constraints(nullable: "false")
            }
            column(name: "custom_species_with_strains_string", type: "VARCHAR(255)")
        }
    }
    changeSet(author: "Julian Rausch", id: "1640271424727-4") {
        addForeignKeyConstraint(baseColumnNames: "project_request_id", baseTableName: "project_request_custom_species_with_strains", constraintName: "FKlqt24pl8r3387kelyrolg7r", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "project_request")
    }
    changeSet(author: "Julian Rausch", id: "1640271424727-40") {
        dropColumn(columnName: "approval_state", tableName: "project_request_user")
    }
    changeSet(author: "Julian Rausch", id: "1640271424727-41") {
        dropColumn(columnName: "custom_species_with_strain", tableName: "project_request")
    }
    changeSet(author: "Julian Rausch", id: "1640271424727-42") {
        dropColumn(columnName: "status", tableName: "project_request")
    }

    changeSet(author: "Julian Rausch", id: "1643034232578-1") {
        createTable(tableName: "project_request_sequencing_centers") {
            column(name: "project_request_id", type: "BIGINT") {
                constraints(nullable: "false")
            }
            column(name: "sequencing_centers_string", type: "VARCHAR(255)")
        }
    }
    changeSet(author: "Julian Rausch", id: "1643034232578-4") {
        addForeignKeyConstraint(baseColumnNames: "project_request_id", baseTableName: "project_request_sequencing_centers", constraintName: "FK1vh77c49r2059hw8ph2oek48x", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "project_request")
    }
    changeSet(author: "Julian Rausch", id: "1643034232578-40") {
        dropColumn(columnName: "sequencing_center", tableName: "project_request")
    }
}
