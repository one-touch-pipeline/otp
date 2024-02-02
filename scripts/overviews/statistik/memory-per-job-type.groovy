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
 * Provides a statistic on the memory usage of the ClusterJobs from the given date until now.
 *
 * Prints out the ClusterJobs grouped by job type (job name) and seqType. For each these groups
 * it provides the average and maximum used value, as well as a list of all used values alongside
 * the number of jobs that used this value.
 */

import de.dkfz.tbi.otp.infrastructure.ClusterJob
import de.dkfz.tbi.util.UnitHelper

Date start = new Date(119, 0, 1)

String convertKiB(BigDecimal kib) {
    if (kib) {
        return UnitHelper.asBytes((kib * 1024) as Long, true)
    }
    return null
}

String getClusterJobMetrics(List<ClusterJob> clusterJobs) {
    String indent = "        "
    String overview = clusterJobs.groupBy { it.usedMemory }.sort().collect { Long usedMemory, List<ClusterJob> clusterJobsPerMemory ->
        return "${convertKiB(usedMemory)} (n=${clusterJobsPerMemory.size()})"
    }.join("\n${indent}")

    List<ClusterJob> validJobs = clusterJobs.findAll { it.usedMemory }
    Long avg = validJobs.sum { it.usedMemory } / validJobs.size()
    Long max = validJobs.max { it.usedMemory }.usedMemory

    return """\
        |${indent}avg: ${convertKiB(avg)}
        |${indent}max: ${convertKiB(max)}
        |${indent}${overview}
        |""".stripMargin()
}

ClusterJob.withCriteria {
    between("dateCreated", start, new Date())
    eq("exitStatus", ClusterJob.Status.COMPLETED)
    not {
        'in'("jobClass", [
                // excluded as those are receive their values via ProcessingOptions
                "CopyFilesJob",
                "FastqcJob",
                "ImportExternallyMergedBamJob",
                "ExecuteRunYapsaJob",
                "ExecuteCellRangerJob",
        ])
    }
}.groupBy { it.jobClass }.each { String jobName, List<ClusterJob> clusterJobsPerJob ->
    println "${jobName} (n=${clusterJobsPerJob.size()})"
    clusterJobsPerJob.groupBy { it.seqType.roddyName }.each { String roddyName, List<ClusterJob> clusterJobsPerRoddyName ->
        println "    ${roddyName} (n=${clusterJobsPerRoddyName.size()})"
        println getClusterJobMetrics(clusterJobsPerRoddyName)
    }
}

''
