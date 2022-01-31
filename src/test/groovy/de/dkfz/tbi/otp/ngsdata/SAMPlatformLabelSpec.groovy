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

class SAMPlatformLabelSpec extends Specification implements DataTest {

    void testExectMatch() {
        given:
        String label = "Illumina"
        SAMPlatformLabel expectedLabel = SAMPlatformLabel.ILLUMINA

        expect:
        expectedLabel == SAMPlatformLabel.map(label)
    }

    void testNotExectMatch() {
        given:
        String label = "ABI_SOLiD"
        SAMPlatformLabel expectedLabel = SAMPlatformLabel.SOLID

        expect:
        expectedLabel == SAMPlatformLabel.map(label)
    }

    void testNoMatch() {
        given:
        String label = "does-not-match-platform-label"

        when:
        SAMPlatformLabel.map(label)

        then:
        thrown IllegalArgumentException
    }

    void testMultipleMatch() {
        given:
        String label = "capillary_solid"

        when:
        SAMPlatformLabel.map(label)

        then:
        thrown IllegalArgumentException
    }
}
