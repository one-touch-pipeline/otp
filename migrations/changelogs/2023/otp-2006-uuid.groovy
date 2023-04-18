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
    changeSet(author: "-", id: "1681921528273-79") {
        createTable(tableName: "base_folder") {
            column(name: "id", type: "BIGINT") {
                constraints(nullable: "false", primaryKey: "true", primaryKeyName: "base_folderPK")
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

            column(name: "path", type: "VARCHAR(255)") {
                constraints(nullable: "false")
            }

            column(name: "writable", type: "BOOLEAN") {
                constraints(nullable: "false")
            }
        }
    }

    changeSet(author: "-", id: "1681921528273-80") {
        createTable(tableName: "work_folder") {
            column(name: "id", type: "BIGINT") {
                constraints(nullable: "false", primaryKey: "true", primaryKeyName: "work_folderPK")
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

            column(name: "uuid", type: "UUID") {
                constraints(nullable: "true")
            }

            column(name: "size", type: "BIGINT") {
                constraints(nullable: "true")
            }

            column(name: "base_folder_id", type: "BIGINT") {
                constraints(nullable: "false")
            }
        }
    }

    changeSet(author: "-", id: "1681921528273-81") {
        addColumn(tableName: "workflow_run") {
            column(name: "work_folder_id", type: "int8") {
                constraints(nullable: "true")
            }
        }
    }

    changeSet(author: "-", id: "1681921528273-85") {
        addUniqueConstraint(columnNames: "path", constraintName: "UC_BASE_FOLDERPATH_COL", tableName: "base_folder")
    }

    changeSet(author: "-", id: "1681921528273-92") {
        addForeignKeyConstraint(baseColumnNames: "base_folder_id", baseTableName: "work_folder", constraintName: "FKe5bau9mk7i8qgxp69xyqakhbg", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "base_folder", validate: "true")
    }

    changeSet(author: "-", id: "1681921528273-94") {
        addForeignKeyConstraint(baseColumnNames: "work_folder_id", baseTableName: "workflow_run", constraintName: "FKk9owf0vupoba1y4260jpdnpb7", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "work_folder", validate: "true")
    }
}
