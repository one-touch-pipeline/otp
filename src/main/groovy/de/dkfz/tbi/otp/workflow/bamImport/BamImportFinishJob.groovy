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
package de.dkfz.tbi.otp.workflow.bamImport

import groovy.transform.CompileDynamic
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile
import de.dkfz.tbi.otp.dataprocessing.ExternallyProcessedBamFile
import de.dkfz.tbi.otp.dataprocessing.bamfiles.ExternallyProcessedBamFileService
import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.job.processing.FileSystemService
import de.dkfz.tbi.otp.ngsdata.ChecksumFileService
import de.dkfz.tbi.otp.workflow.jobs.AbstractFinishJob
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep
import java.nio.file.*

@Component
@Slf4j
class BamImportFinishJob extends AbstractFinishJob implements BamImportShared {

    @Autowired
    ExternallyProcessedBamFileService externallyProcessedBamFileService

    @Autowired
    FileService fileService

    @Autowired
    ChecksumFileService checksumFileService

    @Autowired
    FileSystemService fileSystemService

    @CompileDynamic
    @Override
    void updateDomains(WorkflowStep workflowStep) {
        ExternallyProcessedBamFile bamFile = getBamFile(workflowStep)
        FileSystem fileSystem = fileSystemService.remoteFileSystem
        Path bamFilePath = externallyProcessedBamFileService.getBamFile(bamFile)

        if (!bamFile.maximumReadLength) {
            Path bamMaxReadLengthFile = externallyProcessedBamFileService.getBamMaxReadLengthFile(bamFile)
            fileService.ensureFileIsReadableAndNotEmpty(bamMaxReadLengthFile)
            bamFile.maximumReadLength = bamMaxReadLengthFile.text as Integer
        }

        if (!bamFile.md5sum) {
            Path md5Path = fileSystem.getPath(checksumFileService.md5FileName(bamFilePath.toString()))
            fileService.ensureFileIsReadableAndNotEmpty(md5Path)
            bamFile.md5sum = checksumFileService.firstMD5ChecksumFromFile(md5Path)
        }

        bamFile.fileOperationStatus = AbstractBamFile.FileOperationStatus.PROCESSED
        bamFile.fileSize = Files.size(bamFilePath)
        bamFile.save(flush: true)
        bamFile.workPackage.bamFileInProjectFolder = bamFile
        bamFile.workPackage.save(flush: true)
    }
}
