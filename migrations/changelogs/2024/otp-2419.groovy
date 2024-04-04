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

    changeSet(author: "Julian Rausch", id: "otp-2419") {
        sql("""
               UPDATE workflow_step
                SET state = 'SKIPPED'
                WHERE state='OMITTED';
            """)
        sql("""
               UPDATE workflow_run
                SET state = 'SKIPPED_MISSING_PRECONDITION'
                WHERE state='OMITTED_MISSING_PRECONDITION';
            """)
        sql("""
               UPDATE workflow_artefact
                SET state = 'SKIPPED'
                WHERE state='OMITTED';
            """)
    }

    changeSet(author: "Julian Rausch", id: "1711636381583-91") {
        renameTable(oldTableName: "omitted_message", newTableName: "workflow_step_skip_message")
    }
    changeSet(author: "-", id: "1711636381583-92") {
        renameColumn(tableName: "workflow_run", oldColumnName:"omitted_message_id", newColumnName:"skip_message_id" )
    }
}
