/*
 * Copyright 2011-2023 The OTP authors
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
package de.dkfz.tbi.otp.workflowExecution.decider

import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.workflow.alignment.panCancer.PanCancerWorkflow

@Component
@Transactional
@Slf4j
class PanCancerDecider extends AbstractAlignmentDecider {

    @Override
    final boolean requiresFastqcResults() {
        return true
    }

    @Override
    final String getWorkflowName() {
        return PanCancerWorkflow.WORKFLOW
    }

    @Override
    final String getInputFastqRole() {
        return PanCancerWorkflow.INPUT_FASTQ
    }

    @Override
    final String getInputFastqcRole() {
        return PanCancerWorkflow.INPUT_FASTQC
    }

    @Override
    final String getOutputBamRole() {
        return PanCancerWorkflow.OUTPUT_BAM
    }
}
