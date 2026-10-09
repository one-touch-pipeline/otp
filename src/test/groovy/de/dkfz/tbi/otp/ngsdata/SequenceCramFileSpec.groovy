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

import grails.testing.gorm.DataTest
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.project.Project

class SequenceCramFileSpec extends Specification implements DataTest, DomainFactoryCore {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                FastqFile,
                SequenceCramFile,
                FileType,
                ReferenceGenome,
                Individual,
                Project,
                Run,
                Sample,
                SampleType,
                SeqCenter,
                SeqPlatform,
                SeqPlatformGroup,
                SeqTrack,
                SeqType,
                SoftwareTool,
        ]
    }

    @Unroll
    void "validate, when CRAM file has mateNumber #mateNumber and source fastq files of mates #sourceMates, then it should be valid"() {
        given:
        SeqTrack seqTrack = createSeqTrack([seqType: createSeqType([libraryLayout: SequencingReadType.PAIRED])])
        SequenceCramFile cramFile = createSequenceCramFile([
                seqTrack        : seqTrack,
                mateNumber      : mateNumber,
                sourceFastqFiles: sourceMates.collect { createFastqFile([seqTrack: seqTrack, mateNumber: it]) } as Set,
        ], false)

        expect:
        cramFile.validate()

        where:
        mateNumber | sourceMates
        null       | []
        null       | [1, 2]
        1          | []
        1          | [1]
        2          | [2]
    }

    void "validate, when source fastq files belong to the same mate, should fail"() {
        given:
        SeqTrack seqTrack = createSeqTrack([seqType: createSeqType([libraryLayout: SequencingReadType.PAIRED])])
        Set<FastqFile> sourceFastqFiles = [createFastqFile([seqTrack: seqTrack]), createFastqFile([seqTrack: seqTrack])] as Set
        SequenceCramFile cramFile = createSequenceCramFile([seqTrack: seqTrack, sourceFastqFiles: sourceFastqFiles], false)

        expect:
        TestCase.assertValidateError(cramFile, "sourceFastqFiles", "duplicateMate", sourceFastqFiles)
    }

    @Unroll
    void "validate, when CRAM file has mateNumber #mateNumber and source fastq files of mates #sourceMates, should fail"() {
        given:
        SeqTrack seqTrack = createSeqTrack([seqType: createSeqType([libraryLayout: SequencingReadType.PAIRED])])
        Set<FastqFile> sourceFastqFiles = sourceMates.collect { createFastqFile([seqTrack: seqTrack, mateNumber: it]) } as Set
        SequenceCramFile cramFile = createSequenceCramFile([
                seqTrack        : seqTrack,
                mateNumber      : mateNumber,
                sourceFastqFiles: sourceFastqFiles,
        ], false)

        expect:
        TestCase.assertValidateError(cramFile, "sourceFastqFiles", "mateNumberMismatch", sourceFastqFiles)

        where:
        mateNumber | sourceMates
        1          | [2]
        1          | [1, 2]
    }
}
