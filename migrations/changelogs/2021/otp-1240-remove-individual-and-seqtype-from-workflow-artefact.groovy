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

    changeSet(author: "Julian Rausch", id: "1629353713249-3") {
        dropForeignKeyConstraint(baseTableName: "workflow_artefact", constraintName: "FKcjpthxyd3d9mm1l5xarbq4r0m")
    }

    changeSet(author: "Julian Rausch", id: "1629353713249-4") {
        dropForeignKeyConstraint(baseTableName: "workflow_artefact", constraintName: "FKetm7rqu0oe7usgh7ffrosjsi7")
    }

    changeSet(author: "Julian Rausch", id: "1629353713249-40") {
        dropColumn(columnName: "individual_id", tableName: "workflow_artefact")
    }

    changeSet(author: "Julian Rausch", id: "1629353713249-41") {
        dropColumn(columnName: "seq_type_id", tableName: "workflow_artefact")
    }
}