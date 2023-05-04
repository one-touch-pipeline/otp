/*
 * Copyright 2011-2023 The OTP authors
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

    changeSet(author: "-", id: "1685971733732-91") {
        createTable(tableName: "department") {
            column(name: "id", type: "BIGINT") {
                constraints(nullable: "false", primaryKey: "true", primaryKeyName: "departmentPK")
            }

            column(name: "version", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "ou_number", type: "VARCHAR(255)") {
                constraints(nullable: "false")
            }

            column(name: "cost_center", type: "VARCHAR(255)") {
                constraints(nullable: "false")
            }

            column(name: "department_head_id", type: "BIGINT") {
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

    changeSet(author: "-", id: "1685971733732-94") {
        createTable(tableName: "piuser") {
            column(name: "id", type: "BIGINT") {
                constraints(nullable: "false", primaryKey: "true", primaryKeyName: "piuserPK")
            }

            column(name: "version", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "pi_id", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "deputypi_id", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "date_rights_granted", type: "TIMESTAMP WITHOUT TIME ZONE") {
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

    changeSet(author: "-", id: "1686669130267-91") {
        addUniqueConstraint(columnNames: "ou_number", constraintName: "UC_DEPARTMENTOU_NUMBER_COL", tableName: "department")
    }

    changeSet(author: "-", id: "1685977896232-103") {
        createIndex(indexName: "department_cost_center_idx", tableName: "department") {
            column(name: "cost_center")
        }
    }

    changeSet(author: "-", id: "1685977896232-104") {
        createIndex(indexName: "department_department_head_idx", tableName: "department") {
            column(name: "department_head_id")
        }
    }

    changeSet(author: "-", id: "1685971733732-103") {
        createIndex(indexName: "pi_users_deputy_pi_idx", tableName: "piuser") {
            column(name: "deputypi_id")
        }
    }

    changeSet(author: "-", id: "1685971733732-104") {
        createIndex(indexName: "pi_users_pi_idx", tableName: "piuser") {
            column(name: "pi_id")
        }
    }

    changeSet(author: "-", id: "1685971733732-105") {
        addForeignKeyConstraint(baseColumnNames: "pi_id", baseTableName: "piuser", constraintName: "FK22494moyh86peum538ydvvacc", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "users", validate: "true")
    }

    changeSet(author: "-", id: "1685971733732-106") {
        addForeignKeyConstraint(baseColumnNames: "department_head_id", baseTableName: "department", constraintName: "FK2h8c0g2j4rwyaumqibughbydx", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "users", validate: "true")
    }

    changeSet(author: "-", id: "1685971733732-107") {
        addForeignKeyConstraint(baseColumnNames: "deputypi_id", baseTableName: "piuser", constraintName: "FK302lycyonpec5een448dr7i3n", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "users", validate: "true")
    }

}
