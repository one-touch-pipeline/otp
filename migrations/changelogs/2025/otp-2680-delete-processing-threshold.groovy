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

    changeSet(author: "-", id: "1762269610203-94") {
        dropForeignKeyConstraint(baseTableName: "processing_thresholds", constraintName: "FK8o7rmi93crkr1h2au59gx2u3p")
    }

    changeSet(author: "-", id: "1762269610203-95") {
        dropForeignKeyConstraint(baseTableName: "processing_thresholds", constraintName: "FKb1j34pggqajkqbuch7ybw2su9")
    }

    changeSet(author: "-", id: "1762269610203-104") {
        dropForeignKeyConstraint(baseTableName: "processing_thresholds", constraintName: "sample_type_FK")
    }

    changeSet(author: "-", id: "1762269610203-111") {
        dropUniqueConstraint(constraintName: "UKb7a01d2be3fdbf83d79c75252adb", tableName: "processing_thresholds")
    }

    changeSet(author: "-", id: "1762269610203-117") {
        dropTable(tableName: "processing_thresholds")
    }
}
