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
import org.junit.Test

import de.dkfz.tbi.TestCase

@Build([SeqPlatform, SeqPlatformGroup, SeqPlatformModelLabel, SequencingKitLabel])
class SeqPlatformServiceUnitTests {

    final String PLATFORM_NAME = "platform_name"

    @Test
    void testCreateNewSeqPlatform_SeqPlatformNameIsNull_shouldFail() {
        TestCase.shouldFail(AssertionError) {
            SeqPlatformService.createNewSeqPlatform(null)
        }
    }

    @Test
    void testCreateNewSeqPlatform_OnlyNameIsProvided_AllFine() {
        SeqPlatform seqPlatform = SeqPlatformService.createNewSeqPlatform(PLATFORM_NAME)
        assert seqPlatform.name == PLATFORM_NAME
        assert seqPlatform.seqPlatformModelLabel == null
        assert seqPlatform.sequencingKitLabel == null
    }

    @Test
    void testCreateNewSeqPlatform_NameAndSeqPlatformGroupAreProvided_AllFine() {
        SeqPlatform seqPlatform = SeqPlatformService.createNewSeqPlatform(
                PLATFORM_NAME,
        )
        assert seqPlatform.name == PLATFORM_NAME
        assert seqPlatform.seqPlatformModelLabel == null
        assert seqPlatform.sequencingKitLabel == null
    }

    @Test
    void testCreateNewSeqPlatform_NameAndSeqPlatformGroupAndSeqPlatformModelLabelAreProvided_AllFine() {
        SeqPlatformModelLabel seqPlatformModelLabel = SeqPlatformModelLabel.build()
        SeqPlatform seqPlatform = SeqPlatformService.createNewSeqPlatform(
                PLATFORM_NAME,
                seqPlatformModelLabel
        )
        assert seqPlatform.name == PLATFORM_NAME
        assert seqPlatform.seqPlatformModelLabel == seqPlatformModelLabel
        assert seqPlatform.sequencingKitLabel == null
    }

    @Test
    void testCreateNewSeqPlatform_EverythingIsProvided_AllFine() {
        SeqPlatformModelLabel seqPlatformModelLabel = SeqPlatformModelLabel.build()
        SequencingKitLabel sequencingKitLabel = SequencingKitLabel.build()
        SeqPlatform seqPlatform = SeqPlatformService.createNewSeqPlatform(
                PLATFORM_NAME,
                seqPlatformModelLabel,
                sequencingKitLabel
        )
        assert seqPlatform.name == PLATFORM_NAME
        assert seqPlatform.seqPlatformModelLabel == seqPlatformModelLabel
        assert seqPlatform.sequencingKitLabel == sequencingKitLabel
    }
}
