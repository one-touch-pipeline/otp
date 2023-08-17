/*
 * Copyright 2011-2020 The OTP authors
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
package de.dkfz.tbi.otp.job.jobs.AbstractBamFilePairAnalysis

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair
import de.dkfz.tbi.otp.job.jobs.bamFilePairAnalysis.AbstractBamFilePairAnalysisStartJob
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.tracking.Ticket
import de.dkfz.tbi.otp.workflowExecution.ProcessingPriority

trait StartJobIntegrationSpec {

    SamplePair setupSamplePair() {
        Map map = DomainFactory.createProcessableSamplePair()
        SamplePair samplePair = map.samplePair
        samplePair.project.processingPriority.priority = ProcessingPriority.NORMAL
        samplePair.project.processingPriority.save(flush: true)

        createConfig(samplePair, createPipeline())

        return samplePair
    }

    abstract Pipeline createPipeline()

    abstract BamFilePairAnalysis getInstance()

    abstract Date getStartedDate(Ticket ticket)

    abstract SamplePair.ProcessingStatus getProcessingStatus(SamplePair samplePair)

    abstract ConfigPerProjectAndSeqType createConfig(SamplePair samplePair, Pipeline pipeline)

    abstract AbstractBamFilePairAnalysisStartJob getService()

    /**
     * Callback to set the processing status of our dependant for the test-samplepair.
     *
     * Each workflow startjob that depends on another workflow being in some state MUST
     * provide this method, so that tests in this abstract class can create the correct
     * dependency environment.
     */
    @SuppressWarnings("UnusedMethodParameter")
    void setDependencyProcessingStatus(SamplePair samplePair, SamplePair.ProcessingStatus dependeeProcessingStatus) {
        throw new UnsupportedOperationException('Not supported for this test')
    }

    @SuppressWarnings("UnusedMethodParameter")
    void createDependeeInstance(SamplePair samplePair, AnalysisProcessingStates dependeeAnalysisProcessingState) {
        throw new UnsupportedOperationException('Not supported for this test')
    }
}
