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
package de.dkfz.tbi.otp.infrastructure

import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j

import de.dkfz.tbi.otp.filestore.FilestoreService
import de.dkfz.tbi.otp.ngsdata.LsdfFilesService
import de.dkfz.tbi.otp.ngsdata.RawSequenceFile

import java.nio.file.Path

/**
 * File Service containing the methods for retrieving the files and directories in the work path for rawSequenceFiles.
 */
@Slf4j
@Transactional
class RawSequenceDataWorkFileService implements RawSequenceDataFileService {
    FilestoreService filestoreService
    LsdfFilesService lsdfFilesService // This service can be removed when all projects are migrated to the new uuid structure

    @Override
    Path getDirectoryPath(RawSequenceFile rawSequenceFile) {
        if (rawSequenceFile.seqTrack.workflowArtefact?.producedBy?.workFolder) {
            return filestoreService.getWorkFolderPath(rawSequenceFile.seqTrack.workflowArtefact.producedBy)
        }
        // This is needed, when no workFolder is defined and the seqTrack not in the new uuid structure
        return lsdfFilesService.getSeqCenterRunDirectory(rawSequenceFile)?.resolve(rawSequenceFile.pathName)
    }

    /**
     * Important function.
     * This function knows all naming conventions and data organization
     *
     * @return String with path or null if path can not be established
     */
    @Override
    Path getFilePath(RawSequenceFile rawSequenceFile) {
        return getDirectoryPath(rawSequenceFile)?.resolve(rawSequenceFile?.fileName)
    }

    Path getMd5sumPath(RawSequenceFile rawSequenceFile) {
        return getFilePath(rawSequenceFile)?.resolveSibling(rawSequenceFile.fileName.concat(".md5sum"))
    }
}
