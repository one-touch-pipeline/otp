/*
 * Copyright 2011-2026 The OTP authors
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
package de.dkfz.tbi.otp.infrastructure.fastqc

import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j

import de.dkfz.tbi.otp.dataprocessing.FastqcProcessedFile
import de.dkfz.tbi.otp.infrastructure.RawSequenceDataViewFileService

import java.nio.file.Path

/**
 * File Service containing the methods for retrieving the files and directories in the view by pid structure,
 * which is linked to the work directory, for fastqc files.
 */
@Slf4j
@Transactional
class FastqcLinkFileService implements AbstractFastqcFileService {

    RawSequenceDataViewFileService rawSequenceDataViewFileService

    @Override
    Path getDirectoryPath(FastqcProcessedFile fastqcProcessedFile) {
        Path baseString = rawSequenceDataViewFileService.getRunDirectoryPath(fastqcProcessedFile.sequenceFile)
        return baseString.resolve(FAST_QC_DIRECTORY_PART).resolve(fastqcProcessedFile.workDirectoryName)
    }

    @Override
    Path fastqcOutputPath(FastqcProcessedFile fastqcProcessedFile) {
        String fileName = fastqcFileName(fastqcProcessedFile)
        return getDirectoryPath(fastqcProcessedFile).resolve(fileName)
    }
}
