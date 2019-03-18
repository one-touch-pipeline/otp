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

import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin
import org.junit.Test

import static org.junit.Assert.assertEquals

@TestMixin(GrailsUnitTestMixin)
class SAMPlatformLabelTest {

    @Test
    void testExectMatch() {
        String label = "Illumina"
        SAMPlatformLabel expectedLabel = SAMPlatformLabel.ILLUMINA
        assertEquals(expectedLabel, SAMPlatformLabel.map(label))
    }

    @Test
    void testNotExectMatch() {
        String label = "ABI_SOLiD"
        SAMPlatformLabel expectedLabel = SAMPlatformLabel.SOLID
        assertEquals(expectedLabel, SAMPlatformLabel.map(label))
    }

    @Test(expected = IllegalArgumentException)
    void testNoMatch() {
        String label = "does-not-match-platform-label"
        SAMPlatformLabel.map(label)
    }

    @Test(expected = IllegalArgumentException)
    void testMultipleMatch() {
        String label = "capillary_solid"
        SAMPlatformLabel.map(label)
    }
}
