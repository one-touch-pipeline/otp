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
package de.dkfz.tbi.otp.workflow.analysis

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile
import de.dkfz.tbi.otp.dataprocessing.ProcessingThresholds
import de.dkfz.tbi.otp.dataprocessing.ProcessingThresholdsService
import de.dkfz.tbi.otp.workflow.jobs.AbstractConditionalSkipJob
import de.dkfz.tbi.otp.workflow.shared.JobFailedException
import de.dkfz.tbi.otp.workflow.shared.SkipWorkflowStepException
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep
import de.dkfz.tbi.otp.workflowExecution.WorkflowStepSkipMessage

@Component
@Slf4j
class AnalysisConditionalSkipJob extends AbstractConditionalSkipJob implements AnalysisWorkflowShared {
    @Autowired
    ProcessingThresholdsService processingThresholdsService

    /** Threshold should be moved to OtpWorkflow or Workflow in the future */
    static final Double ANALYSIS_WORKFLOW_COVERAGE_THRESHOLD = 20

    @Override
    void checkRequirements(WorkflowStep workflowStep) throws SkipWorkflowStepException {
        AbstractBamFile tumorBamFile = getTumorBamFile(workflowStep)
        AbstractBamFile controlBamFile = getControlBamFile(workflowStep)

        checkCoverageForWorkflow(tumorBamFile)
        checkCoverageForWorkflow(controlBamFile)

        checkCoverageForProcessingThresholds(tumorBamFile)
        checkCoverageForProcessingThresholds(controlBamFile)
    }

    private void checkCoverageForProcessingThresholds(AbstractBamFile bamFile) throws SkipWorkflowStepException {
        ProcessingThresholds processingThresholds = processingThresholdsService.findByAbstractBamFile(bamFile)
        if (!processingThresholds) {
            throw new JobFailedException("No processing Thresholds where found for project ${bamFile.project}, sample Type " +
                    "${bamFile.sampleType} and seq type ${bamFile.seqType} for bam file ${bamFile.bamFileName}")
        }

        /** Threshold is assumed to be reached, when its not defined, since that can happen for imported bam Files */
        if (bamFile.coverage !== null && processingThresholds.coverage && bamFile.coverage < processingThresholds.coverage) {
            WorkflowStepSkipMessage skipMessage = new WorkflowStepSkipMessage([
                    message : "Coverage threshold of ${processingThresholds.coverage} for project ${processingThresholds.project}, sample Type " +
                            "${processingThresholds.sampleType} and seq type ${processingThresholds.seqType} was not reached by the " +
                            "bam file ${bamFile.bamFileName} with a coverage of ${bamFile.coverage}.",
                    category: WorkflowStepSkipMessage.Category.PROJECT_THRESHOLD_REJECTION,
            ])

            throw new SkipWorkflowStepException(skipMessage)
        }

        /** Threshold is assumed to be reached, when its not defined, since that can happen for imported bam Files */
        if (bamFile.numberOfMergedLanes !== null && processingThresholds.numberOfLanes && bamFile.numberOfMergedLanes < processingThresholds.numberOfLanes) {
            WorkflowStepSkipMessage skipMessage = new WorkflowStepSkipMessage([
                    message : "Number of lanes threshold of ${processingThresholds.numberOfLanes} for project ${processingThresholds.project}, sample Type " +
                            "${processingThresholds.sampleType} and seq type ${processingThresholds.seqType} was not reached by the " +
                            "bam file ${bamFile.bamFileName} with ${bamFile.numberOfMergedLanes} lanes.",
                    category: WorkflowStepSkipMessage.Category.PROJECT_THRESHOLD_REJECTION,
            ])

            throw new SkipWorkflowStepException(skipMessage)
        }
    }

    private void checkCoverageForWorkflow(AbstractBamFile bamFile) throws SkipWorkflowStepException {
        /** Threshold is assumed to be reached, when its not defined, since that can happen for imported bam Files */
        if (bamFile.coverage == null) {
            return
        }

        Double workflowCoverageThreshold = ANALYSIS_WORKFLOW_COVERAGE_THRESHOLD
        if (bamFile.coverage < workflowCoverageThreshold) {
            WorkflowStepSkipMessage skipMessage = new WorkflowStepSkipMessage([
                    message : "Coverage threshold of ${workflowCoverageThreshold} for BamFile ${bamFile.bamFileName} was not reached " +
                            "with a coverage of ${bamFile.coverage}.",
                    category: WorkflowStepSkipMessage.Category.WORKFLOW_COVERAGE_REJECTION,
            ])
            throw new SkipWorkflowStepException(skipMessage)
        }
    }
}
