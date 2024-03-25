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
package de.dkfz.tbi.otp.workflow.analysis.aceseq

import de.dkfz.tbi.otp.dataprocessing.aceseq.AceseqInstance
import de.dkfz.tbi.otp.dataprocessing.sophia.SophiaInstance
import de.dkfz.tbi.otp.workflow.analysis.AbstractAnalysisWorkflow
import de.dkfz.tbi.otp.workflow.analysis.AnalysisWorkflowShared
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

trait AceseqWorkflowShared extends AnalysisWorkflowShared {

    private static final String WORKFLOW = AceseqWorkflow.WORKFLOW
    private static final String SOPHIA_INPUT = "SOPHIA"
    private static final String ACESEQ_OUTPUT = AbstractAnalysisWorkflow.ANALYSIS_OUTPUT

    SophiaInstance getSophiaInstance(WorkflowStep workflowStep) {
        checkWorkflowName(workflowStep, WORKFLOW)
        return concreteArtefactService.<SophiaInstance> getInputArtefact(workflowStep, SOPHIA_INPUT)
    }

    AceseqInstance getAceseqInstance(WorkflowStep workflowStep) {
        checkWorkflowName(workflowStep, WORKFLOW)
        return concreteArtefactService.<AceseqInstance> getOutputArtefact(workflowStep, ACESEQ_OUTPUT)
    }
}
