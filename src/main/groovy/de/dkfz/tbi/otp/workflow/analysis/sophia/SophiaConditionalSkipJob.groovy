/*
 * Copyright 2011-2025 The OTP authors
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
package de.dkfz.tbi.otp.workflow.analysis.sophia

import groovy.util.logging.Slf4j
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile
import de.dkfz.tbi.otp.dataprocessing.SophiaWorkflowQualityAssessment
import de.dkfz.tbi.otp.workflow.analysis.AnalysisConditionalSkipJob
import de.dkfz.tbi.otp.workflow.shared.SkipWorkflowStepException
import de.dkfz.tbi.otp.workflowExecution.WorkflowStepSkipMessage

@Component
@Slf4j
class SophiaConditionalSkipJob extends AnalysisConditionalSkipJob implements SophiaWorkflowShared {

    @Override
    protected void checkBamFiles(AbstractBamFile bamFileDisease, AbstractBamFile bamFileControl) throws SkipWorkflowStepException {
        checkMaximalReadLengthForWorkflow(bamFileDisease)
        checkMaximalReadLengthForWorkflow(bamFileControl)

        checkQualityAssessmentForWorkflow(bamFileDisease)
        checkQualityAssessmentForWorkflow(bamFileControl)
    }

    private void checkMaximalReadLengthForWorkflow(AbstractBamFile bamFile) throws SkipWorkflowStepException {
        if (!bamFile.maximalReadLength) {
            WorkflowStepSkipMessage skipMessage = new WorkflowStepSkipMessage([
                    message : "MaximalReadLength for BamFile ${bamFile.bamFileName} was not set",
                    category: WorkflowStepSkipMessage.Category.MAXIMAL_READ_LENGTH_MISSING,
            ])
            throw new SkipWorkflowStepException(skipMessage)
        }
    }

    private void checkQualityAssessmentForWorkflow(AbstractBamFile bamFile) throws SkipWorkflowStepException {
        if (!(bamFile.qualityAssessment instanceof SophiaWorkflowQualityAssessment)) {
            WorkflowStepSkipMessage skipMessage = new WorkflowStepSkipMessage([
                    message : "No SophiaWorkflowQualityAssessment for BamFile ${bamFile.bamFileName} found",
                    category: WorkflowStepSkipMessage.Category.SOPHIA_WORKFLOW_COMPATIBLE_QUALITY_ASSESSMENT_MISSING,
            ])
            throw new SkipWorkflowStepException(skipMessage)
        }
    }
}
