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
package de.dkfz.tbi.otp.monitor

import grails.transaction.Rollback

import de.dkfz.tbi.otp.dataprocessing.*
import de.dkfz.tbi.otp.ngsdata.DomainFactory

@Rollback
class IndelCallingPipelineCheckerIntegrationSpec extends AbstractVariantCallingPipelineCheckerIntegrationSpec {

    void "workflowName, should return IndelWorkflow"() {
        expect:
        'IndelWorkflow' == new IndelCallingPipelineChecker().getWorkflowName()
    }

    void "processingStateMember, should return indelProcessingStatus"() {
        expect:
        'indelProcessingStatus' == new IndelCallingPipelineChecker().getProcessingStateMember()
    }

    void "pipelineType, should return Pipeline.Type.INDEL"() {
        given:
        createPipeLine()

        expect:
        Pipeline.Type.INDEL == new IndelCallingPipelineChecker().getPipeline().type
    }

    void "bamFilePairAnalysisClass, should return IndelCallingInstance.class"() {
        expect:
        IndelCallingInstance.class == new IndelCallingPipelineChecker().getBamFilePairAnalysisClass()
    }

    @Override
    AbstractVariantCallingPipelineChecker createVariantCallingPipelineChecker() {
        return new IndelCallingPipelineChecker()
    }

    @Override
    Pipeline createPipeLine() {
        return DomainFactory.createIndelPipelineLazy()
    }

    @Override
    Pipeline createPipeLineForCrosschecking() {
        return DomainFactory.createRoddySnvPipelineLazy()
    }

    @Override
    BamFilePairAnalysis createAnalysis(Map properties) {
        return DomainFactory.createIndelCallingInstanceWithRoddyBamFiles(properties)
    }

    @Override
    BamFilePairAnalysis createAnalysisForCrosschecking(Map properties) {
        return DomainFactory.createRoddySnvInstanceWithRoddyBamFiles(properties)
    }
}
