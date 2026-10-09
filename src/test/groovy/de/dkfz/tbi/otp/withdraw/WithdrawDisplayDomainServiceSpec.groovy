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
package de.dkfz.tbi.otp.withdraw

import grails.test.hibernate.HibernateSpec

import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.ngsdata.*

class WithdrawDisplayDomainServiceSpec extends HibernateSpec implements DomainFactoryCore {

    @Override
    List<Class> getDomainClasses() {
        return [
                RawSequenceFile,
                FastqFile,
                SequenceCramFile,
        ]
    }

    private final WithdrawDisplayDomainService service = new WithdrawDisplayDomainService()

    void "rawSequenceFileInfo, when mateNumber is given, then the info contains the mate number"() {
        given:
        RawSequenceFile rawSequenceFile = createFastqFile([
                seqTrack  : createSeqTrack([seqType: createSeqType([libraryLayout: SequencingReadType.PAIRED])]),
                mateNumber: 2,
        ])

        when:
        String info = service.rawSequenceFileInfo(rawSequenceFile)

        then:
        info == "${service.seqTrackInfo(rawSequenceFile.seqTrack)}\t2\t${rawSequenceFile.fileName}"
    }

    void "rawSequenceFileInfo, when the file contains all reads of the lane, then the mate number column is empty"() {
        given:
        RawSequenceFile rawSequenceFile = createSequenceCramFile()

        when:
        String info = service.rawSequenceFileInfo(rawSequenceFile)

        then:
        rawSequenceFile.mateNumber == null
        info == "${service.seqTrackInfo(rawSequenceFile.seqTrack)}\t\t${rawSequenceFile.fileName}"
    }

    void "rawSequenceFileInfo, when withdrawn columns are requested and the file contains all reads of the lane, then all columns are given"() {
        given:
        Date withdrawnDate = new Date()
        RawSequenceFile rawSequenceFile = createSequenceCramFile([
                fileWithdrawn   : true,
                withdrawnDate   : withdrawnDate,
                withdrawnComment: "some comment",
        ])

        when:
        String info = service.rawSequenceFileInfo(rawSequenceFile, true)

        then:
        info == "${service.seqTrackInfo(rawSequenceFile.seqTrack)}\t\t${rawSequenceFile.fileName}\ttrue\t${withdrawnDate}\tsome comment"
    }
}
