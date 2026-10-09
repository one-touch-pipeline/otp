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

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import spock.lang.Specification

import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore

@Rollback
@Integration
class SequenceCramFileIntegrationSpec extends Specification implements DomainFactoryCore {

    void "sourceFastqFiles, when CRAM file was converted from a read pair, then both source fastq files are persisted"() {
        given:
        SeqTrack seqTrack = createSeqTrack([seqType: createSeqType([libraryLayout: SequencingReadType.PAIRED])])
        FastqFile fastqFile1 = createFastqFile([seqTrack: seqTrack, mateNumber: 1])
        FastqFile fastqFile2 = createFastqFile([seqTrack: seqTrack, mateNumber: 2])
        SequenceCramFile cramFile = createSequenceCramFile([seqTrack: seqTrack, sourceFastqFiles: [fastqFile1, fastqFile2] as Set])

        when:
        SequenceCramFile.withSession { it.flush(); it.clear() }
        SequenceCramFile reloaded = SequenceCramFile.get(cramFile.id)

        then:
        reloaded.sourceFastqFiles*.id as Set == [fastqFile1.id, fastqFile2.id] as Set
    }
}
