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
package de.dkfz.tbi.otp.ngsdata

import grails.testing.gorm.DataTest
import spock.lang.Specification

class RunSpec extends Specification implements DataTest {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                SeqPlatformGroup,
                SeqTrack,
        ]
    }

    void "test getSeqType"() {
        given:
        Run run = DomainFactory.createRun()

        SeqType seqType1 = DomainFactory.createWholeGenomeSeqType()

        DomainFactory.createSeqTrack(
            run: run,
            seqType: seqType1,
        )

        DomainFactory.createSeqTrack(
            run: run,
            seqType: seqType1,
        )

        expect:
        run.seqType == seqType1
    }

    void "test getSeqType, when run has multiple seq types, return null"() {
        given:
        Run run = DomainFactory.createRun()

        SeqType seqType1 = DomainFactory.createWholeGenomeSeqType()
        SeqType seqType2 = DomainFactory.createWholeGenomeBisulfiteSeqType()

        DomainFactory.createSeqTrack(
            run: run,
            seqType: seqType1,
        )

        DomainFactory.createSeqTrack(
             run: run,
             seqType: seqType2,
        )

        expect:
        run.seqType == null
    }

    void "test getIndividual"() {
        given:
        Run run = DomainFactory.createRun()

        Individual individual1 = DomainFactory.createIndividual()

        Sample sample1 = DomainFactory.createSample(
            individual: individual1
        )

        DomainFactory.createSeqTrack(
            run: run,
            sample: sample1
        )

        DomainFactory.createSeqTrack(
            run: run,
            sample: sample1
        )

        expect:
        individual1 == run.individual
    }

    void "test getIndividual, when run has multiple individuals, return null"() {
        given:
        Run run = DomainFactory.createRun()

        Individual individual1 = DomainFactory.createIndividual()
        Individual individual2 = DomainFactory.createIndividual()

        Sample sample1 = DomainFactory.createSample(
            individual: individual1
        )

        Sample sample2 = DomainFactory.createSample(
            individual: individual2
        )

        DomainFactory.createSeqTrack(
            run: run,
            sample: sample1
        )

        DomainFactory.createSeqTrack(
            run: run,
            sample: sample2
        )

        expect:
        run.individual == null
    }
}
