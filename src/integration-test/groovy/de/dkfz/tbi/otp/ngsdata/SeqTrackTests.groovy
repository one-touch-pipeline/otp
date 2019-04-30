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

import grails.testing.mixin.integration.Integration
import grails.transaction.Rollback
import org.junit.Before
import org.junit.Test

import de.dkfz.tbi.TestCase

@Rollback
@Integration
class SeqTrackTests {

    ReferenceGenomeProjectSeqType referenceGenomeProjectSeqTypeWithoutSampleType
    ReferenceGenomeProjectSeqType referenceGenomeProjectSeqTypeWithSampleType
    SeqTrack seqTrack

    @Before
    void setup() {
        seqTrack = DomainFactory.createSeqTrack().save(flush: true)
        referenceGenomeProjectSeqTypeWithSampleType = ReferenceGenomeProjectSeqType.build(
                project: seqTrack.project,
                seqType: seqTrack.seqType,
                sampleType: seqTrack.sampleType,
        ).save(flush: true)
        referenceGenomeProjectSeqTypeWithoutSampleType = ReferenceGenomeProjectSeqType.build(
                project: seqTrack.project,
                seqType: seqTrack.seqType,
        ).save(flush: true)
    }


    @Test
    void testGetConfiguredReferenceGenome_ProjectDefault() {
        seqTrack.sampleType.specificReferenceGenome = SampleType.SpecificReferenceGenome.USE_PROJECT_DEFAULT

        ReferenceGenome referenceGenome = seqTrack.getConfiguredReferenceGenome()
        assert referenceGenomeProjectSeqTypeWithoutSampleType.referenceGenome == referenceGenome
    }

    @Test
    void testGetConfiguredReferenceGenome_SampleTypeSpecific() {
        seqTrack.sampleType.specificReferenceGenome = SampleType.SpecificReferenceGenome.USE_SAMPLE_TYPE_SPECIFIC

        ReferenceGenome referenceGenome = seqTrack.getConfiguredReferenceGenome()
        assert referenceGenomeProjectSeqTypeWithSampleType.referenceGenome == referenceGenome
    }

    @Test
    void testLog() {
        seqTrack.log("Test")
        TestCase.assertContainSame(seqTrack.logMessages*.message, ["Test"])
    }

    @Test
    void testLog_Twice() {
        seqTrack.log("Test")
        seqTrack.log("Test2")
        TestCase.assertContainSame(seqTrack.logMessages*.message, ["Test", "Test2"])
    }

    @Test
    void testLog_WrongOrder() {
        seqTrack.log("Test")
        seqTrack.log("Test2")
        TestCase.assertContainSame(seqTrack.logMessages*.message, ["Test", "Test2"])
        shouldFail {
            assert seqTrack.logMessages.message[0] == "Test2"
        }
        shouldFail {
            assert seqTrack.logMessages.message[1] == "Test"
        }
    }
}
