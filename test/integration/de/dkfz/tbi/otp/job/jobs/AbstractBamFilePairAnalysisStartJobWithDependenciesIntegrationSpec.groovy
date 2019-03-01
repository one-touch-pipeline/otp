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

package de.dkfz.tbi.otp.job.jobs

import de.dkfz.tbi.otp.dataprocessing.AnalysisProcessingStates
import de.dkfz.tbi.otp.dataprocessing.ProcessingPriority
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair

abstract class AbstractBamFilePairAnalysisStartJobWithDependenciesIntegrationSpec extends AbstractBamFilePairAnalysisStartJobIntegrationSpec {

    void "don't findSamplePairToProcess when prereq not yet done"() {
        given:
        SamplePair samplePair = setupSamplePair()
        setDependencyProcessingStatus(samplePair, SamplePair.ProcessingStatus.NEEDS_PROCESSING)
        samplePair.save(flush: true)

        expect:
        null == getService().findSamplePairToProcess(ProcessingPriority.NORMAL)
    }

    void "don't findSamplePairToProcess, when at least one prereq still running"() {
        given:
        SamplePair samplePair = setupSamplePair()
        setDependencyProcessingStatus(samplePair, SamplePair.ProcessingStatus.NO_PROCESSING_NEEDED)
        samplePair.save(flush: true)

        createDependeeInstance(samplePair, AnalysisProcessingStates.FINISHED)
        createDependeeInstance(samplePair, AnalysisProcessingStates.IN_PROGRESS)

        expect:
        null == getService().findSamplePairToProcess(ProcessingPriority.NORMAL)
    }
}
