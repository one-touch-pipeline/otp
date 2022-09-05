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

    changeSet(author: "", id: "1662388090999-172") {
        dropForeignKeyConstraint(baseTableName: "acl_object_identity", constraintName: "fk2a2bb00970422cc5")
    }

    changeSet(author: "", id: "1662388090999-173") {
        dropForeignKeyConstraint(baseTableName: "acl_object_identity", constraintName: "fk2a2bb00990ec1949")
    }

    changeSet(author: "", id: "1662388090999-174") {
        dropForeignKeyConstraint(baseTableName: "acl_object_identity", constraintName: "fk2a2bb009a50290b8")
    }

    changeSet(author: "", id: "1662388090999-175") {
        dropForeignKeyConstraint(baseTableName: "acl_entry", constraintName: "fk5302d47d8fdb88d5")
    }

    changeSet(author: "", id: "1662388090999-176") {
        dropForeignKeyConstraint(baseTableName: "acl_entry", constraintName: "fk5302d47db0d9dc4d")
    }

    changeSet(author: "", id: "1662388090999-185") {
        dropTable(tableName: "acl_class")
    }

    changeSet(author: "", id: "1662388090999-186") {
        dropTable(tableName: "acl_entry")
    }

    changeSet(author: "", id: "1662388090999-187") {
        dropTable(tableName: "acl_object_identity")
    }

    changeSet(author: "", id: "1662388090999-188") {
        dropTable(tableName: "acl_sid")
    }
}
