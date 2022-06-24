/*
 * Copyright 2011-2021 The OTP authors
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

package initializations.demoImports

import de.dkfz.tbi.otp.infrastructure.ClusterJob
import de.dkfz.tbi.otp.job.jobs.aceseq.ParseAceseqQcJob
import de.dkfz.tbi.otp.job.jobs.aceseq.RoddyAceseqStartJob
import de.dkfz.tbi.otp.job.plan.JobDefinition
import de.dkfz.tbi.otp.job.plan.JobExecutionPlan
import de.dkfz.tbi.otp.job.processing.*
import de.dkfz.tbi.otp.ngsdata.Realm

import java.time.ZonedDateTime

JobExecutionPlan.withTransaction {
    JobExecutionPlan jobExecutionPlan1 = new JobExecutionPlan(
            name: "Test Plan ${JobExecutionPlan.count()}",
            planVersion: 0,
            obsoleted: false,
            enabled: true,
    ).save(flush: true)

    Process process1 = new Process(
            started: new Date(),
            startJobClass: RoddyAceseqStartJob.name,
            finished: false,
            jobExecutionPlan: jobExecutionPlan1,
    ).save(flush: true)

    JobDefinition jobDefinition1 = new JobDefinition(
            plan: jobExecutionPlan1,
            name: 'Sample Job Definition 1',
            bean: ParseAceseqQcJob.simpleName.uncapitalize(),
    ).save(flush: true)

    ProcessingStep processingStep1 = new ProcessingStep(
            process: process1,
            jobClass: ParseAceseqQcJob.name,
            jobDefinition: jobDefinition1,
    ).save(flush: true)

    ProcessingStepUpdate processingStepUpdate = new ProcessingStepUpdate(
            state: ExecutionState.CREATED,
            date: new Date(),
            processingStep: processingStep1,
            version: 0,
    ).save(flush: true)

    new ProcessingStepUpdate(
            state: ExecutionState.STARTED,
            date: new Date(),
            processingStep: processingStep1,
            version: 0,
            previous: processingStepUpdate,
    ).save(flush: true)

    Realm realm = Realm.findAll().first()

    new ClusterJob([
            validated     : true,
            realm         : realm,
            clusterJobId  : "oldJob-" + processingStep1.id,
            clusterJobName: "Cluster Job " + processingStep1.id,
            jobClass      : "test",
            queued        : ZonedDateTime.now().minusDays(2),
            started       : ZonedDateTime.now().minusDays(1),
            ended         : ZonedDateTime.now(),
            userName      : "Test User",
            checkStatus   : ClusterJob.CheckStatus.CREATED,
            oldSystem     : true,
            processingStep: processingStep1,
    ]).save(flush: true)
}
