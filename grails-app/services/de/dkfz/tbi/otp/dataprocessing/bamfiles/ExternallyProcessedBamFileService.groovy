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
package de.dkfz.tbi.otp.dataprocessing.bamfiles

import grails.gorm.transactions.Transactional

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.filestore.FilestoreService
import de.dkfz.tbi.otp.filestore.PathOption
import de.dkfz.tbi.otp.job.processing.FileSystemService
import de.dkfz.tbi.otp.project.ProjectService

import java.nio.file.FileSystem
import java.nio.file.Path

@Transactional
class ExternallyProcessedBamFileService<T extends AbstractBamFile> extends AbstractAbstractBamFileService<ExternallyProcessedBamFile> {

    AbstractBamFileService abstractBamFileService
    FilestoreService filestoreService
    ProjectService projectService
    FileSystemService fileSystemService

    Path getBamFile(ExternallyProcessedBamFile bamFile, PathOption... options) {
        return getImportFolder(bamFile, options).resolve(bamFile.bamFileName)
    }

    Path getBaiFile(ExternallyProcessedBamFile bamFile, PathOption... options) {
        return getImportFolder(bamFile, options).resolve(bamFile.baiFileName)
    }

    List<Path> getFurtherFiles(ExternallyProcessedBamFile bamFile, PathOption... options) {
        return bamFile.furtherFiles.collect {
            getImportFolder(bamFile, options).resolve(it)
        }
    }

    Path getBamMaxReadLengthFile(ExternallyProcessedBamFile bamFile, PathOption... options) {
        return getImportFolder(bamFile, options).resolve("${bamFile.bamFileName}.maxReadLength")
    }

    Path getNonOtpFolder(ExternallyProcessedBamFile bamFile, PathOption... options) {
        if (options.contains(PathOption.REAL_PATH) && bamFile.workflowArtefact.producedBy.workFolder) {
            return filestoreService.getWorkFolderPath(bamFile.workflowArtefact.producedBy)
        }
        return abstractBamFileService.getBaseDirectory(bamFile).resolve("nonOTP")
    }

    Path getImportFolder(ExternallyProcessedBamFile bamFile, PathOption... options) {
        if (options.contains(PathOption.REAL_PATH) && bamFile.workflowArtefact.producedBy.workFolder) {
            return filestoreService.getWorkFolderPath(bamFile.workflowArtefact.producedBy)
        }
        return getNonOtpFolder(bamFile, options).resolve("analysisImport_${bamFile.referenceGenome}")
    }

    Path getSourceBamFilePath(ExternallyProcessedBamFile bamFile) {
        FileSystem fs = fileSystemService.remoteFileSystem
        return fs.getPath(bamFile.importedFrom)
    }

    Path getSourceBaiFilePath(ExternallyProcessedBamFile bamFile) {
        return getSourceBaseDirFilePath(bamFile).resolve(bamFile.baiFileName)
    }

    Path getSourceBaseDirFilePath(ExternallyProcessedBamFile bamFile) {
        return getSourceBamFilePath(bamFile).parent
    }

    @Override
    Path getFinalInsertSizeFile(ExternallyProcessedBamFile bamFile, PathOption... options) {
        return getImportFolder(bamFile, options).resolve(bamFile.insertSizeFile)
    }

    @Override
    protected Path getPathForFurtherProcessingNoCheck(ExternallyProcessedBamFile bamFile) {
        return getBamFile(bamFile)
    }
}
