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
package de.dkfz.tbi.otp.workflowExecution

import grails.util.Pair
import groovy.transform.CompileDynamic

import java.sql.Timestamp

@CompileDynamic
class WorkflowRunOverviewService {
    Map<Pair<WorkflowRun.State, Workflow>, Long> getNumberOfRunsPerWorkflowAndState() {
        return WorkflowRun.createCriteria().list {
            projections {
                groupProperty("state")
                groupProperty("workflow")
                count("state")
            }
        }.collectEntries {
            [(new Pair<WorkflowRun.State, Workflow>(it[0], it[1])): it[2]]
        }
    }

    Map<Workflow, Timestamp> getLastRuns() {
        return WorkflowRun.executeQuery("""
            SELECT run.workflow.id, max(step.dateCreated)
            FROM WorkflowRun run
            JOIN run.workflowSteps step
            WHERE step.previous IS NULL
            GROUP BY run.workflow.id
        """).collectEntries {
            [(Workflow.get(it[0])): it[1]]
        }
    }

    Map<Workflow, Timestamp> getLastFailedRuns() {
        return WorkflowRun.executeQuery("""
            SELECT run.workflow.id, max(step.lastUpdated)
            FROM WorkflowRun run
            JOIN run.workflowSteps step
            WHERE run.state = :state
            GROUP BY run.workflow.id
        """, [state: WorkflowRun.State.FAILED]).collectEntries {
            [(Workflow.get(it[0])): it[1]]
        }
    }
}
