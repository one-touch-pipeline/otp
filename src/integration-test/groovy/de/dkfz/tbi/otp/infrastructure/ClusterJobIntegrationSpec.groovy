/*
 * Copyright 2011-2023 The OTP authors
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

import grails.testing.mixin.integration.Integration
import grails.gorm.transactions.Rollback
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.otp.dataprocessing.ProcessingOption.OptionName
import de.dkfz.tbi.otp.dataprocessing.ProcessingOptionService
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory
import de.dkfz.tbi.otp.ngsdata.DomainFactory

import java.time.Duration
import java.time.ZoneId
import java.time.ZonedDateTime

@Rollback
@Integration
class ClusterJobIntegrationSpec extends Specification implements WorkflowSystemDomainFactory {

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

    ProcessingOptionService processingOptionService

    void setupData() {
        findOrCreateProcessingOption([name: OptionName.STATISTICS_BASES_PER_BYTES_FASTQ, value: "1.0"])
    }

    void "test getter"() {
        given:
        setupData()

        ClusterJob clusterJob = createClusterJob(
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

        expect:
        Closure<Boolean> doublesEqual = { double d1, double d2, int p = 5 ->
            return d1.round(p) == d2.round(p)
        }

        doublesEqual(clusterJob.memoryEfficiency, USED_MEMORY / REQUESTED_MEMORY)
        doublesEqual(clusterJob.cpuTimePerCore, CPU_TIME / USED_CORES)
        doublesEqual(clusterJob.cpuAvgUtilised, CPU_TIME / ELAPSED_WALLTIME)
        doublesEqual(clusterJob.elapsedWalltime.toMillis(), ELAPSED_WALLTIME)
        doublesEqual(clusterJob.walltimeDiff.toMillis(), REQUESTED_WALLTIME - ELAPSED_WALLTIME)
    }

    @Unroll
    void "test nullable constraint with #notNullableProperty = null"() {
        given:
        setupData()

        ClusterJob clusterJob = createClusterJob([
                seqType                   : null,
                nBases                    : null,
                nReads                    : null,
                fileSize                  : null,
                basesPerBytesFastq        : null,
                xten                      : null,
                jobLog                    : null,
                exitStatus                : null,
                exitCode                  : null,
                eligible                  : null,
                started                   : null,
                ended                     : null,
                requestedWalltime         : null,
                requestedCores            : null,
                usedCores                 : null,
                cpuTime                   : null,
                requestedMemory           : null,
                usedMemory                : null,
                node                      : null,
                usedSwap                  : null,
                accountName               : null,
                individual                : null,
        ])

        when:
        if (notNullableProperty) {
            clusterJob."$notNullableProperty" = null
        }
        clusterJob.validate()

        then:
        clusterJob.errors.allErrors.empty == (notNullableProperty == null)

        where:
        notNullableProperty << [null, "clusterJobId", "userName", "clusterJobName", "jobClass", "queued"]
    }

    @Unroll
    void "beforeValidate, #testSubject"() {
        given:
        setupData()

        double basesPerBytesFastQFactor = processingOptionService.findOptionAsDouble(OptionName.STATISTICS_BASES_PER_BYTES_FASTQ)

        when:
        ClusterJob clusterJob = DomainFactory.createClusterJob([fileSize: fileSize, nBases: nBases])

        if (changedFileSize) {
            clusterJob.fileSize = changedFileSize
            assert clusterJob.save(flush: true)
        }

        then:
        clusterJob.nBases == nBases ? nBases : (clusterJob.fileSize * basesPerBytesFastQFactor) as Long
        clusterJob.basesPerBytesFastq == nBases ? null : basesPerBytesFastQFactor

        where:
        fileSize | nBases | changedFileSize || testSubject
        100L     | null   | null            || "when fileSize is given and nBases is null, should fill nBases"
        1000L    | 100L   | null            || "does not set basesPerBytesFastq, because fileSize and nBases are set"
        null     | null   | 100L            || "sets basesPerBytesFastq and nBases, because nBases is null and fileSize is updated"
    }
}
