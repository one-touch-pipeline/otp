/*
 * Copyright 2011-2022 The OTP authors
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

    changeSet(author: "Sunakshi", id: "1655108618839-1") {
        createTable(tableName: "project_request_custom_sequencing_centers") {
            column(name: "project_request_id", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "custom_sequencing_centers_string", type: "VARCHAR(255)")
        }
    }

    changeSet(author: "Sunakshi", id: "1655108618839-2") {
        createTable(tableName: "project_request_seq_center") {
            column(name: "project_request_sequencing_centers_id", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "seq_center_id", type: "BIGINT")
        }
    }

    changeSet(author: "Sunakshi", id: "1655108618839-5") {
        addForeignKeyConstraint(baseColumnNames: "project_request_id", baseTableName: "project_request_custom_sequencing_centers", constraintName: "FKbjg27pi2nchyepj8emqtqgpb7", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "project_request")
    }

    changeSet(author: "Sunakshi", id: "1655108618839-6") {
        addForeignKeyConstraint(baseColumnNames: "seq_center_id", baseTableName: "project_request_seq_center", constraintName: "FKm2y52tjoby8xxtp9qykcrhbr3", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "seq_center")
    }

    changeSet(author: "Sunakshi", id: "1655108618839-7") {
        addForeignKeyConstraint(baseColumnNames: "project_request_sequencing_centers_id", baseTableName: "project_request_seq_center", constraintName: "FKnm65euhaauog4ip6tjauhqbsp", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "project_request")
    }

    changeSet(author: "Sunakshi", id: "1655108618839-8") {
        dropForeignKeyConstraint(baseTableName: "project_request_sequencing_centers", constraintName: "FK1vh77c49r2059hw8ph2oek48x")
    }

    changeSet(author: "Sunakshi", id: "1655108618839-27") {
        dropTable(tableName: "project_request_sequencing_centers")
    }

    changeSet(author: "Sunakshi", id: "1654172584096-3") {
        createTable(tableName: "project_request_custom_seq_types") {
            column(name: "project_request_id", type: "BIGINT") {
                constraints(nullable: "false")
            }
            column(name: "custom_seq_types_string", type: "VARCHAR(255)")
        }
    }
    changeSet(author: "Sunakshi", id: "1654172584096-4") {
        addForeignKeyConstraint(baseColumnNames: "project_request_id", baseTableName: "project_request_custom_seq_types", constraintName: "FKofffbb2qlnx4ytkmaejbkuyqk", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "project_request")
    }
}
