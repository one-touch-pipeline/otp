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
import de.dkfz.tbi.otp.dataprocessing.runYapsa.RunYapsaInstance
import de.dkfz.tbi.otp.dataprocessing.snvcalling.SamplePair
import de.dkfz.tbi.otp.ngsdata.DomainFactory

@Rollback
class RunYapsaPipelineCheckerIntegrationSpec extends AbstractVariantCallingPipelineCheckerIntegrationSpec {

    void "workflowName, should return RunYapsaWorkflow"() {
        expect:
        'RunYapsaWorkflow' == createVariantCallingPipelineChecker().workflowName
    }

    void "processingStateMember, should return runYapsaProcessingStatus"() {
        expect:
        'runYapsaProcessingStatus' == createVariantCallingPipelineChecker().processingStateMember
    }

    void "pipelineType, should return Pipeline.Type.MUTATIONAL_SIGNATURE"() {
        given:
        createPipeLine()

        expect:
        Pipeline.Type.MUTATIONAL_SIGNATURE == createVariantCallingPipelineChecker().pipeline.type
    }

    void "bamFilePairAnalysisClass, should return RunYapsaInstance.class"() {
        expect:
        RunYapsaInstance.class == createVariantCallingPipelineChecker().bamFilePairAnalysisClass
    }

    @Override
    AbstractVariantCallingPipelineChecker createVariantCallingPipelineChecker() {
        return new RunYapsaPipelineChecker()
    }

    @Override
    Pipeline createPipeLine() {
        return DomainFactory.createRunYapsaPipelineLazy()
    }

    @Override
    Pipeline createPipeLineForCrosschecking() {
        return DomainFactory.createRoddySnvPipelineLazy()
    }

    @Override
    BamFilePairAnalysis createAnalysis(Map properties) {
        return DomainFactory.createRunYapsaInstanceWithRoddyBamFiles(properties)
    }

    @Override
    BamFilePairAnalysis createAnalysisForCrosschecking(Map properties) {
        return DomainFactory.createRoddySnvInstanceWithRoddyBamFiles(properties)
    }

    // RunYapsa is not a Roddy workflow, so the default doesn't work...
    @Override
    ConfigPerProjectAndSeqType createConfig(SamplePair samplePair, Map properties = [:]) {
        DomainFactory.createRunYapsaConfigLazy([
                pipeline: createPipeLine(),
                seqType : samplePair.seqType,
                project : samplePair.project,
        ] + properties)
    }
}
