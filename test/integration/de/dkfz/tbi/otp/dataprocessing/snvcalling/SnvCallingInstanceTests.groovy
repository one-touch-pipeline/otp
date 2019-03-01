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

package de.dkfz.tbi.otp.dataprocessing.snvcalling

import org.junit.Before
import org.junit.Test

import de.dkfz.tbi.otp.dataprocessing.AnalysisProcessingStates
import de.dkfz.tbi.otp.ngsdata.DomainFactory

class SnvCallingInstanceTests {

    SamplePair samplePair

    @Before
    void setUp() {
        samplePair = DomainFactory.createSamplePairWithProcessedMergedBamFiles()
    }

    @Test
    void test_updateProcessingState_WhenStateIsNull_ShouldFail() {
        RoddySnvCallingInstance snvCallingInstance = createSnvCallingInstance()
        def msg = shouldFail AssertionError, { snvCallingInstance.updateProcessingState(null) }
        assert msg =~ /not allowed to be null/
    }

    @Test
    void test_updateProcessingState_WhenStateIsChangedToSame_ShouldSucceed() {
        RoddySnvCallingInstance snvCallingInstance = createSnvCallingInstance()
        snvCallingInstance.updateProcessingState(AnalysisProcessingStates.IN_PROGRESS)
        assert snvCallingInstance.processingState == AnalysisProcessingStates.IN_PROGRESS
    }

    @Test
    void test_updateProcessingState_WhenStateIsChangedToDifferent_ShouldSucceed() {
        RoddySnvCallingInstance snvCallingInstance = createSnvCallingInstance()
        snvCallingInstance.updateProcessingState(AnalysisProcessingStates.FINISHED)
        assert snvCallingInstance.processingState == AnalysisProcessingStates.FINISHED
    }

    @Test
    void testProcessingStateIsFailed() {
        def instance = createSnvCallingInstance()
        instance.withdrawn = true
        assert instance.validate()
    }

    private RoddySnvCallingInstance createSnvCallingInstance() {
        return DomainFactory.createRoddySnvCallingInstance([
                sampleType1BamFile: samplePair.mergingWorkPackage1.bamFileInProjectFolder,
                sampleType2BamFile: samplePair.mergingWorkPackage2.bamFileInProjectFolder,
                samplePair        : samplePair,
        ])
    }
}
