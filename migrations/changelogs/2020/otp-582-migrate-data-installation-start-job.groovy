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

    changeSet(author: "borufka (generated)", id: "1593436283687-1") {
        addColumn(tableName: "seq_track") {
            column(name: "withdrawn_comment", type: "varchar(255)")
        }
    }

    changeSet(author: "borufka (generated)", id: "1593436283687-2") {
        addColumn(tableName: "seq_track") {
            column(name: "withdrawn_date", type: "date")
        }
    }

    changeSet(author: "borufka (generated)", id: "1593436283687-3") {
        addColumn(tableName: "seq_track") {
            column(name: "workflow_artefact_id", type: "int8")
        }
    }

    changeSet(author: "borufka (generated)", id: "1593436283687-6") {
        createIndex(indexName: "seq_track_workflow_artefact_idx", tableName: "seq_track") {
            column(name: "workflow_artefact_id")
        }
    }

    changeSet(author: "borufka (generated)", id: "1593436283687-7") {
        addForeignKeyConstraint(baseColumnNames: "workflow_artefact_id", baseTableName: "seq_track", constraintName: "FKt90xeu1b0thcssepl007773v8", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "workflow_artefact")
    }
}


