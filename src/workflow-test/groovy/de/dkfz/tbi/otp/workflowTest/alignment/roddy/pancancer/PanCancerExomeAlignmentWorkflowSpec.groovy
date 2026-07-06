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
package de.dkfz.tbi.otp.workflowTest.alignment.roddy.pancancer

import de.dkfz.tbi.otp.infrastructure.ClusterJob
import de.dkfz.tbi.otp.ngsdata.SeqType
import de.dkfz.tbi.otp.ngsdata.SeqTypeService
import de.dkfz.tbi.otp.utils.SessionUtils
import de.dkfz.tbi.otp.utils.ThreadUtils
import de.dkfz.tbi.otp.workflowExecution.WorkflowRun
import de.dkfz.tbi.otp.workflowExecution.WorkflowService

import java.util.concurrent.TimeoutException

class PanCancerExomeAlignmentWorkflowSpec extends AbstractPanCancerWorkflowWithoutAntibodySpec {

    WorkflowService workflowService

    @Override
    protected SeqType findSeqType() {
        return SeqTypeService.exomePairedSeqType
    }

    void "test kill workflow while cluster jobs are running"() {
        given:
        SessionUtils.withTransaction {
            createSeqTrack("readGroup1")
            decide(3, 1)
        }

        when: "start workflow and wait until cluster jobs are submitted (run enters RUNNING_WES)"
        startWorkflow()
        waitUntilWorkflowStarts()
        waitUntilWorkflowRunWes()

        and: "kill the workflow"
        WorkflowRun killedRun = SessionUtils.withTransaction {
            List<Long> runIds = newWorkflowRuns*.id
            WorkflowRun run = WorkflowRun.findByStateAndIdInList(WorkflowRun.State.RUNNING_WES, runIds)
            assert run: "Expected a RUNNING_WES run"
            assert run.workflowSteps?.last()?.clusterJobs?.size() > 0: "No cluster jobs found"
            workflowService.killWorkflowRun(run)
        }
        waitUntilWorkflowKilled()

        and: "wait until the cluster job monitor marks all cluster jobs as finished"
        Long killedRunId = killedRun.id
        boolean allFinished = ThreadUtils.waitFor({
            SessionUtils.withTransaction {
                WorkflowRun freshRun = WorkflowRun.get(killedRunId)
                freshRun.workflowSteps.collectMany { it.clusterJobs }.every {
                    it.checkStatus == ClusterJob.CheckStatus.FINISHED
                }
            }
        }, runningTimeout.toMillis(), 5000L)
        if (!allFinished) {
            throw new TimeoutException("Cluster jobs did not reach FINISHED state within ${runningTimeout}")
        }

        then: "workflow run is KILLED and cluster jobs exited with failure (killed)"
        WorkflowRun.State runState = null
        List<ClusterJob> runJobs = null
        SessionUtils.withTransaction {
            WorkflowRun freshRun = WorkflowRun.get(killedRun.id)
            runState = freshRun.state
            runJobs = freshRun.workflowSteps.collectMany { it.clusterJobs } as List<ClusterJob>
        }
        runState == WorkflowRun.State.KILLED
        // checkStatus FINISHED: monitor confirmed the cluster-side final state was retrieved
        runJobs.every { it.checkStatus == ClusterJob.CheckStatus.FINISHED }
        // exitStatus FAILED: killed jobs always exit non-zero; null means scheduler did not provide exit code
        runJobs.every { it.exitStatus == ClusterJob.Status.FAILED || it.exitStatus == null }
    }
}
