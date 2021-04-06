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

    changeSet(author: "gabkol", id: "otp-919-1-new-dta-data-structure") {
        createTable(tableName: "data_transfer_agreement") {
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
            column(name: "project_id", type: "BIGINT") {
                constraints(nullable: "false")
            }
            column(name: "comment", type: "TEXT") {
                constraints(nullable: "true")
            }
            column(name: "dta_id", type: "VARCHAR(255)") {
                constraints(nullable: "true")
            }
            column(name: "peer_institution", type: "VARCHAR(255)") {
                constraints(nullable: "false")
            }
            column(name: "legal_basis", type: "VARCHAR(255)") {
                constraints(nullable: "false")
            }
            column(name: "validity_date", type: "DATE") {
                constraints(nullable: "true")
            }
        }

        addPrimaryKey(columnNames: "id", constraintName: "data_transfer_agreementPK", tableName: "data_transfer_agreement")
        addForeignKeyConstraint(baseColumnNames: "project_id", baseTableName: "data_transfer_agreement", constraintName: "data_transfer_agreement_project_id_fkey", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "project")

        createTable(tableName: "data_transfer_agreement_document") {
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
            column(name: "data_transfer_agreement_id", type: "BIGINT") {
                constraints(nullable: "false")
            }
            column(name: "file_name", type: "TEXT") {
                constraints(nullable: "false")
            }
        }

        addPrimaryKey(columnNames: "id", constraintName: "data_transfer_agreement_documentPK", tableName: "data_transfer_agreement_document")
        addForeignKeyConstraint(baseColumnNames: "data_transfer_agreement_id", baseTableName: "data_transfer_agreement_document", constraintName: "data_transfer_agreement_document_data_transfer_agreement_id_fkey", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "data_transfer_agreement")
        addUniqueConstraint(columnNames: "data_transfer_agreement_id, file_name", constraintName: "data_transfer_agreement_document_data_transfer_agreement_id_file_name_unique", tableName: "data_transfer_agreement_document")

        createTable(tableName: "data_transfer_document") {
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
            column(name: "data_transfer_id", type: "BIGINT") {
                constraints(nullable: "false")
            }
            column(name: "file_name", type: "TEXT") {
                constraints(nullable: "false")
            }
        }

        addPrimaryKey(columnNames: "id", constraintName: "data_transfer_documentPK", tableName: "data_transfer_document")
        addForeignKeyConstraint(baseColumnNames: "data_transfer_id", baseTableName: "data_transfer_document", constraintName: "data_transfer_document_data_transfer_id_fkey", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "data_transfer")
        addUniqueConstraint(columnNames: "data_transfer_id, file_name", constraintName: "data_transfer_document_data_transfer_id_file_name_unique", tableName: "data_transfer_document")

        sql("ALTER TABLE data_transfer ADD data_transfer_agreement_id BIGINT")
        addForeignKeyConstraint(baseColumnNames: "data_transfer_agreement_id", baseTableName: "data_transfer", constraintName: "data_transfer_data_transfer_agreement_id_fkey", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "data_transfer_agreement")
    }
}
