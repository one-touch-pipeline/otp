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

package de.dkfz.tbi.otp.job.processing

import de.dkfz.tbi.otp.config.ConfigService
import de.dkfz.tbi.otp.ngsdata.LsdfFilesService
import de.dkfz.tbi.otp.ngsdata.Realm

class ClusterJobLoggingService {

    final static CLUSTER_LOG_BASE_DIR = 'clusterLog'

    LsdfFilesService lsdfFilesService

    static File logDirectory(ProcessingStep processingStep) {
        assert processingStep: 'No processing step specified.'

        Date date = processingStep.firstProcessingStepUpdate.date
        String dateDirectory = date.format('yyyy-MM-dd')
        return new File("${ConfigService.getInstance().getLoggingRootPath()}/${CLUSTER_LOG_BASE_DIR}/${dateDirectory}")
    }

    File createAndGetLogDirectory(Realm realm, ProcessingStep processingStep) {
        assert realm: 'No realm specified.'
        File logDirectory = logDirectory(processingStep)
        if (!logDirectory.exists()) {
            //race condition between threads and within NFS can be ignored, since the command 'mkdir --parent' does not fail if the directory already exists.
            lsdfFilesService.createDirectory(logDirectory, realm)
            LsdfFilesService.ensureDirIsReadable(logDirectory)
        }
        return logDirectory
    }
}
