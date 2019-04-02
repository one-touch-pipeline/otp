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

package de.dkfz.tbi.otp.fileSystemConsistency

import de.dkfz.tbi.otp.ngsdata.DataFile
import de.dkfz.tbi.otp.utils.Entity

/**
 * Class representing the status of a DataFile after a consistency check is performed.
 * Only not consistent status should be stored.
 */
class ConsistencyStatus implements Entity {

    enum Status {
        NO_FILE,                // path is not null but file does not exist
        NO_READ_PERMISSION,     // file exists but cannot be read
        VIEW_BY_PID_NO_FILE,    // path from view by pid is not linked to the correct file.
        SIZE_DIFFERENCE,        // file exists but the size of the file in database is different
        CONSISTENT,
    }

    /**
     * Consistency Status
     */
    Status status

    /**
     * Inconsistency resolution date
     */
    Date resolvedDate

    static constraints = {
        resolvedDate(nullable: true)
    }

    static belongsTo = [
        consistencyCheck: ConsistencyCheck,
        dataFile: DataFile,
    ]
}
