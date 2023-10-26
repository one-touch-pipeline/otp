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
package de.dkfz.tbi.otp.job.processing

import grails.testing.mixin.integration.Integration
import grails.gorm.transactions.Rollback
import org.junit.Test

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.infrastructure.*
import de.dkfz.tbi.otp.ngsdata.DomainFactory
import de.dkfz.tbi.otp.ngsdata.Realm

@Rollback
@Integration
class AbstractMaybeSubmitWaitValidateJobIntegrationTests extends TestCase {

    ClusterJobService clusterJobService
    AbstractMaybeSubmitWaitValidateJob abstractMaybeSubmitWaitValidateJob
    ProcessingStep processingStep

    void setupData() {
        abstractMaybeSubmitWaitValidateJob = [:] as AbstractMaybeSubmitWaitValidateJob
        processingStep = DomainFactory.createProcessingStep()
        abstractMaybeSubmitWaitValidateJob.setProcessingStep(processingStep)
        abstractMaybeSubmitWaitValidateJob.clusterJobService = clusterJobService
    }

    @Test
    void testCreateExceptionString() {
        setupData()
        Realm realm = DomainFactory.createRealm()

        ClusterJob clusterJob1 = clusterJobService.createClusterJob(realm, "1111", 'user', processingStep)
        ClusterJobIdentifier identifier1 = new ClusterJobIdentifier(clusterJob1)

        ClusterJob clusterJob2 = clusterJobService.createClusterJob(realm, "2222", 'user', processingStep)
        ClusterJobIdentifier identifier2 = new ClusterJobIdentifier(clusterJob2)

        Map failedClusterJobs = [(identifier2): "Failed2.", (identifier1): "Failed1."]
        List finishedClusterJobs = [identifier2, identifier1]

        String expected = """\

            2 of 2 cluster jobs failed:

            ${identifier1}: Failed1.
            Log file: ${clusterJob1.jobLog}

            ${identifier2}: Failed2.
            Log file: ${clusterJob2.jobLog}
            """.stripIndent()

        String actual = abstractMaybeSubmitWaitValidateJob.createExceptionString(failedClusterJobs, finishedClusterJobs)

        assert expected == actual
    }
}
