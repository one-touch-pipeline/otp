/*
 * Copyright 2011-2019 The OTP authors
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

    changeSet(author: "borufka (generated)", id: "1553072107377-1") {
        addColumn(tableName: "seq_type") {
            column(name: "has_antibody_target", type: "boolean") {
                constraints(nullable: "true")
            }
        }
    }

    changeSet(author: "borufka", id: "make-anitbody-configureable-in-seqtype") {
        sqlFile(path: 'changelogs/2019/make-anitbody-configureable-in-seqtype.sql')
    }

    changeSet(author: "borufka (generated)", id: "1553076016879-105") {
        addNotNullConstraint(columnDataType: "boolean", columnName: "has_antibody_target", tableName: "seq_type")
    }

    changeSet(author: "borufka (generated)", id: "1553072107377-164") {
        createIndex(indexName: "seq_type__has_antibody_target_idx", tableName: "seq_type") {
            column(name: "has_antibody_target")
        }
    }
}
