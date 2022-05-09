/*
 * Copyright 2011-2020 The OTP authors
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
package de.dkfz.tbi.otp.dataprocessing.singleCell

import grails.gorm.transactions.Transactional

import de.dkfz.tbi.otp.ngsdata.*

import java.nio.file.Path

@Transactional
class SingleCellService {

    static final String MAPPING_FILE_SUFFIX = 'mapping.tsv'

    LsdfFilesService lsdfFilesService

    String buildMappingFileName(Individual individual, SampleType sampleType) {
        return "${individual.pid}_${sampleType.name}_${MAPPING_FILE_SUFFIX}"
    }

    String buildMappingFileName(DataFile dataFile) {
        Sample sample = dataFile.seqTrack.sample
        return buildMappingFileName(sample.individual, sample.sampleType)
    }

    Path singleCellMappingFile(DataFile dataFile) {
        return lsdfFilesService.getSingleCellWellDirectory(dataFile).resolve(buildMappingFileName(dataFile))
    }

    /**
     * Defines the structure of a mapping entry inside the mapping file.
     *
     * When changing this you will have to recreate all existing files after the change is released.
     *
     * @param dataFile to build an entry for
     * @return an entry in the expected format for the mapping file
     */
    String mappingEntry(DataFile dataFile) {
        return "${lsdfFilesService.getFileViewByPidPath(dataFile)}\t${dataFile.seqTrack.singleCellWellLabel}"
    }
}
