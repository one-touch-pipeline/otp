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

import de.dkfz.tbi.otp.dataprocessing.singleCell.SingleCellService
import de.dkfz.tbi.otp.infrastructure.RawSequenceDataAllWellFileService
import de.dkfz.tbi.otp.infrastructure.RawSequenceDataViewFileService
import de.dkfz.tbi.otp.infrastructure.RawSequenceDataWorkFileService
import de.dkfz.tbi.otp.ngsdata.RawSequenceFile

import java.nio.file.Path

List<RawSequenceFile> rawSequenceFiles = RawSequenceFile.createCriteria().list {
    seqTrack {
        isNotNull('singleCellWellLabel')
        ne('singleCellWellLabel', '')
    }
} as List<RawSequenceFile>

SingleCellService singleCellService = ctx.singleCellService
RawSequenceDataWorkFileService rawSequenceDataWorkFileService = ctx.rawSequenceDataWorkFileService
RawSequenceDataViewFileService rawSequenceDataViewFileService = ctx.rawSequenceDataViewFileService
RawSequenceDataAllWellFileService rawSequenceDataAllWellFileService = ctx.rawSequenceDataAllWellFileService

println "rawSequenceFiles with well label: ${rawSequenceFiles.size()}"

println rawSequenceFiles.collect { RawSequenceFile rawSequenceFile ->
    Path newPath = rawSequenceDataViewFileService.getFilePath(rawSequenceFile)
    String oldPath = newPath.toString().replace(rawSequenceFile.seqTrack.singleCellWellLabel, '').replace('//', '/')
    Path wellPath = rawSequenceDataAllWellFileService.getFilePath(rawSequenceFile)
    Path source = rawSequenceDataWorkFileService.getFilePath(rawSequenceFile)
    Path mappingFile = singleCellService.singleCellMappingFile(rawSequenceFile)

    [
            '-------------------------------',
            "#${rawSequenceFile} of ${rawSequenceFile.seqTrack.toString().replace('<br>', ' ')}",
            "mkdir -m 2750 -p ${newPath.parent}",
            "mkdir -m 2750 -p ${wellPath.parent}",
            "rm ${oldPath} ",
            "ln -s ${source} ${newPath}",
            "ln -s ${source} ${wellPath}",
            "touch ${mappingFile}",
            "echo ${rawSequenceFile.fileName} >> ${mappingFile}",
    ].join('\n')
}.join('\n\n\n')

''
