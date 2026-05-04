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
    changeSet(author: "tirtaram", id: "otp-2951-1") {
        addColumn(tableName: "project_request_persistent_state") {
            column(name: "approval_round_started_at", type: "DATE") {
                constraints(nullable: true)
            }
        }
    }
    changeSet(author: "tirtaram", id: "otp-2951-2") {
        // Backfill: for requests currently in Approval state, use date_created as a best approximation.
        // For first-round approvals this is accurate. For re-entered approvals, date_created predates
        // the current round — no better source exists in the DB for these rows. Any such request
        // retains an approximate timestamp until it completes and re-enters Approval after deployment.
        sql("""
            UPDATE project_request_persistent_state
            SET approval_round_started_at = date_created
            WHERE bean_name = 'approval'
        """)
    }
}
