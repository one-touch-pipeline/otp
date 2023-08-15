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

    changeSet(author: "-", id: "1691593564141-89") {
        addColumn(tableName: "config_per_project_and_seq_type") {
            column(name: "auto_exec", type: "boolean")
        }
    }

    changeSet(author: "-", id: "1691593564141-89b") {
        sql("UPDATE config_per_project_and_seq_type SET auto_exec = FALSE")
    }

    changeSet(author: "-", id: "1691593564141-90") {
        addColumn(tableName: "config_per_project_and_seq_type") {
            column(name: "enforced_cells", type: "int4")
        }
    }

    changeSet(author: "-", id: "1691593564141-91") {
        addColumn(tableName: "config_per_project_and_seq_type") {
            column(name: "expected_cells", type: "int4")
        }
    }

    changeSet(author: "-", id: "1691593564141-92") {
        addColumn(tableName: "config_per_project_and_seq_type") {
            column(name: "reference_genome_index_id", type: "int8")
        }
    }

    changeSet(author: "-", id: "1691593564141-93") {
        addForeignKeyConstraint(baseColumnNames: "reference_genome_index_id", baseTableName: "config_per_project_and_seq_type", constraintName: "FK9jptqu4osf38f2i2bp3ei8kml", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "reference_genome_index", validate: "true")
    }
}
