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
package de.dkfz.tbi.otp.infrastructure

import grails.converters.JSON
import org.springframework.security.access.prepost.PreAuthorize

import de.dkfz.tbi.otp.ngsdata.Individual
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.workflowExecution.Workflow
import de.dkfz.tbi.otp.workflowExecution.WorkflowRun
import de.dkfz.tbi.util.TimeFormats
import de.dkfz.tbi.util.TimeUtils

import java.time.Duration

@PreAuthorize("hasRole('ROLE_OPERATOR')")
class ClusterJobDetailController {

    ClusterJobService clusterJobService
    ClusterJobDetailService clusterJobDetailService

    static allowedMethods = [
            show                     : "GET",
            showLog                  : "GET",
            getStatesTimeDistribution: "GET",
    ]

    def show(NavigationCommand navigationCommand) {
        ClusterJob clusterJob = clusterJobService.findById(params.id as long)
        Individual individual = clusterJob ? clusterJobService.findProcessParameterObjectByClusterJob(clusterJob)?.individual : null
        Project project = clusterJob.oldSystem ? individual?.project : clusterJob.workflowStep.workflowRun.project
        String memoryEfficiency = clusterJobDetailService.calculateMemoryEfficiency(clusterJob)?.round(2) ?: 'N/A'
        String cpuAvgUtilised = clusterJobDetailService.calculateCpuAvgUtilised(clusterJob)?.round(2) ?: 'N/A'
        String elapsedWalltimeAsISO = TimeUtils.getFormattedDuration(clusterJobDetailService.calculateElapsedWalltime(clusterJob)) ?: 'N/A'
        String requestedWalltimeAsISO = TimeUtils.getFormattedDuration(clusterJob.requestedWalltime) ?: 'N/A'
        String walltimeDiffAsISO = TimeUtils.getFormattedDuration(clusterJobDetailService.calculateWalltimeDiff(clusterJob)) ?: 'N/A'
        String cpuTimeAsISO = TimeUtils.getFormattedDuration(clusterJob.cpuTime) ?: 'N/A'

        return [
                'job'                   : clusterJob,
                'jobQueued'             : TimeFormats.DATE_TIME.getFormattedZonedDateTime(clusterJob.queued),
                'jobStarted'            : TimeFormats.DATE_TIME.getFormattedZonedDateTime(clusterJob.started),
                'jobEnded'              : TimeFormats.DATE_TIME.getFormattedZonedDateTime(clusterJob.ended),
                'individual'            : individual,
                'project'               : project,
                'memoryEfficiency'      : memoryEfficiency,
                'cpuAvgUtilised'        : cpuAvgUtilised,
                'elapsedWalltimeAsISO'  : elapsedWalltimeAsISO,
                'requestedWalltimeAsISO': requestedWalltimeAsISO,
                'walltimeDiffAsISO'     : walltimeDiffAsISO,
                'cpuTimeAsISO'          : cpuTimeAsISO,
                'NA'                    : ClusterJob.NOT_AVAILABLE,
                'nav'                   : navigationCommand,
        ]
    }

    def showLog(NavigationCommand navigationCommand) {
        ClusterJob clusterJob = clusterJobService.findById(params.id as long)
        String content = clusterJobService.getClusterJobLog(clusterJob)
        return [
                job    : clusterJob,
                content: content,
                nav    : navigationCommand,
        ]
    }

    def getStatesTimeDistribution() {
        Map<String, Map<String, Long>> data = clusterJobService.findJobSpecificStatesTimeDistributionByJobId(params.id as Long)

        Map result = [
                keys  : ["Queue", "Process"],
                labels: ["${data.queue.percentage}% (${applyPeriodFormat(data.queue.ms)})",
                         "${data.process.percentage}% (${applyPeriodFormat(data.process.ms)})"],
                data  : [data.queue.percentage, data.process.percentage],

        ]
        render(result as JSON)
    }

    private String applyPeriodFormat(Long ms) {
        return TimeUtils.getFormattedDuration(Duration.ofMillis(ms))
    }
}

class NavigationCommand {
    WorkflowRun workflowRun
    Workflow workflow

    List<WorkflowRun.State> states
    String name

    void setState(String state) {
        states = state.split(",").collect { WorkflowRun.State.valueOf(it) } ?: []
    }
}
