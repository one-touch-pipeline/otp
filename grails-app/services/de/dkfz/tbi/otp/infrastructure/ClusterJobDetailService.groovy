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

import de.dkfz.tbi.otp.utils.TimeUtils

import java.time.Duration

class ClusterJobDetailService {

    static final String NOT_AVAILABLE = "N/A"

    /**
     * describes how efficient the memory was used
     * usedMemory divided by requestedMemory
     */
    Double calculateMemoryEfficiency(ClusterJob clusterJob) {
        if (clusterJob.usedMemory != null && clusterJob.requestedMemory != null) {
            return (((double) clusterJob.usedMemory) / clusterJob.requestedMemory)
        }
        return null
    }

    Double calculateCpuAvgUtilised(ClusterJob clusterJob) {
        if (clusterJob.cpuTime != null && calculateElapsedWalltime(clusterJob) != null && calculateElapsedWalltime(clusterJob).toMillis() != 0) {
            return ((double) clusterJob.cpuTime.toMillis()) / calculateElapsedWalltime(clusterJob).toMillis()
        }
        return null
    }

    /**
     * Result will be in seconds.
     */
    Duration calculateElapsedWalltime(ClusterJob clusterJob) {
        if (clusterJob.ended != null && clusterJob.started != null) {
            return Duration.between(clusterJob.started, clusterJob.ended)
        }
        return null
    }

    /**
     * difference of requested and elapsed walltime
     */
    Duration calculateWalltimeDiff(ClusterJob clusterJob) {
        if (clusterJob.requestedWalltime != null && calculateElapsedWalltime(clusterJob) != null) {
            return clusterJob.requestedWalltime - calculateElapsedWalltime(clusterJob)
        }
        return null
    }

    String getElapsedWalltimeAsHhMmSs(ClusterJob clusterJob) {
        return TimeUtils.getFormattedDurationForZonedDateTime(clusterJob.started, clusterJob.ended) ?: NOT_AVAILABLE
    }
}
