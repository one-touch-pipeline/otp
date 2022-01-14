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
package de.dkfz.tbi.otp.ngsdata

import grails.gorm.transactions.Transactional

@Transactional
class FileTypeService {

    boolean isGoodSequenceDataFile(DataFile dataFile) {
        if (dataFile.fileWithdrawn) {
            return false
        }
        return isSequenceDataFile(dataFile)
    }

    boolean isSequenceDataFile(DataFile dataFile) {
        return dataFile.fileType.type == FileType.Type.SEQUENCE
    }

    List<FileType> alignmentSequenceTypes() {
        List<String> extensions = ["bam", "bwt"]
        return FileType.findAllByTypeAndSubTypeInList(FileType.Type.ALIGNMENT, extensions)
    }

    /**
     * Provides object from file name and known type
     *
     * to make a difference between 'stats' from sequence and alignment
     *
     * @return FileType
     */
    static FileType getFileType(String filename, FileType.Type type) {
        List<FileType> types = FileType.findAllByType(type, [sort: "id", order: "asc"])
        for (FileType subType in types) {
            if (filename.contains(subType.signature)) {
                return subType
            }
        }
        throw new FileTypeUndefinedException(filename)
    }
}
