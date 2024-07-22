/*
 * Copyright 2011-2024 The OTP authors
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

import de.dkfz.tbi.otp.ngsdata.*

import java.nio.file.Path
import java.nio.file.Paths

/**
 * File Service containing the methods for retrieving the files and directories in the view by pid structure,
 * which is linked to the work directory, for rawSequenceFiles.
 * If a singleCellWellLabel is defined on the SeqTrack another layer is added for the well structure.
 */
@Slf4j
@Transactional
class RawSequenceDataViewFileService implements RawSequenceDataFileService {
    IndividualService individualService

    @Override
    Path getDirectoryPath(RawSequenceFile rawSequenceFile) {
        Path basePath = getRunDirectoryPath(rawSequenceFile)
        // For historic reasons, vbpPath starts and ends with a slash.
        // Thus slashes are removed here, otherwise it would be interpreted as an absolute path by the resolve method.
        String vbpPath = Paths.get(rawSequenceFile.fileType.vbpPath).getName(0)
        return basePath.resolve(vbpPath)
    }

    Path getRunDirectoryPath(RawSequenceFile rawSequenceFile) {
        Path basePath = getSingleCellWellDirectoryPath(rawSequenceFile)
        SeqTrack seqTrack = rawSequenceFile.seqTrack
        return basePath.resolve(seqTrack.seqType.libraryLayoutDirName).resolve(seqTrack.run.dirName)
    }

    Path getSingleCellWellDirectoryPath(RawSequenceFile rawSequenceFile) {
        Path basePath = getSampleTypeDirectoryPath(rawSequenceFile)
        SeqTrack seqTrack = rawSequenceFile.seqTrack
        if (seqTrack.singleCellWellLabel && seqTrack.seqType.singleCell) {
            return basePath.resolve(seqTrack.singleCellWellLabel)
        }
        return basePath
    }

    /**
     * Attention: In most cases the method {@link #getSingleCellWellDirectoryPath(RawSequenceFile)} is to use
     * instead of this one to include the well label level.
     */
    Path getSampleTypeDirectoryPath(RawSequenceFile rawSequenceFile) {
        Path basePath = individualService.getViewByPidPath(rawSequenceFile.individual, rawSequenceFile.seqType)
        SeqTrack seqTrack = rawSequenceFile.seqTrack
        String antiBodyTarget = seqTrack.seqType.hasAntibodyTarget ? "-${seqTrack.antibodyTarget.name}" : ""
        return basePath.resolve("${seqTrack.sample.sampleType.dirName}${antiBodyTarget}")
    }

    @Override
    Path getFilePath(RawSequenceFile rawSequenceFile) {
        Path basePath = getDirectoryPath(rawSequenceFile)
        return basePath.resolve(rawSequenceFile.vbpFileName)
    }
}
