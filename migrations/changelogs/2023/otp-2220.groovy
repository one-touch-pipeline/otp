/*
 * Copyright 2011-2024 The OTP authors
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
    changeSet(author: "strubelp", id: "1698262206515-1") {
        sql("""
            DROP VIEW IF EXISTS aggregate_sequences;
            DROP VIEW IF EXISTS sequences;
        """)
    }
    changeSet(author: "strubelp", id: "1698262206515-2") {
        sql("""
                DELETE FROM processing_option WHERE name='REALM_DEFAULT_VALUE';
        """)
    }
    changeSet(author: "strubelp", id: "1698262206515-96") {
        dropForeignKeyConstraint(baseTableName: "project", constraintName: "project_realm_id_fkey")
    }
    changeSet(author: "strubelp", id: "1698262206515-110") {
        dropUniqueConstraint(constraintName: "unique_realm_name_constraint", tableName: "realm")
    }
    changeSet(author: "strubelp", id: "1698262206515-111") {
        dropTable(tableName: "realm")
    }
    changeSet(author: "strubelp", id: "1698262206515-128") {
        dropColumn(columnName: "realm_id", tableName: "project")
    }
}
