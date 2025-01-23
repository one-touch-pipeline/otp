/*
 * Copyright 2011-2025 The OTP authors
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

import de.dkfz.tbi.otp.dataprocessing.*

import java.nio.file.Path

abstract class AbstractAlignmentLinkFileService<T extends AbstractBamFile> implements ArtefactFileService<T> {

    AbstractBamFileService abstractBamFileService

    Path getPathForFurtherProcessing(T bamFile) {
        bamFile.mergingWorkPackage.refresh() // Sometimes the mergingWorkPackage.processableBamFileInProjectFolder is empty but should have a value
        AbstractBamFile processableBamFileInProjectFolder = bamFile.mergingWorkPackage.processableBamFileInProjectFolder
        if (bamFile.id == processableBamFileInProjectFolder?.id) {
            return getPathForFurtherProcessingNoCheck(bamFile)
        }
        throw new IllegalStateException("This BAM file is not in the project folder or not processable.\n" +
                "this: ${bamFile}\nprocessableBamFileInProjectFolder: ${processableBamFileInProjectFolder}")
    }

    abstract protected Path getPathForFurtherProcessingNoCheck(T bamFile)

    abstract Path getInsertSizeFile(T bamFile)
}
