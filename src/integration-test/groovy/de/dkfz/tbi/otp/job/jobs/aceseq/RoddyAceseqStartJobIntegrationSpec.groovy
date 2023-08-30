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
package de.dkfz.tbi.otp.job.jobs.aceseq

import org.springframework.beans.factory.annotation.Autowired

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair
import de.dkfz.tbi.otp.job.jobs.AbstractBamFilePairAnalysis.RoddyJobSpec
import de.dkfz.tbi.otp.job.jobs.AbstractBamFilePairAnalysis.WithReferenceGenomeRestrictionSpec
import de.dkfz.tbi.otp.job.jobs.AbstractBamFilePairAnalysisStartJobWithDependenciesIntegrationSpec
import de.dkfz.tbi.otp.job.jobs.bamFilePairAnalysis.AbstractBamFilePairAnalysisStartJob
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.tracking.OtrsTicket

class RoddyAceseqStartJobIntegrationSpec extends AbstractBamFilePairAnalysisStartJobWithDependenciesIntegrationSpec implements RoddyJobSpec, WithReferenceGenomeRestrictionSpec {

    @Autowired
    RoddyAceseqStartJob roddyAceseqStartJob

    @Override
    Pipeline createPipeline() {
        return DomainFactory.createAceseqPipelineLazy()
    }

    @Override
    AbstractBamFilePairAnalysisStartJob getService() {
        return roddyAceseqStartJob
    }

    @Override
    BamFilePairAnalysis getInstance() {
        return DomainFactory.createAceseqInstanceWithRoddyBamFiles()
    }

    @Override
    Date getStartedDate(OtrsTicket otrsTicket) {
        return otrsTicket.aceseqStarted
    }

    @Override
    SamplePair.ProcessingStatus getProcessingStatus(SamplePair samplePair) {
        return samplePair.aceseqProcessingStatus
    }

    @Override
    SamplePair setupSamplePair() {
        SamplePair samplePair = WithReferenceGenomeRestrictionSpec.super.setupSamplePair()

        // prepare a "finished" sophia analysis, since we depend on that.
        samplePair.sophiaProcessingStatus = SamplePair.ProcessingStatus.NO_PROCESSING_NEEDED
        samplePair.save(flush: true)
        createDependeeInstance(samplePair, AnalysisProcessingStates.FINISHED)

        return samplePair
    }

    @Override
    void setDependencyProcessingStatus(SamplePair samplePair, SamplePair.ProcessingStatus dependeeProcessingStatus) {
        samplePair.sophiaProcessingStatus = dependeeProcessingStatus
    }

    @Override
    void createDependeeInstance(SamplePair samplePair, AnalysisProcessingStates dependeeAnalysisProcessingState) {
        DomainFactory.createSophiaInstance(samplePair, [processingState: dependeeAnalysisProcessingState])
    }

    @Override
    ProcessingOption.OptionName getProcessingOptionNameForReferenceGenome() {
        return ProcessingOption.OptionName.PIPELINE_ACESEQ_REFERENCE_GENOME
    }
}
