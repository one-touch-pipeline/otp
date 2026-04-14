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
package de.dkfz.tbi.otp.infrastructure.fastqc

import grails.testing.gorm.DataTest
import spock.lang.Specification

import de.dkfz.tbi.otp.dataprocessing.FastqcProcessedFile
import de.dkfz.tbi.otp.domainFactory.FastqcDomainFactory
import de.dkfz.tbi.otp.infrastructure.RawSequenceDataViewFileService
import de.dkfz.tbi.otp.ngsdata.FastqFile

import java.nio.file.Path

class FastqcLinkFileServiceSpec extends Specification implements DataTest, FastqcDomainFactory {

    @Override
    Class<?>[] getDomainClassesToMock() {
        return [
                FastqFile,
                FastqcProcessedFile,
        ]
    }

    void "getDirectoryPath, when called, return path in project"() {
        given:
        Path rawSequencePath = Path.of("/tmp/rawsequence")
        FastqcProcessedFile fastqcProcessedFile = createFastqcProcessedFile()
        FastqcLinkFileService fastqcLinkFileService = new FastqcLinkFileService([
                rawSequenceDataViewFileService: Mock(RawSequenceDataViewFileService) {
                    1 * getRunDirectoryPath(fastqcProcessedFile.sequenceFile) >> rawSequencePath
                    0 * _
                }
        ])
        Path expected = rawSequencePath.resolve(FastqcLinkFileService.FAST_QC_DIRECTORY_PART).resolve(fastqcProcessedFile.workDirectoryName)

        when:
        Path path = fastqcLinkFileService.getDirectoryPath(fastqcProcessedFile)

        then:
        path == expected
    }

    void "fastqcOutputPath, when called, then return correct path"() {
        given:
        Path rawSequencePath = Path.of("/tmp/rawsequence")
        FastqcProcessedFile fastqcProcessedFile = createFastqcProcessedFile()
        FastqcLinkFileService fastqcLinkFileService = new FastqcLinkFileService([
                rawSequenceDataViewFileService: Mock(RawSequenceDataViewFileService) {
                    1 * getRunDirectoryPath(fastqcProcessedFile.sequenceFile) >> rawSequencePath
                    0 * _
                }
        ])

        and:
        String fastqcName = fastqcLinkFileService.fastqcFileName(fastqcProcessedFile)
        Path expected = rawSequencePath.resolve(FastqcLinkFileService.FAST_QC_DIRECTORY_PART).
                resolve(fastqcProcessedFile.workDirectoryName).
                resolve(fastqcName)

        expect:
        fastqcLinkFileService.fastqcOutputPath(fastqcProcessedFile) == expected
    }
}
