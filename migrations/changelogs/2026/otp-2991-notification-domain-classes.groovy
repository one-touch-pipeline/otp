/*
 * Copyright 2011-2026 The OTP authors
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
    changeSet(author: "tirtaram", id: "otp-2991-1") {
        createTable(tableName: "notification") {
            column(name: "id", type: "BIGINT") {
                constraints(nullable: "false", primaryKey: "true", primaryKeyName: "notificationPK")
            }
            column(name: "version", type: "BIGINT") {
                constraints(nullable: "false")
            }
            column(name: "date_created", type: "TIMESTAMP WITH TIME ZONE") {
                constraints(nullable: "false")
            }
            column(name: "last_updated", type: "TIMESTAMP WITH TIME ZONE") {
                constraints(nullable: "false")
            }
            column(name: "notification_state", type: "VARCHAR(255)") {
                constraints(nullable: "false")
            }
            column(name: "notification_scope", type: "VARCHAR(255)") {
                constraints(nullable: "false")
            }
            column(name: "notification_scope_id", type: "BIGINT") {
                constraints(nullable: "false")
            }
            column(name: "workflow_id", type: "BIGINT")
            column(name: "project_id", type: "BIGINT") {
                constraints(nullable: "false")
            }
        }
    }

    changeSet(author: "tirtaram", id: "otp-2991-2") {
        createTable(tableName: "notification_status") {
            column(name: "id", type: "BIGINT") {
                constraints(nullable: "false", primaryKey: "true", primaryKeyName: "notification_statusPK")
            }
            column(name: "version", type: "BIGINT") {
                constraints(nullable: "false")
            }
            column(name: "date_created", type: "TIMESTAMP WITH TIME ZONE") {
                constraints(nullable: "false")
            }
            column(name: "last_updated", type: "TIMESTAMP WITH TIME ZONE") {
                constraints(nullable: "false")
            }
            column(name: "finished_workflow_count", type: "INTEGER") {
                constraints(nullable: "false")
            }
            column(name: "ticket_id", type: "BIGINT") {
                constraints(nullable: "false")
            }
            column(name: "final_send", type: "BOOLEAN") {
                constraints(nullable: "false")
            }
        }
    }

    changeSet(author: "tirtaram", id: "otp-2991-3") {
        createTable(tableName: "create_notification") {
            column(name: "id", type: "BIGINT") {
                constraints(nullable: "false", primaryKey: "true", primaryKeyName: "create_notificationPK")
            }
            column(name: "version", type: "BIGINT") {
                constraints(nullable: "false")
            }
            column(name: "date_created", type: "TIMESTAMP WITH TIME ZONE") {
                constraints(nullable: "false")
            }
            column(name: "last_updated", type: "TIMESTAMP WITH TIME ZONE") {
                constraints(nullable: "false")
            }
            column(name: "ticket_id", type: "BIGINT") {
                constraints(nullable: "false")
            }
            column(name: "state", type: "VARCHAR(255)") {
                constraints(nullable: "false")
            }
        }
    }

    changeSet(author: "tirtaram", id: "otp-2991-4") {
        createTable(tableName: "notification_workflow_run") {
            column(name: "notification_workflow_runs_id", type: "BIGINT") {
                constraints(nullable: "false")
            }
            column(name: "workflow_run_id", type: "BIGINT") {
                constraints(nullable: "false")
            }
        }
    }

    changeSet(author: "tirtaram", id: "otp-2991-5") {
        createTable(tableName: "notification_depending_notification") {
            column(name: "notification_id", type: "BIGINT") {
                constraints(nullable: "false")
            }
            column(name: "depending_notification_id", type: "BIGINT") {
                constraints(nullable: "false")
            }
        }
    }

    changeSet(author: "tirtaram", id: "otp-2991-6") {
        createTable(tableName: "notification_status_notification") {
            column(name: "notification_status_notifications_id", type: "BIGINT") {
                constraints(nullable: "false")
            }
            column(name: "notification_id", type: "BIGINT") {
                constraints(nullable: "false")
            }
        }
    }

    changeSet(author: "tirtaram", id: "otp-2991-7") {
        createTable(tableName: "create_notification_workflow_run") {
            column(name: "create_notification_workflow_runs_id", type: "BIGINT") {
                constraints(nullable: "false")
            }
            column(name: "workflow_run_id", type: "BIGINT") {
                constraints(nullable: "false")
            }
        }
    }

    changeSet(author: "tirtaram", id: "otp-2991-8") {
        addForeignKeyConstraint(
                constraintName: "notification_workflow_fkey",
                baseTableName: "notification",
                baseColumnNames: "workflow_id",
                referencedTableName: "workflow",
                referencedColumnNames: "id"
        )
        addForeignKeyConstraint(
                constraintName: "notification_project_fkey",
                baseTableName: "notification",
                baseColumnNames: "project_id",
                referencedTableName: "project",
                referencedColumnNames: "id"
        )
        addForeignKeyConstraint(
                constraintName: "notification_status_ticket_fkey",
                baseTableName: "notification_status",
                baseColumnNames: "ticket_id",
                referencedTableName: "ticket",
                referencedColumnNames: "id"
        )
        addForeignKeyConstraint(
                constraintName: "create_notification_ticket_fkey",
                baseTableName: "create_notification",
                baseColumnNames: "ticket_id",
                referencedTableName: "ticket",
                referencedColumnNames: "id"
        )
        addForeignKeyConstraint(
                constraintName: "notification_workflow_run_notification_fkey",
                baseTableName: "notification_workflow_run",
                baseColumnNames: "notification_workflow_runs_id",
                referencedTableName: "notification",
                referencedColumnNames: "id"
        )
        addForeignKeyConstraint(
                constraintName: "notification_workflow_run_workflow_run_fkey",
                baseTableName: "notification_workflow_run",
                baseColumnNames: "workflow_run_id",
                referencedTableName: "workflow_run",
                referencedColumnNames: "id"
        )
        addForeignKeyConstraint(
                constraintName: "notification_depending_notification_notification_fkey",
                baseTableName: "notification_depending_notification",
                baseColumnNames: "notification_id",
                referencedTableName: "notification",
                referencedColumnNames: "id"
        )
        addForeignKeyConstraint(
                constraintName: "notification_depending_notification_depending_fkey",
                baseTableName: "notification_depending_notification",
                baseColumnNames: "depending_notification_id",
                referencedTableName: "notification",
                referencedColumnNames: "id"
        )
        addForeignKeyConstraint(
                constraintName: "notification_status_notification_notification_status_fkey",
                baseTableName: "notification_status_notification",
                baseColumnNames: "notification_status_notifications_id",
                referencedTableName: "notification_status",
                referencedColumnNames: "id"
        )
        addForeignKeyConstraint(
                constraintName: "notification_status_notification_notification_fkey",
                baseTableName: "notification_status_notification",
                baseColumnNames: "notification_id",
                referencedTableName: "notification",
                referencedColumnNames: "id"
        )
        addForeignKeyConstraint(
                constraintName: "create_notification_workflow_run_create_notification_fkey",
                baseTableName: "create_notification_workflow_run",
                baseColumnNames: "create_notification_workflow_runs_id",
                referencedTableName: "create_notification",
                referencedColumnNames: "id"
        )
        addForeignKeyConstraint(
                constraintName: "create_notification_workflow_run_workflow_run_fkey",
                baseTableName: "create_notification_workflow_run",
                baseColumnNames: "workflow_run_id",
                referencedTableName: "workflow_run",
                referencedColumnNames: "id"
        )
    }

    changeSet(author: "tirtaram", id: "otp-2991-9") {
        createIndex(indexName: "notification_workflow_idx", tableName: "notification") {
            column(name: "workflow_id")
        }
        createIndex(indexName: "notification_project_idx", tableName: "notification") {
            column(name: "project_id")
        }
        createIndex(indexName: "notification_status_ticket_idx", tableName: "notification_status") {
            column(name: "ticket_id")
        }
        createIndex(indexName: "create_notification_ticket_idx", tableName: "create_notification") {
            column(name: "ticket_id")
        }
        createIndex(indexName: "notification_workflow_run_notification_idx", tableName: "notification_workflow_run") {
            column(name: "notification_workflow_runs_id")
        }
        createIndex(indexName: "notification_workflow_run_workflow_run_idx", tableName: "notification_workflow_run") {
            column(name: "workflow_run_id")
        }
        createIndex(indexName: "notification_depending_notification_notification_idx", tableName: "notification_depending_notification") {
            column(name: "notification_id")
        }
        createIndex(indexName: "notification_depending_notification_depending_idx", tableName: "notification_depending_notification") {
            column(name: "depending_notification_id")
        }
        createIndex(indexName: "notification_status_notification_notification_status_idx", tableName: "notification_status_notification") {
            column(name: "notification_status_notifications_id")
        }
        createIndex(indexName: "notification_status_notification_notification_idx", tableName: "notification_status_notification") {
            column(name: "notification_id")
        }
        createIndex(indexName: "create_notification_workflow_run_create_notification_idx", tableName: "create_notification_workflow_run") {
            column(name: "create_notification_workflow_runs_id")
        }
        createIndex(indexName: "create_notification_workflow_run_workflow_run_idx", tableName: "create_notification_workflow_run") {
            column(name: "workflow_run_id")
        }
    }
}
