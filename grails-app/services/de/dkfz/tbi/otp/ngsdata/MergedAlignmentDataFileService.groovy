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

import de.dkfz.tbi.otp.dataprocessing.AbstractBamFileService

@Transactional
class MergedAlignmentDataFileService {
    /**
     * @deprecated can not handle {@link SeqType} with {@link AntibodyTarget} correctly, use {@link AbstractBamFileService#getBaseDirectory()} instead
     */
    @Deprecated
    static String buildRelativePath(SeqType type, Sample sample) {
        assert !type.hasAntibodyTarget
        String sampleType = sample.sampleType.dirName
        String layout = type.libraryLayoutDirName
        return "${sample.individual.getViewByPidPath(type).relativePath}/${sampleType}/${layout}/merged-alignment/"
    }
}
