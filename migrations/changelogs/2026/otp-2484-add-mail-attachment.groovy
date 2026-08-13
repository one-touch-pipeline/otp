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
    changeSet(author: "Foued", id: "otp-2484-1") {
        createTable(tableName: "attachment") {
            column(name: "id", type: "BIGINT") {
                constraints(nullable: "false", primaryKey: "true", primaryKeyName: "attachmentPK")
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
            column(name: "name", type: "VARCHAR(255)") {
                constraints(nullable: "false")
            }
            column(name: "content", type: "TEXT") {
                constraints(nullable: "false")
            }
            column(name: "mail_id", type: "BIGINT") {
                constraints(nullable: "false")
            }
            column(name: "attachments_idx", type: "INTEGER")
        }
    }

    changeSet(author: "Foued", id: "otp-2484-2") {
        addForeignKeyConstraint(
                constraintName: "attachment_mail_fkey",
                baseTableName: "attachment",
                baseColumnNames: "mail_id",
                referencedTableName: "mail",
                referencedColumnNames: "id"
        )
    }

    changeSet(author: "Foued", id: "otp-2484-3") {
        createIndex(indexName: "attachment_mail_idx", tableName: "attachment") {
            column(name: "mail_id")
        }
    }
}
