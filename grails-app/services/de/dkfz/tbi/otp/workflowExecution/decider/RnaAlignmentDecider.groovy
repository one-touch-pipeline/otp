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
package de.dkfz.tbi.otp.workflowExecution.decider

import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.dataprocessing.Pipeline
import de.dkfz.tbi.otp.dataprocessing.RoddyBamFile
import de.dkfz.tbi.otp.dataprocessing.rnaAlignment.RnaRoddyBamFile
import de.dkfz.tbi.otp.workflow.alignment.rna.RnaAlignmentWorkflow

@Component
@Transactional
@Slf4j
class RnaAlignmentDecider extends AbstractAlignmentDecider {

    @Override
    final boolean requiresFastqcResults() {
        return false
    }

    @Override
    final String getWorkflowName() {
        return RnaAlignmentWorkflow.WORKFLOW
    }

    @Override
    final String getInputFastqRole() {
        return RnaAlignmentWorkflow.INPUT_FASTQ
    }

    final String inputFastqcRole = null

    @Override
    final String getOutputBamRole() {
        return RnaAlignmentWorkflow.OUTPUT_BAM
    }

    @Override
    final Pipeline.Name getPipelineName() {
        return Pipeline.Name.RODDY_RNA_ALIGNMENT
    }

    @Override
    RoddyBamFile createBamFileWithoutFlush(Map properties) {
        return new RnaRoddyBamFile(properties).save(flush: false, deepValidate: false)
    }
}
