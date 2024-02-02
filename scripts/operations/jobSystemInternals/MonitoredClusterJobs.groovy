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

/**
 * Lists the cluster jobs which are currently being monitored by the {@link OldClusterJobMonitor}.
 */

import de.dkfz.tbi.otp.infrastructure.ClusterJob
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.job.scheduler.OldClusterJobMonitor

import static de.dkfz.tbi.otp.utils.CollectionUtils.atMostOneElement

ClusterJob.findAllByCheckStatus(ClusterJob.CheckStatus.CHECKING).groupBy {
    it.processingStep
}.each { ProcessingStep processingStep, List<ClusterJob> clusterJobs ->
    Process process = processingStep.process
    println "ProcessingStep ${processingStep.id}: ${processingStep.jobDefinition.name} on ${atMostOneElement(ProcessParameter.findAllByProcess(process))?.toObject()}"
    clusterJobs.each {
        println "  ${it}"
    }
}
''
