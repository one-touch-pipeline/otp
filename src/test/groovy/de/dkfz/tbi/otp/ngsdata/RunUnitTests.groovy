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

import grails.buildtestdata.mixin.Build
import grails.test.mixin.TestFor
import org.junit.Test

import static org.junit.Assert.*

@TestFor(Run)
@Build([
    SeqPlatformGroup,
    SeqTrack,
])
class RunUnitTests {

    @Test
    void testGetSeqType() {

        Run run = Run.build()

        SeqType seqType1 = SeqType.build()
        SeqType seqType2 = SeqType.build()

        DomainFactory.createSeqTrack(
            run: run,
            seqType: seqType1,
        )

        DomainFactory.createSeqTrack(
            run: run,
            seqType: seqType1,
        )

        assertNotNull(run.getSeqType())

        DomainFactory.createSeqTrack(
             run: run,
             seqType: seqType2,
        )

        assertNull(run.getSeqType())
    }

    @Test
    void testGetIndividual() {

        Run run = Run.build()

        Individual individual1 = Individual.build()
        Individual individual2 = Individual.build()

        Sample sample1 = Sample.build(
            individual: individual1
        )

        Sample sample2 = Sample.build(
            individual: individual2
        )

        DomainFactory.createSeqTrack(
            run: run,
            sample: sample1
        )

        DomainFactory.createSeqTrack(
            run: run,
            sample: sample1
        )

        assertEquals(individual1, run.getIndividual())

        SeqTrack.build (
            run: run,
            sample: sample2
        )

        assertNull(run.getIndividual())
    }
}
