/*
 * Copyright 2011-2025 The OTP authors
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
package de.dkfz.tbi.otp.infrastructure.alignment

import de.dkfz.tbi.otp.dataprocessing.ExternallyProcessedBamFile

import java.nio.file.Path

class ExternalAlignmentWorkFileService extends AbstractAlignmentWorkFileService<ExternallyProcessedBamFile> implements AbstractExternalAlignmentFileService {

    @Override
    Path getDirectoryPath(ExternallyProcessedBamFile bamFile) {
        if (getWorkFolder(bamFile)) {
            return filestoreService.getWorkFolderPath(bamFile.workflowArtefact.producedBy)
        }
        // This is needed, when no workFolder is defined and the data not in the new uuid structure
        return getNonOtpFolder(bamFile).resolve("analysisImport_${bamFile.referenceGenome}")
    }

    Path getNonOtpFolder(ExternallyProcessedBamFile bamFile) {
        if (bamFile.workflowArtefact?.producedBy?.workFolder) {
            return filestoreService.getWorkFolderPath(bamFile.workflowArtefact.producedBy)
        }
        // This is needed, when no workFolder is defined and the data not in the new uuid structure
        return abstractBamFileService.getBaseDirectory(bamFile).resolve("nonOTP")
    }
}
