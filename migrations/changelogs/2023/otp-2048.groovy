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

    changeSet(author: "-", id: "1693575545089-90") {
        createTable(tableName: "project_request_pi_user") {
            column(name: "project_request_pi_users_id", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "project_request_user_id", type: "BIGINT")
        }
        createIndex(tableName: "project_request_pi_user", indexName: "project_request_pi_users_idx") {
            column(name: "project_request_pi_users_id")
        }
    }

    changeSet(author: "-", id: "1693575545089-91") {
        addForeignKeyConstraint(baseColumnNames: "project_request_user_id", baseTableName: "project_request_pi_user", constraintName: "FK2nqhfr1eddcm8v03o5ihkmof7", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "project_request_user", validate: "true")
    }

    changeSet(author: "-", id: "1693575545089-92") {
        addForeignKeyConstraint(baseColumnNames: "project_request_pi_users_id", baseTableName: "project_request_pi_user", constraintName: "FK5cliatrudmd4l0qrpnpg1k6dq", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "project_request", validate: "true")
    }
}
