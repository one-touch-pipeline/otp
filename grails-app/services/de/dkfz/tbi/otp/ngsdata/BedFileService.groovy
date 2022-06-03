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

import de.dkfz.tbi.otp.ngsdata.referencegenome.ReferenceGenomeService

import static org.springframework.util.Assert.notNull

@Transactional
class BedFileService {

    ReferenceGenomeService referenceGenomeService
    private final static String TARGET_REGIONS_DIR = "targetRegions"

    /**
     * @return absolute path to the given {@link BedFile}
     * at given {@link Realm}
     */
    String filePath(BedFile bedFile, boolean checkExistence = true) {
        notNull(bedFile, "bedFile must not be null")
        String refGenomePath = referenceGenomeService.referenceGenomeDirectory(bedFile.referenceGenome, checkExistence).path
        String bedFilePath = "${refGenomePath}/${TARGET_REGIONS_DIR}/${bedFile.fileName}"
        File file = new File(bedFilePath)
        if (!checkExistence || file.canRead()) {
            return bedFilePath
        }
        throw new RuntimeException("the bedFile can not be read: ${bedFilePath}")
    }
}
