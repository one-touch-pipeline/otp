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
package de.dkfz.tbi.otp.workflow.bamImport

import groovy.util.logging.Slf4j
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.workflow.jobs.AttachUuidJob
import de.dkfz.tbi.otp.workflow.jobs.SetCorrectPermissionJob
import de.dkfz.tbi.otp.workflowExecution.*

/**
 * Represents the bam import workflow
 */
@Component
@Slf4j
class BamImportWorkflow implements OtpWorkflow {

    public static final String WORKFLOW = "Externally merged BAM files installation"
    public static final String OUTPUT_BAM = "Bam"

    @Override
    List<String> getJobBeanNames() {
        return [
                // BamImportFragmentJob.simpleName.uncapitalize(),
                // BamImportConditionalFailJob.simpleName.uncapitalize(),
                AttachUuidJob.simpleName.uncapitalize(),
                // BamImportPrepareJob.simpleName.uncapitalize(),
                // BamImportCopyJob.simpleName.uncapitalize(),
                // BamImportValidationJob.simpleName.uncapitalize(),
                // BamImportLinkSourceJob.simpleName.uncapitalize(),
                // BamImportReplaceSourceJob.simpleName.uncapitalize(),
                SetCorrectPermissionJob.simpleName.uncapitalize(),
                // CalculateFileSizeJob.simpleName.uncapitalize(),
                BamImportLinkJob.simpleName.uncapitalize(),
                // BamImportFinishJob.simpleName.uncapitalize(),
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
