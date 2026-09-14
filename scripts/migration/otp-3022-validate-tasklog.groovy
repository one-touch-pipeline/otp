/*
 * Copyright 2011-2026 The OTP authors
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

import de.dkfz.tbi.otp.workflow.restartHandler.WorkflowJobErrorDefinition

/**
 * Convenient script to create Restart Handler's error definitions for the case
 * "Unknow error 512"
 */
WorkflowJobErrorDefinition.withTransaction {
    ["panCancerValidationJob", "rnaAlignmentValidationJob", "wgbsValidationJob"].each {
        WorkflowJobErrorDefinition workflowJobErrorDefinition = new WorkflowJobErrorDefinition([
                jobBeanName: it,
                sourceType: WorkflowJobErrorDefinition.SourceType.CLUSTER_JOB,
                name: "Unknown error 512 " + it,
                action: WorkflowJobErrorDefinition.Action.RESTART_WORKFLOW,
                errorExpression: "Unknown error 512",
                allowRestartingCount: 3,
                beanToRestart: null,
                mailText: "Found \"Unknown error 512\" in a cluster job ${it}, even though the job is marked as success",
                checkClusterLogOnSuccess: true,
        ])
        workflowJobErrorDefinition.save(flush: true)
    }
}
