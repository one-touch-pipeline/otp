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
package de.dkfz.tbi.otp.job.jobs.sophia

import org.springframework.beans.factory.annotation.Autowired

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair
import de.dkfz.tbi.otp.job.jobs.AbstractBamFilePairAnalysis.RoddyJobSpec
import de.dkfz.tbi.otp.job.jobs.AbstractBamFilePairAnalysis.WithReferenceGenomeRestrictionSpec
import de.dkfz.tbi.otp.job.jobs.AbstractBamFilePairAnalysisStartJobIntegrationSpec
import de.dkfz.tbi.otp.job.jobs.bamFilePairAnalysis.AbstractBamFilePairAnalysisStartJob
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.tracking.Ticket

class RoddySophiaStartJobIntegrationSpec extends AbstractBamFilePairAnalysisStartJobIntegrationSpec implements RoddyJobSpec, WithReferenceGenomeRestrictionSpec {

    @Autowired
    RoddySophiaStartJob roddySophiaStartJob

    @Override
    Pipeline createPipeline() {
        return DomainFactory.createSophiaPipelineLazy()
    }

    @Override
    AbstractBamFilePairAnalysisStartJob getService() {
        return roddySophiaStartJob
    }

    @Override
    BamFilePairAnalysis getInstance() {
        return DomainFactory.createSophiaInstanceWithRoddyBamFiles()
    }

    @Override
    Date getStartedDate(Ticket ticket) {
        return ticket.sophiaStarted
    }

    @Override
    SamplePair.ProcessingStatus getProcessingStatus(SamplePair samplePair) {
        return samplePair.sophiaProcessingStatus
    }

    @Override
    SamplePair setupSamplePair() {
        SamplePair samplePair = WithReferenceGenomeRestrictionSpec.super.setupSamplePair()
        return samplePair
    }

    @Override
    ProcessingOption.OptionName getProcessingOptionNameForReferenceGenome() {
        return ProcessingOption.OptionName.PIPELINE_SOPHIA_REFERENCE_GENOME
    }
}
