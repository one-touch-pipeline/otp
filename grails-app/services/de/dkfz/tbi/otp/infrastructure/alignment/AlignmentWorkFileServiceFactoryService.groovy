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
package de.dkfz.tbi.otp.infrastructure.alignment

import grails.gorm.transactions.Transactional

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.rnaAlignment.RnaRoddyBamFile
import de.dkfz.tbi.otp.dataprocessing.singleCell.SingleCellBamFile

@Transactional
class AlignmentWorkFileServiceFactoryService {

    ExternalAlignmentWorkFileService externalAlignmentWorkFileService
    PanCancerWorkFileService panCancerWorkFileService
    RnaAlignmentWorkFileService rnaAlignmentWorkFileService
    CellRangerWorkFileService cellRangerWorkFileService

    private Map serviceMap

    def <T extends AbstractBamFile> AbstractAlignmentWorkFileService<T> getService(Class<T> clazz) {
        AbstractAlignmentWorkFileService<? extends AbstractBamFile> result = map[clazz]
        if (!result) {
            throw new IllegalArgumentException("No service exists for ${clazz.simpleName}")
        }
        return result
    }

    def <T extends AbstractBamFile> AbstractAlignmentWorkFileService<T> getService(T instance) {
        return getService(instance.class)
    }

    private Map getMap() {
        serviceMap = serviceMap ?: Collections.unmodifiableMap([
                (ExternallyProcessedBamFile) : externalAlignmentWorkFileService,
                (ExternallyProcessedCramFile): externalAlignmentWorkFileService,
                (RoddyBamFile)               : panCancerWorkFileService,
                (RnaRoddyBamFile)            : rnaAlignmentWorkFileService,
                (SingleCellBamFile)          : cellRangerWorkFileService,
        ])
        return serviceMap
    }
}
