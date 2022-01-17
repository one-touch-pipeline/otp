/*
 * Copyright 2011-2021 The OTP authors
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

import java.nio.file.Path

import static de.dkfz.tbi.otp.dataprocessing.AbstractMergedBamFile.QcTrafficLightStatus

@Transactional
abstract class AbstractAbstractMergedBamFileService<T extends AbstractMergedBamFile> {

    abstract Path getFinalInsertSizeFile(T bamFile)
    protected abstract Path getPathForFurtherProcessingNoCheck(T bamFile)

    Path getPathForFurtherProcessing(T bamFile) {
        if (bamFile.qcTrafficLightStatus in [QcTrafficLightStatus.REJECTED, QcTrafficLightStatus.BLOCKED]) {
            return null
        }
        bamFile.mergingWorkPackage.refresh() //Sometimes the mergingWorkPackage.processableBamFileInProjectFolder is empty but should have a value
        AbstractMergedBamFile processableBamFileInProjectFolder = bamFile.mergingWorkPackage.processableBamFileInProjectFolder
        if (bamFile.id == processableBamFileInProjectFolder?.id) {
            return getPathForFurtherProcessingNoCheck(bamFile)
        }
        throw new IllegalStateException("This BAM file is not in the project folder or not processable.\n" +
                "this: ${bamFile}\nprocessableBamFileInProjectFolder: ${processableBamFileInProjectFolder}")
    }
}
