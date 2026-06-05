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
package de.dkfz.tbi.otp.ngsdata

import grails.gorm.transactions.Transactional
import groovy.transform.CompileDynamic

import de.dkfz.tbi.otp.infrastructure.FileService
import de.dkfz.tbi.otp.ngsdata.referencegenome.ReferenceGenomeService
import de.dkfz.tbi.otp.utils.CollectionUtils

import java.nio.file.Path

import static org.springframework.util.Assert.notNull

@Transactional
class BedFileService {

    ReferenceGenomeService referenceGenomeService

    FileService fileService

    private final static String TARGET_REGIONS_DIR = "targetRegions"

    /**
     * @return absolute path to the given {@link BedFile}
     */
    Path filePath(BedFile bedFile, boolean checkExistence = true) {
        notNull(bedFile, "bedFile must not be null")
        Path refGenomePath = referenceGenomeService.referenceGenomeDirectory(bedFile.referenceGenome, checkExistence)
        Path bedFilePath = refGenomePath.resolve(TARGET_REGIONS_DIR).resolve(bedFile.fileName)
        if (!checkExistence || fileService.fileIsReadable(bedFilePath)) {
            return bedFilePath
        }
        throw new FileNotReadableException("the bedFile can not be read: ${bedFilePath}")
    }

    @CompileDynamic
    BedFile findBedFileByReferenceGenomeAndLibraryPreparationKit(ReferenceGenome referenceGenome, LibraryPreparationKit libraryPreparationKit) {
        return CollectionUtils.atMostOneElement(BedFile.findAllByReferenceGenomeAndLibraryPreparationKit(
                referenceGenome,
                libraryPreparationKit
        ))
    }
}
