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
package de.dkfz.tbi.otp.dataprocessing

import grails.testing.gorm.DataTest
import spock.lang.Specification

import de.dkfz.tbi.otp.ngsdata.*

class ProcessingThresholdsServiceSpec extends Specification implements DataTest {

    @Override
    Class[] getDomainClassesToMock() {
        [
                Individual,
                ProcessingThresholds,
                Project,
                Realm,
                Sample,
                SampleType,
                SeqType,
        ]
    }


    void "test getSeqTracksWithoutProcessingThreshold"() {
        List<SeqType> seqTypes = DomainFactory.createAllAnalysableSeqTypes()

        SeqTrack st1 = DomainFactory.createSeqTrack()
        SeqTrack st2 = DomainFactory.createSeqTrack(seqType: seqTypes.first())
        SeqTrack st3 = DomainFactory.createSeqTrack()
        SeqTrack st4 = DomainFactory.createSeqTrack(seqType: seqTypes.first())
        DomainFactory.createProcessingThresholds(project: st1.project, sampleType: st1.sampleType, seqType: st1.seqType)
        DomainFactory.createProcessingThresholds(project: st2.project, sampleType: st2.sampleType, seqType: st2.seqType)

        ProcessingThresholdsService service = new ProcessingThresholdsService()

        expect:
        [st4] == service.getSeqTracksWithoutProcessingThreshold([st1, st2, st3, st4])
    }
}
