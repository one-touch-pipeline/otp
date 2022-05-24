/*
 * Copyright 2011-2022 The OTP authors
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

import grails.test.hibernate.HibernateSpec

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.domainFactory.FastqcDomainFactory
import de.dkfz.tbi.otp.ngsdata.DataFile
import de.dkfz.tbi.otp.ngsdata.SeqTrack

class FastqcProcessedFileSpec extends HibernateSpec implements FastqcDomainFactory {

    @Override
    List<Class> getDomainClasses() {
        return [
                FastqcProcessedFile,
        ]
    }

    void "validate, when workDirectoryName has the same value for an SeqTrack, then validate succeeds"() {
        given:
        String workDir = "workdir"
        SeqTrack seqTrack = createSeqTrack()
        DataFile dataFile1 = createDataFile([seqTrack: seqTrack])
        DataFile dataFile2 = createDataFile([seqTrack: seqTrack])

        when:
        createFastqcProcessedFile(dataFile: dataFile1, workDirectoryName: workDir)
        createFastqcProcessedFile(dataFile: dataFile2, workDirectoryName: workDir)

        then:
        noExceptionThrown()
    }

    void "validate, when workDirectoryName has different value for an SeqTrack, then validate fails"() {
        given:
        String workDir = "workdir"
        String otherWorkDir = "otherWorkDir"

        SeqTrack seqTrack = createSeqTrack()
        DataFile dataFile1 = createDataFile([seqTrack: seqTrack])
        DataFile dataFile2 = createDataFile([seqTrack: seqTrack])
        createFastqcProcessedFile(dataFile: dataFile1, workDirectoryName: workDir)

        when:
        FastqcProcessedFile fastqcProcessedFile2 = createFastqcProcessedFile(dataFile: dataFile2, workDirectoryName: otherWorkDir, false)

        then:
        TestCase.assertValidateError(fastqcProcessedFile2, "workDirectoryName", "fastqcProcessedFile.workDirectoryName.differ")
    }
}
