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

    changeSet(author: "-", id: "1702037906067-89") {
        addColumn(tableName: "import_process") {
            column(name: "workflow_create_state", type: "varchar(255)") {
                constraints(nullable: "true")
            }
        }
    }

    changeSet(author: "-", id: "1702037906067-89b") {
        sql("update import_process set workflow_create_state = 'SUCCESS';")
    }

    changeSet(author: "-", id: "1702037906067-89c") {
        addNotNullConstraint(columnDataType: "varchar(255)", columnName: "workflow_create_state", tableName: "import_process")
    }

    changeSet(author: "-", id: "1702384583265-90") {
        createIndex(indexName: "import_process_workflow_create_state_idx", tableName: "import_process") {
            column(name: "workflow_create_state")
        }
    }
}
