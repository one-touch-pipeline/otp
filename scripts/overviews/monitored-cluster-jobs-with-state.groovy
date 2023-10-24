/*
 * Copyright 2011-2020 The OTP authors
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

/**
 * Overview script to show all monitored cluster jobs and their state.
 *
 * The script tries to show "suspicious" activities and lists them at the end.
 *
 * Suspicious processes are processes of which all ClusterJobs are in a QUEUED state.
 * Suspicious cluster jobs are cluster jobs that are in a state, in which they should
 * be tracked, but aren't.
 *
 */

import de.dkfz.roddy.execution.jobs.*
import de.dkfz.tbi.otp.dataprocessing.ProcessingOption
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.infrastructure.ClusterJob
import de.dkfz.tbi.otp.job.processing.ClusterJobManagerFactoryService
import de.dkfz.tbi.otp.job.processing.Process
import de.dkfz.tbi.otp.job.scheduler.OldClusterJobMonitor
import de.dkfz.tbi.otp.utils.CollectionUtils

ProcessingOptionService processingOptionService = ctx.processingOptionService
ClusterJobManagerFactoryService clusterJobManagerFactoryService = ctx.clusterJobManagerFactoryService
OldClusterJobMonitor oldClusterJobMonitor = ctx.oldClusterJobMonitor

BatchEuphoriaJobManager jobManager = clusterJobManagerFactoryService.getJobManager()
Map<BEJobID, JobState> jobMap = jobManager.queryJobStatusAll()

List<Process> suspiciousProcesses = []

List<ClusterJob> monitoredClusterJobs = oldClusterJobMonitor.findAllClusterJobsToCheck()

monitoredClusterJobs.groupBy { ClusterJob clusterJob ->
    clusterJob.processingStep.process
}.each { Process process, List<ClusterJob> clusterJobs ->
    println "Process:  ${process.id}  ${process.startJobClass.split("\\.").last()}"
    clusterJobs.each {
        println "  ${it.clusterJobId}  ${jobMap[new BEJobID(it.clusterJobId)]}  ${it.clusterJobName}"
    }
    if (clusterJobs.every { jobMap[new BEJobID(it.clusterJobId)] == JobState.QUEUED }) {
        suspiciousProcesses << process
    }
}

println "\nSuspicious Processes:"
suspiciousProcesses.each { Process process ->
    println "  - ${process}"
}

println "\nSuspicious Cluster Jobs:"

Set<JobState> shouldBeTrackedStates = [
        JobState.UNSTARTED,
        JobState.HOLD,
        JobState.QUEUED,
        JobState.STARTED,
        JobState.RUNNING,
        JobState.SUSPENDED,
        JobState.ABORTED,
        JobState.UNKNOWN,
        JobState.DUMMY,
]

List<String> shouldBeTracked = jobMap.findAll { BEJobID beJob, JobState state ->
    state in shouldBeTrackedStates
}*.key*.id

List<String> untrackedClusterJobs = (shouldBeTracked - monitoredClusterJobs*.clusterJobId)
untrackedClusterJobs.each { String it ->
    println "  - ${it} ${jobMap[new BEJobID(it)]}"
}

''
