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

    changeSet(author: "Julian Rausch", id: "1632985366742-1") {
        createTable(tableName: "sample_common_name") {
            column(name: "sample_mixed_in_species_id", type: "BIGINT") {
                constraints(nullable: "false")
            }
            column(name: "common_name_id", type: "BIGINT")
        }
    }
    changeSet(author: "Julian Rausch", id: "1632985366742-2") {
        addColumn(tableName: "individual") {
            column(name: "species_id", type: "int8")
        }
    }
    changeSet(author: "Julian Rausch", id: "1632985366742-5") {
        createIndex(indexName: "individual_species_idx", tableName: "individual") {
            column(name: "species_id")
        }
    }
    changeSet(author: "Julian Rausch", id: "1632985366742-6") {
        addForeignKeyConstraint(baseColumnNames: "species_id", baseTableName: "individual", constraintName: "FK1l5rx680lr3qe4ir3acc0hdgf", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "common_name")
    }
    changeSet(author: "Julian Rausch", id: "1632985366742-7") {
        addForeignKeyConstraint(baseColumnNames: "sample_mixed_in_species_id", baseTableName: "sample_common_name", constraintName: "FKar1okxqf8tj1k6ff2q6qsaf4a", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "sample")
    }
    changeSet(author: "Julian Rausch", id: "1632985366742-8") {
        addForeignKeyConstraint(baseColumnNames: "common_name_id", baseTableName: "sample_common_name", constraintName: "FKrdyf0yckeip1htcaed7ue0x3y", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "common_name")
    }
}
