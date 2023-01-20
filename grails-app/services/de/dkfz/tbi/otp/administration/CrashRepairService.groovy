/*
 * Copyright 2011-2022 The OTP authors
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
package de.dkfz.tbi.otp.administration

import grails.gorm.transactions.Transactional
import groovy.transform.CompileDynamic
import org.hibernate.criterion.CriteriaSpecification
import org.hibernate.sql.JoinType

import de.dkfz.tbi.otp.workflowExecution.WorkflowService
import de.dkfz.tbi.otp.workflowExecution.WorkflowStateChangeService
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

@CompileDynamic
@Transactional
class CrashRepairService {

    WorkflowStateChangeService workflowStateChangeService
    WorkflowService workflowService

    List<WorkflowStep> findStillRunningWorkflowStepsAfterCrash() {
        return WorkflowStep.createCriteria().list {
            eq("state", WorkflowStep.State.RUNNING)

            resultTransformer(CriteriaSpecification.ALIAS_TO_ENTITY_MAP)
            createAlias("workflowRun", "run", JoinType.INNER_JOIN)
            createAlias("run.workflow", "wf", JoinType.INNER_JOIN)

            projections {
                property("id", "id")
                property("beanName", "beanName")
                property("lastUpdated", "lastUpdated")
                property("run.id", "workflowRunId")
                property("run.displayName", "workflowRunName")
                property("run.shortDisplayName", "workflowRunShortName")
                property("run.jobCanBeRestarted", "workflowRunJobCanBeRestarted")
                property("wf.id", "workflowId")
                property("wf.name", "workflowName")
            }
        }
    }

    void restartWorkflowRun(WorkflowStep step) {
        workflowStateChangeService.changeStateToFailedWithManualChangedError(step)
        workflowService.createRestartedWorkflow(step)
    }
}
