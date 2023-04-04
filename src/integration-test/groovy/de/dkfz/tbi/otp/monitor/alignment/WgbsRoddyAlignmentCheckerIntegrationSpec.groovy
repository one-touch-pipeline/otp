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
package de.dkfz.tbi.otp.monitor.alignment

import grails.gorm.transactions.Rollback

import de.dkfz.tbi.otp.dataprocessing.Pipeline
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.ngsdata.SeqType
import de.dkfz.tbi.otp.workflow.panCancer.PanCancerWorkflow
import de.dkfz.tbi.otp.workflow.wgbs.WgbsWorkflow

@Rollback
class WgbsRoddyAlignmentCheckerIntegrationSpec extends AbstractAlignmentCheckerIntegrationSpec {

    @Override
    AbstractAlignmentChecker createAlignmentChecker() {
        return new WgbsRoddyAlignmentChecker()
    }

    @Override
    String getWorkflowName() {
        return WgbsWorkflow.WORKFLOW
    }

    @Override
    Set<SeqType> getSupportedSeqTypes() {
        return [
                DomainFactory.createWholeGenomeBisulfiteSeqType(),
                DomainFactory.createWholeGenomeBisulfiteTagmentationSeqType(),
        ] as Set
    }

    @Override
    Pipeline createPipeLine() {
        return DomainFactory.createPanCanPipeline()
    }

    @Override
    Pipeline createPipeLineForCrosschecking() {
        return DomainFactory.createRoddyRnaPipeline()
    }

    @Override
    String getWorkflowNameForCrosschecking() {
        return PanCancerWorkflow.WORKFLOW
    }

    void "workflowName, should return WgbsAlignmentWorkflow"() {
        expect:
        'WgbsAlignmentWorkflow' == createAlignmentChecker().workflowName
    }

    void "pipeLineName, should return PANCAN_ALIGNMENT"() {
        expect:
        Pipeline.Name.PANCAN_ALIGNMENT == createAlignmentChecker().pipeLineName
    }
}
