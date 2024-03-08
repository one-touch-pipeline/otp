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
package de.dkfz.tbi.otp.workflow.analysis.runyapsa

import de.dkfz.tbi.otp.dataprocessing.indelcalling.IndelCallingInstance
import de.dkfz.tbi.otp.dataprocessing.runYapsa.RunYapsaInstance
import de.dkfz.tbi.otp.dataprocessing.snvcalling.RoddySnvCallingInstance
import de.dkfz.tbi.otp.workflow.analysis.AnalysisWorkflowShared
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

trait RunYapsaWorkflowShared extends AnalysisWorkflowShared {

    private static final String WORKFLOW = RunYapsaWorkflow.WORKFLOW
    private static final String SNV_INPUT = "SNV"
    private static final String INDEL_INPUT = "INDEL"
    private static final String RUNYAPSA_OUTPUT = "RUNYAPSA"

    RoddySnvCallingInstance getSnvInstance(WorkflowStep workflowStep) {
        checkWorkflowName(workflowStep, WORKFLOW)
        return concreteArtefactService.<RoddySnvCallingInstance> getInputArtefact(workflowStep, SNV_INPUT)
    }

    IndelCallingInstance getIndelInstance(WorkflowStep workflowStep) {
        checkWorkflowName(workflowStep, WORKFLOW)
        return concreteArtefactService.<IndelCallingInstance> getInputArtefact(workflowStep, INDEL_INPUT)
    }

    RunYapsaInstance getRunYapsaInstance(WorkflowStep workflowStep) {
        checkWorkflowName(workflowStep, WORKFLOW)
        return concreteArtefactService.<RunYapsaInstance> getOutputArtefact(workflowStep, RUNYAPSA_OUTPUT)
    }
}
