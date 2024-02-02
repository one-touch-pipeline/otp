/*
 * Copyright 2011-2024 The OTP authors
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

    changeSet(author: "-", id: "1685018775340-90") {
        createTable(tableName: "wes_run_log_wes_log") {
            column(name: "wes_run_log_task_logs_id", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "wes_log_id", type: "BIGINT")
        }
    }

    changeSet(author: "-", id: "1685018775340-91") {
        createIndex(indexName: "wes_run_log_run_log_idx", tableName: "wes_run_log") {
            column(name: "run_log_id")
        }
    }

    changeSet(author: "-", id: "1685018775340-92") {
        createIndex(indexName: "wes_run_log_state_idx", tableName: "wes_run_log") {
            column(name: "state")
        }
    }

    changeSet(author: "-", id: "1685018775340-93") {
        createIndex(indexName: "wes_run_state_idx", tableName: "wes_run") {
            column(name: "state")
        }
    }

    changeSet(author: "-", id: "1685018775340-94") {
        createIndex(indexName: "wes_run_wes_identifier_idx", tableName: "wes_run") {
            column(name: "wes_identifier")
        }
    }

    changeSet(author: "-", id: "1685018775340-95") {
        addForeignKeyConstraint(baseColumnNames: "wes_run_log_task_logs_id", baseTableName: "wes_run_log_wes_log", constraintName: "FKhhk7h5orfon7w95kk76mwrf84", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "wes_run_log", validate: "true")
    }

    changeSet(author: "-", id: "1685018775340-96") {
        addForeignKeyConstraint(baseColumnNames: "wes_log_id", baseTableName: "wes_run_log_wes_log", constraintName: "FKogq6dnt82emgwdsdt4srcbjkn", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "wes_log", validate: "true")
    }

    changeSet(author: "-", id: "1685018775340-97") {
        dropForeignKeyConstraint(baseTableName: "wes_log", constraintName: "FKffdx2dvrplxc515o3dujd0pvr")
    }

    changeSet(author: "-", id: "1685018775340-133") {
        dropColumn(columnName: "task_logs_idx", tableName: "wes_log")
    }

    changeSet(author: "-", id: "1685018775340-134") {
        dropColumn(columnName: "wes_run_log_id", tableName: "wes_log")
    }

    changeSet(author: "-", id: "1685018775340-34") {
        addNotNullConstraint(columnDataType: "bigint", columnName: "run_log_id", tableName: "wes_run_log", validate: "true")
    }

    changeSet(author: "-", id: "1685018775340-100") {
        createIndex(indexName: "wes_run_log_wes_log_wes_run_log_task_logs_id_idx", tableName: "wes_run_log_wes_log") {
            column(name: "wes_run_log_task_logs_id")
        }
    }

    changeSet(author: "-", id: "1685018775340-101") {
        createIndex(indexName: "wes_run_log_wes_log_wes_log_id_idx", tableName: "wes_run_log_wes_log") {
            column(name: "wes_log_id")
        }
    }

    changeSet(author: "-", id: "1685018775340-102") {
        modifyDataType(columnName: "run_request", newDataType: "text", tableName: "wes_run_log")
    }
}
