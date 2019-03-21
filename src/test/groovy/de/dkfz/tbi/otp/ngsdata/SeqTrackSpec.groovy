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


import grails.testing.gorm.DataTest
import spock.lang.Specification

class SeqTrackSpec extends Specification implements DataTest {

    Class[] getDomainClassesToMock() {[
            DataFile,
            FileType,
            Individual,
            Project,
            ProjectCategory,
            Realm,
            Run,
            RunSegment,
            Sample,
            SampleType,
            SeqCenter,
            SeqPlatform,
            SeqPlatformGroup,
            SeqPlatformModelLabel,
            SeqTrack,
            SeqType,
            SoftwareTool,
    ]}

    void "test getNReads, returns null"() {
        given:
        SeqTrack seqTrack = DomainFactory.createSeqTrack()
        DomainFactory.createDataFile([seqTrack: seqTrack, nReads: null])
        DomainFactory.createDataFile([seqTrack: seqTrack, nReads: input])

        expect:
        seqTrack.getNReads() == null

        where:
        input | _
        null  | _
        420   | _
    }

    void "test getNReads, returns sum"() {
        given:
        SeqTrack seqTrack = DomainFactory.createSeqTrack()
        DomainFactory.createDataFile([seqTrack: seqTrack, nReads: 525])
        DomainFactory.createDataFile([seqTrack: seqTrack, nReads: 25])

        expect:
        seqTrack.getNReads() == 550
    }
}
