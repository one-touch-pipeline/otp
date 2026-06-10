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
    changeSet(author: "-", id: "otp-2837-populate-job-started-finished-for-old-entries-1") {

        // Terminated steps: set both columns from the record's own timestamps.
        // RUNNING steps: set only job_started; job_finished left NULL to be filled automatically.
        // All other states (CREATED etc.): left untouched.
        sql("""
            UPDATE workflow_step
            SET job_started  = date_created,
                job_finished = last_updated
            WHERE state IN ('SKIPPED', 'SUCCESS', 'FAILED')
              AND job_started IS NULL;
        """)

        sql("""
            UPDATE workflow_step
            SET job_started = date_created
            WHERE state = 'RUNNING'
              AND job_started IS NULL;
        """)

        // Terminated runs: set both columns from the record's own timestamps.
        // RUNNING_WES runs: set only first_job_started; last_job_finished left NULL to be filled automatically.
        // All other states (PENDING, WAITING_FOR_USER, RUNNING_OTP, FAILED etc.): left untouched.
        sql("""
            UPDATE workflow_run
            SET first_job_started = date_created,
                last_job_finished  = last_updated
            WHERE state IN ('SKIPPED_MISSING_PRECONDITION', 'SUCCESS', 'FAILED_FINAL',
                            'FAILED_WAITING', 'RESTARTED', 'KILLED', 'LEGACY')
              AND first_job_started IS NULL;
        """)

        sql("""
            UPDATE workflow_run
            SET first_job_started = date_created
            WHERE state IN ('RUNNING_WES', 'WAITING_FOR_USER', 'RUNNING_OTP', 'FAILED')
              AND first_job_started IS NULL;
        """)
    }
}
