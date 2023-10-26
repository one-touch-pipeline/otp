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

@Rollback
@Integration
class ClusterJobIntegrationSpec extends Specification implements WorkflowSystemDomainFactory {

    ProcessingOptionService processingOptionService

    void setupData() {
        findOrCreateProcessingOption([name: OptionName.STATISTICS_BASES_PER_BYTES_FASTQ, value: "1.0"])
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
