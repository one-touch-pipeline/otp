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
package de.dkfz.tbi.otp.workflow.alignment.wgbs

import groovy.util.logging.Slf4j
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.workflow.alignment.*
import de.dkfz.tbi.otp.workflow.alignment.panCancer.PanCancerCleanUpJob
import de.dkfz.tbi.otp.workflow.alignment.panCancer.PanCancerWorkflow
import de.dkfz.tbi.otp.workflow.jobs.AttachUuidJob
import de.dkfz.tbi.otp.workflow.jobs.CalculateSizeJob
import de.dkfz.tbi.otp.workflow.jobs.Job
import de.dkfz.tbi.otp.workflow.jobs.SetCorrectPermissionJob
import de.dkfz.tbi.otp.workflowExecution.*

@Component
@Slf4j
class WgbsWorkflow extends PanCancerWorkflow {

    public static final String WORKFLOW = "WGBS alignment"

    @Override
    List<Class<? extends Job>> getJobList() {
        return [
                RoddyAlignmentFragmentJob,
//                RoddyAlignmentCheckFragmentKeysJob,
                // will be uncommented after default fragments have been adapted
                // Keep the order, since it is important
//                RoddyAlignmentCreateNotificationJob,
                AttachUuidJob,
                RoddyAlignmentConditionalFailJob,
                WgbsPrepareJob,
                WgbsExecuteJob,
                WgbsValidationJob,
                WgbsParseJob,
                RoddyAlignmentCheckQcJob,
                PanCancerCleanUpJob,
                SetCorrectPermissionJob,
                CalculateSizeJob,
                WgbsLinkJob,
                RoddyAlignmentFinishJob,
        ]
    }

    /**
     * There is nothing to do in a single run workflow.
     */
    @Override
    void reconnectDependencies(Artefact artefact, WorkflowArtefact newWorkflowArtefact) {
    }

    final String userDocumentation = "notification.template.references.alignment.pancancer"
}
