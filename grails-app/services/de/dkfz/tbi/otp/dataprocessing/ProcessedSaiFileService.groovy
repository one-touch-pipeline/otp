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

package de.dkfz.tbi.otp.dataprocessing

import grails.gorm.transactions.Transactional

import de.dkfz.tbi.otp.ngsdata.SavingException
import de.dkfz.tbi.otp.ngsdata.SeqTrack

@Transactional
class ProcessedSaiFileService {

    DataProcessingFilesService dataProcessingFilesService
    ProcessedAlignmentFileService processedAlignmentFileService

    String getFilePath(ProcessedSaiFile saiFile) {
        String dir = getDirectory(saiFile)
        String filename = getFileName(saiFile)
        return "${dir}/${filename}"
    }

    /**
     * Retrieves the path to a log file used by bwa aln
     * (Although is not Philosophy of OTP to keep track of log files,
     * it is required by bwa since it produces not empty output files
     * even when it fails, and so we need to analyse the log file contents too)
     *
     * @param saiFile processed sai file object
     * @return Path to the outputted error file produced by bwa aln
     */
    String bwaAlnErrorLogFilePath(ProcessedSaiFile saiFile) {
        return "${getFilePath(saiFile)}_bwaAlnErrorLog.txt"
    }

    String getDirectory(ProcessedSaiFile saiFile) {
        return processedAlignmentFileService.getDirectory(saiFile.alignmentPass)
    }

    String getFileName(ProcessedSaiFile saiFile) {
        SeqTrack seqTrack = saiFile.alignmentPass.seqTrack
        String sampleType = seqTrack.sample.sampleType.dirName
        String runName = seqTrack.run.name
        String filename = saiFile.dataFile.fileName
        filename = filename.substring(0, filename.lastIndexOf("."))
        return "${sampleType}_${runName}_${filename}.sai"
    }

}
