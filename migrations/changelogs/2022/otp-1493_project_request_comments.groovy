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
    changeSet(author: "Julian Rausch", id: "1653317032799-1") {
        createTable(tableName: "project_request_comment") {
            column(name: "project_request_comments_id", type: "BIGINT") {
                constraints(nullable: "false")
            }
            column(name: "comment_id", type: "BIGINT")
            column(name: "comments_idx", type: "INT")
        }
    }

    changeSet(author: "Julian Rausch", id: "1653317032799-5") {
        addForeignKeyConstraint(baseColumnNames: "comment_id", baseTableName: "project_request_comment", constraintName: "FKeirwxy4mlvsodx1717gsgtepp", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "comment")
    }

    changeSet(author: "Julian Rausch", id: "1653317032799-41") {
        renameColumn(oldColumnName: "comments", newColumnName: "requester_comment", tableName: "project_request")
    }
}
