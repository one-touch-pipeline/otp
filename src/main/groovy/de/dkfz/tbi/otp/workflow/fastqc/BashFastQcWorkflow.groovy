/*
 * Copyright 2011-2021 The OTP authors
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
package de.dkfz.tbi.otp.workflow.fastqc

import groovy.util.logging.Slf4j
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.workflowExecution.*

/**
 * Represents the fastqc workflow, which calculates or copies the fastqc reports.
 */
@Component
@Slf4j
class BashFastQcWorkflow implements OtpWorkflow {

    public static final String WORKFLOW = "Bash Fastqc"
    public static final String INPUT_FASTQ = "FASTQ"
    public static final String OUTPUT_FASTQC = "FASTQC"

    @Override
    List<String> getJobBeanNames() {
        return [
                FastqcFragmentJob.simpleName.uncapitalize(),
                FastqcConditionalFailJob.simpleName.uncapitalize(),
                FastqcPrepareJob.simpleName.uncapitalize(),
                FastqcExecuteClusterPipelineJob.simpleName.uncapitalize(),
                FastqcClusterValidationJob.simpleName.uncapitalize(),
                FastqcParseJob.simpleName.uncapitalize(),
                FastqcFinishJob.simpleName.uncapitalize(),
        ]
    }

    /**
     * Since it is designed for one run, it passes the artefact as it is.
     */
    @Override
    Artefact createCopyOfArtefact(Artefact artefact) {
        return artefact
    }

    /**
     * There is nothing to do in a single run workflow.
     */
    @Override
    void reconnectDependencies(Artefact artefact, WorkflowArtefact newWorkflowArtefact) {
    }

    final String userDocumentation = null

    @Override
    boolean isAlignment() {
        return false
    }

    @Override
    boolean isAnalysis() {
        return false
    }
}
