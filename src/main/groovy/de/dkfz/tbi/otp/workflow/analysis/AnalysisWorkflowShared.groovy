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

import org.springframework.beans.factory.annotation.Autowired

import de.dkfz.tbi.otp.dataprocessing.AbstractBamFile
import de.dkfz.tbi.otp.dataprocessing.BamFilePairAnalysis
import de.dkfz.tbi.otp.workflow.ConcreteArtefactService
import de.dkfz.tbi.otp.workflow.WorkflowShared
import de.dkfz.tbi.otp.workflow.analysis.aceseq.AceseqWorkflow
import de.dkfz.tbi.otp.workflow.analysis.indel.IndelWorkflow
import de.dkfz.tbi.otp.workflow.analysis.runyapsa.RunYapsaWorkflow
import de.dkfz.tbi.otp.workflow.analysis.snv.SnvWorkflow
import de.dkfz.tbi.otp.workflow.analysis.sophia.SophiaWorkflow
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

trait AnalysisWorkflowShared extends WorkflowShared {

    private static final List<String> WORKFLOWS = [AceseqWorkflow.WORKFLOW, IndelWorkflow.WORKFLOW, SnvWorkflow.WORKFLOW, SophiaWorkflow.WORKFLOW,
                                                   RunYapsaWorkflow.WORKFLOW]
    public static final String INPUT_TUMOR_BAM = "TUMOR_BAM"
    public static final String INPUT_CONTROL_BAM = "CONTROL_BAM"
    public static final String ANALYSIS_OUTPUT = AbstractAnalysisWorkflow.ANALYSIS_OUTPUT

    @Autowired
    ConcreteArtefactService concreteArtefactService

    AbstractBamFile getTumorBamFile(WorkflowStep workflowStep) {
        checkWorkflowName(workflowStep, WORKFLOWS)
        return concreteArtefactService.<AbstractBamFile> getInputArtefact(workflowStep, INPUT_TUMOR_BAM)
    }

    AbstractBamFile getControlBamFile(WorkflowStep workflowStep) {
        checkWorkflowName(workflowStep, WORKFLOWS)
        return concreteArtefactService.<AbstractBamFile> getInputArtefact(workflowStep, INPUT_CONTROL_BAM)
    }

    BamFilePairAnalysis getOutputInstance(WorkflowStep workflowStep) {
        checkWorkflowName(workflowStep, WORKFLOWS)
        return concreteArtefactService.<BamFilePairAnalysis> getOutputArtefact(workflowStep, ANALYSIS_OUTPUT)
    }
}
