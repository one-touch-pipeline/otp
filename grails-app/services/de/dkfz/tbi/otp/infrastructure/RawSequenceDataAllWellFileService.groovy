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
 * File Service containing the methods for retrieving the files and directories in the view by pid structure containing all well structure,
 * which is linked to the work directory, for rawSequenceFiles. This all well directory contains all the files of the well structure and a file for
 * mapping these files to the different singleCellWellLabels.
 *
 * This is only possible if a singleCellWellLabel is defined on the SeqTrack.
 */
@Slf4j
@Transactional
class RawSequenceDataAllWellFileService implements RawSequenceDataFileService {
    private static final String SINGLE_CELL_ALL_WELL = '0_all'

    IndividualService individualService
    RawSequenceDataViewFileService rawSequenceDataViewFileService

    @Override
    Path getDirectoryPath(RawSequenceFile rawSequenceFile) {
        Path basePath = getRunDirectoryPath(rawSequenceFile)
        // For historic reasons, vbpPath starts and ends with a slash.
        // Thus slashes are removed here, otherwise it would be interpreted as an absolute path by the resolve method.
        String vbpPath = Paths.get(rawSequenceFile.fileType.vbpPath).getName(0)
        return basePath.resolve(vbpPath)
    }

    Path getRunDirectoryPath(RawSequenceFile rawSequenceFile) {
        Path basePath = getAllWellDirectoryPath(rawSequenceFile)
        SeqTrack seqTrack = rawSequenceFile.seqTrack
        return basePath.resolve(seqTrack.seqType.libraryLayoutDirName).resolve(seqTrack.run.dirName)
    }

    Path getAllWellDirectoryPath(RawSequenceFile rawSequenceFile) {
        SeqTrack seqTrack = rawSequenceFile.seqTrack
        assert seqTrack.singleCellWellLabel && seqTrack.seqType.singleCell: 'To retrieve the all well structure a singleCellWellLabel has to be defined'
        Path basePath = rawSequenceDataViewFileService.getSampleTypeDirectoryPath(rawSequenceFile)
        return basePath.resolve(SINGLE_CELL_ALL_WELL)
    }

    @Override
    Path getFilePath(RawSequenceFile rawSequenceFile) {
        Path basePath = getDirectoryPath(rawSequenceFile)
        return basePath.resolve(rawSequenceFile.vbpFileName)
    }
}
