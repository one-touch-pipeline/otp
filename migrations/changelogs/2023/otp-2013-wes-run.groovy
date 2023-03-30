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

    changeSet(author: "", id: "1680162652373-75") {
        createTable(tableName: "wes_run") {
            column(name: "id", type: "BIGINT") {
                constraints(nullable: "false", primaryKey: "true", primaryKeyName: "wes_runPK")
            }

            column(name: "version", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "workflow_step_id", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "date_created", type: "TIMESTAMP WITHOUT TIME ZONE") {
                constraints(nullable: "false")
            }

            column(name: "wes_identifier", type: "VARCHAR(255)") {
                constraints(nullable: "false")
            }

            column(name: "last_updated", type: "TIMESTAMP WITHOUT TIME ZONE") {
                constraints(nullable: "false")
            }

            column(name: "state", type: "VARCHAR(255)") {
                constraints(nullable: "false")
            }

            column(name: "sub_path", type: "VARCHAR(255)") {
                constraints(nullable: "false")
            }

            column(name: "wes_run_log_id", type: "BIGINT")
        }
    }

    changeSet(author: "", id: "1680162652373-78") {
        createIndex(indexName: "wes_run_wes_run_log_idx", tableName: "wes_run") {
            column(name: "wes_run_log_id")
        }
    }

    changeSet(author: "", id: "1680162652373-79") {
        createIndex(indexName: "wes_run_workflow_step_idx", tableName: "wes_run") {
            column(name: "workflow_step_id")
        }
    }

    changeSet(author: "", id: "1680162652373-80") {
        addForeignKeyConstraint(baseColumnNames: "workflow_step_id", baseTableName: "wes_run", constraintName: "FKfht8np7t6730tru4oi1ewtbcp", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "workflow_step", validate: "true")
    }

    changeSet(author: "", id: "1680162652373-81") {
        addForeignKeyConstraint(baseColumnNames: "wes_run_log_id", baseTableName: "wes_run", constraintName: "FKqt8kluo2951qfjvlw65g09at8", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "wes_run_log", validate: "true")
    }

    changeSet(author: "", id: "1680162652373-82") {
        dropForeignKeyConstraint(baseTableName: "wes_run_log", constraintName: "FK3geplj7dns1dc5e8wifj9p59f")
    }

    changeSet(author: "", id: "1680162652373-118") {
        dropColumn(columnName: "wes_identifier", tableName: "workflow_step")
    }

    changeSet(author: "", id: "1680162652373-119") {
        dropColumn(columnName: "workflow_step_id", tableName: "wes_run_log")
    }
}
