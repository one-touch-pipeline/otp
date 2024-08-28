/*
 * Copyright 2011-2024 The OTP authors
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
package de.dkfz.tbi.otp.ngsqc

import grails.test.hibernate.HibernateSpec

import de.dkfz.tbi.otp.dataprocessing.FastqcProcessedFile
import de.dkfz.tbi.otp.domainFactory.FastqcDomainFactory
import de.dkfz.tbi.otp.ngsdata.*

class FastqcResultsServiceSpec extends HibernateSpec implements FastqcDomainFactory {

    FastqcResultsService fastqcResultsService

    @Override
    List<Class> getDomainClasses() {
        return [
                Sequence,
                RawSequenceFile,
                FastqFile,
                FastqcProcessedFile,
        ]
    }

    void setup() {
        fastqcResultsService = new FastqcResultsService()
    }

    void "fastQCFiles, should returns the sequence files with correct state"() {
        given:
        List<FastqcProcessedFile> expectedProcessedFiles = [
                createProcessedFilesWithFinishedState(),
                createProcessedFilesWithFinishedState(),
                createFastqcProcessedFile(),
        ]

        List<Sequence> sequences = [
                DomainFactory.createSequence([
                        seqTrackId: expectedProcessedFiles[0].sequenceFile.seqTrack.id,
                ]),
                DomainFactory.createSequence([
                        seqTrackId: expectedProcessedFiles[1].sequenceFile.seqTrack.id,
                ]),
                DomainFactory.createSequence([
                        fastqcState: SeqTrack.DataProcessingState.UNKNOWN
                ]),
        ]

        when:
        List<RawSequenceFile> seqFiles = fastqcResultsService.fastQCFiles(sequences)

        then:
        expectedProcessedFiles*.sequenceFile.findAll {
            it.seqTrack.fastqcState == SeqTrack.DataProcessingState.FINISHED
        } == seqFiles
    }

    private FastqcProcessedFile createProcessedFilesWithFinishedState() {
        return createFastqcProcessedFile([
                sequenceFile: createFastqFile([
                        seqTrack: createSeqTrack([
                                fastqcState: SeqTrack.DataProcessingState.FINISHED
                        ])
                ])
        ])
    }
}
