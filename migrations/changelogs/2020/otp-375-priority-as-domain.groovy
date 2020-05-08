/*
 * Copyright 2011-2020 The OTP authors
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

    changeSet(author: "borufka (generated)", id: "1588598589423-1") {
        createTable(tableName: "processing_priority") {
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

            column(name: "roddy_config_suffix", type: "VARCHAR(255)") {
                constraints(nullable: "false")
            }

            column(name: "queue", type: "VARCHAR(255)") {
                constraints(nullable: "false")
            }

            column(name: "allowed_parallel_workflow_runs", type: "INT") {
                constraints(nullable: "false")
            }

            column(name: "priority", type: "INT") {
                constraints(nullable: "false")
            }

            column(name: "name", type: "VARCHAR(255)") {
                constraints(nullable: "false")
            }

            column(name: "error_mail_prefix", type: "VARCHAR(255)") {
                constraints(nullable: "false")
            }
        }
    }

    changeSet(author: "borufka (generated)", id: "1588598589423-2") {
        addColumn(tableName: "project") {
            column(name: "processing_priority_id", type: "int8") {
                constraints(nullable: "true")
            }
        }
    }

    changeSet(author: "borufka (generated)", id: "1588598589423-3") {
        addPrimaryKey(columnNames: "id", constraintName: "processing_priorityPK", tableName: "processing_priority")
    }

    changeSet(author: "borufka (generated)", id: "1588598589423-6") {
        addUniqueConstraint(columnNames: "name", constraintName: "UC_PROCESSING_PRIORITYNAME_COL", tableName: "processing_priority")
    }

    changeSet(author: "borufka (generated)", id: "1588598589423-7") {
        addUniqueConstraint(columnNames: "priority", constraintName: "UC_PROCESSING_PRIORITYPRIORITY_COL", tableName: "processing_priority")
    }

    changeSet(author: "borufka (generated)", id: "1588598589423-8") {
        createIndex(indexName: "processing_priority_allowed_parallel_workflow_runs_idx", tableName: "processing_priority") {
            column(name: "allowed_parallel_workflow_runs")
        }
    }

    changeSet(author: "borufka (generated)", id: "1588598589423-9") {
        addForeignKeyConstraint(baseColumnNames: "processing_priority_id", baseTableName: "project", constraintName: "FKly20vwdui47fy3awg5dwxkmfc", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "processing_priority")
    }

    changeSet(author: "borufka (generated)", id: "1588598589423-sql") {
        sqlFile(path: 'changelogs/2020/otp-375-priority-as-domain.sql')
    }

    changeSet(author: "borufka (generated)", id: "1588598589423-43") {
        dropColumn(columnName: "processing_priority", tableName: "project")
    }

    changeSet(author: "borufka (generated)", id: "1588598807994-3") {
        createIndex(indexName: "project_processing_priority_idx", tableName: "project") {
            column(name: "processing_priority_id")
        }
    }

    changeSet(author: "borufka (generated)", id: "1588598918120-60") {
        addNotNullConstraint(columnDataType: "bigint", columnName: "processing_priority_id", tableName: "project")
    }
}
