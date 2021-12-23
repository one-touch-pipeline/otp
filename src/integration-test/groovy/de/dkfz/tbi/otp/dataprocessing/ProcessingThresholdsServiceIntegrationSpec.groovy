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

import grails.testing.mixin.integration.Integration
import grails.gorm.transactions.Rollback
import spock.lang.Specification

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.domainFactory.DomainFactoryCore
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.ngsdata.SeqTrack

@Rollback
@Integration
class ProcessingThresholdsServiceIntegrationSpec extends Specification implements DomainFactoryCore {

    void "test generateDefaultThresholds, when threshold does not exist yet, create it"() {
        given:
        ProcessingThresholdsService service = new ProcessingThresholdsService()
        SeqTrack seqTrack1 = createSeqTrack()

        when:
        List<ProcessingThresholds> thresholds = service.generateDefaultThresholds([seqTrack1])

        then:
        thresholds.size() == 1
        thresholds[0].project == seqTrack1.project
        thresholds[0].sampleType == seqTrack1.sampleType
        thresholds[0].seqType == seqTrack1.seqType
    }

    void "test generateDefaultThresholds, when threshold already exist, don't create it again"() {
        given:
        ProcessingThresholdsService service = new ProcessingThresholdsService()

        SeqTrack seqTrack1 = createSeqTrack()
        DomainFactory.createProcessingThresholdsForSeqTrack(seqTrack1)

        when:
        List<ProcessingThresholds> thresholds = service.generateDefaultThresholds([seqTrack1])

        then:
        thresholds.empty
    }

    void "test generateDefaultThresholds, when multiple lanes have the same combination, then create threshold only once"() {
        given:
        ProcessingThresholdsService service = new ProcessingThresholdsService()

        SeqTrack seqTrack1 = createSeqTrack()
        SeqTrack seqTrack2 = createSeqTrack([
                seqType: seqTrack1.seqType,
                sample : seqTrack1.sample,
        ])

        when:
        List<ProcessingThresholds> thresholds = service.generateDefaultThresholds([seqTrack1, seqTrack2])

        then:
        thresholds.size() == 1
        thresholds[0].project == seqTrack1.project
        thresholds[0].sampleType == seqTrack1.sampleType
        thresholds[0].seqType == seqTrack1.seqType
    }

    void "test generateDefaultThresholds, when lane has not a threshold and lanes with same individual and seqType exist without an threshold, then create also threshold for them"() {
        given:
        ProcessingThresholdsService service = new ProcessingThresholdsService()

        SeqTrack seqTrack1 = createSeqTrack()
        SeqTrack seqTrack2 = createSeqTrack([
                seqType: seqTrack1.seqType,
                sample : createSample([
                        individual: seqTrack1.individual,
                ]),
        ])

        when:
        List<ProcessingThresholds> thresholds = service.generateDefaultThresholds([seqTrack1])

        then:
        thresholds.size() == 2
        thresholds*.project == [seqTrack1.project, seqTrack2.project]
        thresholds*.seqType == [seqTrack1.seqType, seqTrack2.seqType]
        TestCase.assertContainSame(thresholds*.sampleType, [seqTrack1.sampleType, seqTrack2.sampleType])
    }

    void "test generateDefaultThresholds, when the lane has a threshold, but there are other lanes for the same individual and seqType without threshold, then create the threshold only for them"() {
        given:
        ProcessingThresholdsService service = new ProcessingThresholdsService()

        SeqTrack seqTrack1 = createSeqTrack()
        SeqTrack seqTrack2 = createSeqTrack([
                seqType: seqTrack1.seqType,
                sample : createSample([
                        individual: seqTrack1.individual,
                ]),
        ])
        DomainFactory.createProcessingThresholdsForSeqTrack(seqTrack1)

        when:
        List<ProcessingThresholds> thresholds = service.generateDefaultThresholds([seqTrack1])

        then:
        thresholds.size() == 1
        thresholds[0].project == seqTrack2.project
        thresholds[0].sampleType == seqTrack2.sampleType
        thresholds[0].seqType == seqTrack2.seqType
    }

    void "test generateDefaultThresholds, when lane of same individual, but other seqType exists, then do not create a threshold therefore"() {
        given:
        ProcessingThresholdsService service = new ProcessingThresholdsService()

        SeqTrack seqTrack1 = createSeqTrack()
        createSeqTrack([
                sample: createSample([
                        individual: seqTrack1.individual,
                ]),
        ])

        when:
        List<ProcessingThresholds> thresholds = service.generateDefaultThresholds([seqTrack1])

        then:
        thresholds.size() == 1
        thresholds[0].project == seqTrack1.project
        thresholds[0].sampleType == seqTrack1.sampleType
        thresholds[0].seqType == seqTrack1.seqType
    }
}
