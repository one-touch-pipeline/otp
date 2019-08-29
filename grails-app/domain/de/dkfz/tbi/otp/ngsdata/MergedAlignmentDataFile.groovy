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
package de.dkfz.tbi.otp.ngsdata

import de.dkfz.tbi.otp.utils.Entity

@Deprecated
class MergedAlignmentDataFile implements Entity {

    String fileSystem
    String filePath
    String fileName

    Date createdDate = new Date()
    Date fileSystemDate

    boolean fileExists = false
    boolean indexFileExists = false
    long fileSize = 0

    MergingLog mergingLog

    static belongsTo = [
            mergingLog: MergingLog,
    ]

    static constraints = {
        fileName()
        filePath()
        fileSystem()
        createdDate()
        fileSystemDate(nullable: true)
        mergingLog()
    }

    String fileSizeString() {
        if (fileSize > 1e9) {
            return String.format("%.2f GB", fileSize / 1e9)
        } else if (fileSize > 1e6) {
            return String.format("%.2f MB", fileSize / 1e6)
        } else if (fileSize > 1e3) {
            return String.format("%.2f kB", fileSize / 1e3)
        }
        return fileSize
    }

    static mapping = {
        mergingLog index: "merged_alignment_data_file_merging_log_idx"
    }
}
