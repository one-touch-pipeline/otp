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

    changeSet(author: "-", id: "1726589155331-91") {
        createIndex(indexName: "workflow_run_input_artefact_workflow_run_workflow_artefact_idx", tableName: "workflow_run_input_artefact", unique: "false") {
            column(name: "workflow_run_id")

            column(name: "workflow_artefact_id")
        }
    }

    changeSet(author: "-", id: "1726589155331-92") {
        createIndex(indexName: "project_state_idx", tableName: "project") {
            column(name: "state")
        }
    }

    changeSet(author: "-", id: "1726589155331-93") {
        createIndex(indexName: "workflow_enabled_idx", tableName: "workflow") {
            column(name: "enabled")
        }
    }

    changeSet(author: "-", id: "1726589155331-95") {
        createIndex(indexName: "workflow_run_state_workflow_priority_project_idx", tableName: "workflow_run") {
            column(name: "state")

            column(name: "workflow_id")

            column(name: "priority_id")

            column(name: "project_id")
        }
    }

    changeSet(author: "-", id: "1726589155331-96") {
        dropIndex(indexName: "workflow_run_state_idx", tableName: "workflow_run")
    }

    changeSet(author: "-", id: "1726589155331-97") {
        dropIndex(indexName: "workflow_run_input_workflow_run_idx", tableName: "workflow_run_input_artefact")
    }
}
