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
package de.dkfz.tbi.otp.job.processing

import grails.testing.mixin.integration.Integration
import grails.transaction.Rollback
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.job.jobs.runYapsa.ExecuteRunYapsaJob
import de.dkfz.tbi.otp.ngsdata.*

@Rollback
@Integration
class ClusterJobSubmissionOptionsServiceIntegrationSpec extends Specification {

    @Unroll
    void "test readOptionsFromDatabase ('#realmOption', ''#jobSpecificOption', '#jobAndSeqTypeSpecific')"() {
        given:
        ClusterJobSubmissionOptionsService service = new ClusterJobSubmissionOptionsService()
        service.processingOptionService = new ProcessingOptionService()

        Realm realm = DomainFactory.createRealm(
                defaultJobSubmissionOptions: realmOption,
        )

        SeqType seqType = DomainFactory.createWholeGenomeSeqType()
        ProcessParameterObject ppo = DomainFactory.createSeqTrack(seqType: seqType)
        ProcessingStep processingStep = DomainFactory.createAndSaveProcessingStep(ExecuteRunYapsaJob.simpleName, ppo)

        service.processingOptionService.createOrUpdate(
                ProcessingOption.OptionName.CLUSTER_SUBMISSIONS_OPTION,
                jobSpecificOption,
                "${processingStep.nonQualifiedJobClass}",
        )
        service.processingOptionService.createOrUpdate(
                ProcessingOption.OptionName.CLUSTER_SUBMISSIONS_OPTION,
                jobAndSeqTypeSpecific,
                "${processingStep.nonQualifiedJobClass}_${seqType.processingOptionName}",
        )

        expect:
        result == service.readOptionsFromDatabase(processingStep, realm)

        where:
        realmOption      | jobSpecificOption   | jobAndSeqTypeSpecific || result
        '{"NODES": "1"}' | '{"WALLTIME": "2"}' | '{"QUEUE": "3"}'      || [(JobSubmissionOption.NODES): "1", (JobSubmissionOption.WALLTIME): "2", (JobSubmissionOption.QUEUE): "3"]
        ''               | ''                  | ''                    || [:]
        ''               | ''                  | '{"NODES": "3"}'      || [(JobSubmissionOption.NODES): "3"]
        ''               | '{"NODES": "2"}'    | ''                    || [(JobSubmissionOption.NODES): "2"]
        ''               | '{"NODES": "2"}'    | '{"NODES": "3"}'      || [(JobSubmissionOption.NODES): "3"]
        '{"NODES": "1"}' | ''                  | ''                    || [(JobSubmissionOption.NODES): "1"]
        '{"NODES": "1"}' | ''                  | '{"NODES": "3"}'      || [(JobSubmissionOption.NODES): "3"]
        '{"NODES": "1"}' | '{"NODES": "2"}'    | ''                    || [(JobSubmissionOption.NODES): "2"]
        '{"NODES": "1"}' | '{"NODES": "2"}'    | '{"NODES": "3"}'      || [(JobSubmissionOption.NODES): "3"]
    }
}
