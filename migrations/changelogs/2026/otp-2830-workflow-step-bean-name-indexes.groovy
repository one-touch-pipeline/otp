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
    changeSet(author: "tirtaram", id: "otp-2830-1") {
        createIndex(indexName: "workflow_step_bean_name__obsolete__id__workflow_run_id__idx", tableName: "workflow_step") {
            column(name: "bean_name")
            column(name: "obsolete")
            column(name: "id")
            column(name: "workflow_run_id")
        }
    }
    changeSet(author: "tirtaram", id: "otp-2830-2") {
        createIndex(indexName: "workflow_step_obsolete__workflow_run_id__id__idx", tableName: "workflow_step") {
            column(name: "obsolete")
            column(name: "workflow_run_id")
            column(name: "id")
        }
    }
}
