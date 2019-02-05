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

import grails.test.mixin.*
import grails.test.mixin.support.GrailsUnitTestMixin
import org.junit.*

import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertTrue

@TestMixin(GrailsUnitTestMixin)
@TestFor(ChipSeqSeqTrack)
@Mock([ChipSeqSeqTrack, AntibodyTarget])
class ChipSeqSeqTrackTests {

    static final String ANTIBODY = "antibodyName"

    static final AntibodyTarget ANTIBODY_TARGET = new AntibodyTarget(name: "antibodyTargetName")

    ChipSeqSeqTrack chipSeqSeqTrack

    @Before
    void setUp() throws Exception {
        chipSeqSeqTrack =  new ChipSeqSeqTrack(
                        laneId: "lane",
                        run: new Run(),
                        sample: new Sample(),
                        seqType: new SeqType(name: SeqTypeNames.CHIP_SEQ.seqTypeName),
                        seqPlatform: new SeqPlatform(),
                        pipelineVersion: new SoftwareTool()
                        )
    }

    @After
    void tearDown() throws Exception {
        chipSeqSeqTrack = null
    }

    @Test
    void testNullableAntibodyAndValidAntibodyTarget() {
        chipSeqSeqTrack.antibodyTarget = ANTIBODY_TARGET
        assertTrue chipSeqSeqTrack.validate()
    }

    @Test
    void testNotNullAntibodyAndValidAntibodyTarget() {
        chipSeqSeqTrack.antibodyTarget = ANTIBODY_TARGET
        chipSeqSeqTrack.antibody = ANTIBODY
        assertTrue chipSeqSeqTrack.validate()
    }

    @Test
    void testNullAntibodyTarget() {
        chipSeqSeqTrack.antibody = ANTIBODY
        assertFalse chipSeqSeqTrack.validate()
    }
}
