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

import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import spock.lang.Shared
import spock.lang.Specification

import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.workflowExecution.ProcessingPriority
import de.dkfz.tbi.otp.workflowExecution.Workflow
import de.dkfz.tbi.otp.workflowExecution.WorkflowRun

import java.time.Duration
import java.time.ZoneId
import java.time.ZonedDateTime

class ClusterJobDetailServiceSpec extends Specification implements DataTest, ServiceUnitTest<ClusterJobDetailService>, WorkflowSystemDomainFactory {

    ClusterJobDetailService clusterJobDetailService = new ClusterJobDetailService()

    static final int REQUESTED_MEMORY = 1000
    static final int USED_MEMORY = 800
    static final int CPU_TIME = 12 * 60 * 60 * 1000
    static final int REQUESTED_CORES = 10
    static final int USED_CORES = 10
    static final int REQUESTED_WALLTIME = 24 * 60 * 60 * 1000
    static final int ELAPSED_WALLTIME = 24 * 60 * 60 * 1000
    static final ZonedDateTime QUEUED = ZonedDateTime.of(1993, 5, 15, 12, 0, 0, 0, ZoneId.systemDefault())
    static final ZonedDateTime STARTED = QUEUED.plusDays(1)
    static final ZonedDateTime ENDED = STARTED.plusDays(1)

    @Shared
    ClusterJob clusterJob

    @Shared
    Closure<Boolean> doublesEqual

    @Override
    Class<?>[] getDomainClassesToMock() {
        return [
                Workflow,
                ProcessingPriority,
                Project,
                WorkflowRun,
        ]
    }

    void setupSpec() {
        doublesEqual = { double d1, double d2, int p = 5 ->
            return d1.round(p) == d2.round(p)
        }

        clusterJob = createClusterJob(
                queued: QUEUED,
                started: STARTED,
                ended: ENDED,
                requestedWalltime: Duration.ofMillis(REQUESTED_WALLTIME),
                requestedCores: REQUESTED_CORES,
                usedCores: USED_CORES,
                cpuTime: Duration.ofMillis(CPU_TIME),
                requestedMemory: REQUESTED_MEMORY,
                usedMemory: USED_MEMORY
        )
    }

    void "test calculateMemoryEfficiency"() {
        expect:
        doublesEqual(clusterJobDetailService.calculateMemoryEfficiency(clusterJob), USED_MEMORY / REQUESTED_MEMORY)
    }

    void "test calculateCpuAvgUtilised"() {
        expect:
        doublesEqual(clusterJobDetailService.calculateCpuAvgUtilised(clusterJob), CPU_TIME / ELAPSED_WALLTIME)
    }

    void "test calculateElapsedWalltime"() {
        expect:
        doublesEqual(clusterJobDetailService.calculateElapsedWalltime(clusterJob).toMillis(), ELAPSED_WALLTIME)
    }

    void "test calculateWalltimeDiff"() {
        expect:
        doublesEqual(clusterJobDetailService.calculateWalltimeDiff(clusterJob).toMillis(), REQUESTED_WALLTIME - ELAPSED_WALLTIME)
    }
}
