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

import grails.gorm.transactions.Transactional

import de.dkfz.tbi.otp.fileSystemConsistency.ConsistencyStatus.Status
import de.dkfz.tbi.otp.ngsdata.DataFile
import de.dkfz.tbi.otp.ngsdata.LsdfFilesService

/**
 * Service to check for consistency of Database and LSDF
 */
@Transactional
class ConsistencyService {

    LsdfFilesService lsdfFilesService

    /**
     * Retrieves the consistency status of the DataFile
     *
     * @return Consistency status of the DataFile
     */
    Status checkStatus(DataFile dataFile) {
        String path = lsdfFilesService.getFileFinalPath(dataFile)
        // if there is no path to the file the status will be considered consistent
        if (!path) {
            return Status.CONSISTENT
        }
        // hack to not evaluate consistency at these files. (will be considered as consistent)
        if (path.contains("_export.txt.gz") ||
            path.contains("_export.txt.tar.bz2")) {
            return Status.CONSISTENT
        }
        File file = new File(path)
        if (!file.exists()) {
            return Status.NO_FILE
        }
        if (!file.canRead()) {
            return Status.NO_READ_PERMISSION
        }
        File viewByPidFile = new File(lsdfFilesService.getFileViewByPidPath(dataFile))
        if (viewByPidFile.canonicalPath != file.canonicalPath) {
            return Status.VIEW_BY_PID_NO_FILE
        }
        if (file.size() != dataFile.fileSize) {
            return Status.SIZE_DIFFERENCE
        }
        return Status.CONSISTENT
    }
}
