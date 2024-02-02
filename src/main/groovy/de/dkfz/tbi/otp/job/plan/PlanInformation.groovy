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
package de.dkfz.tbi.otp.job.plan

/**
 * Class representing a serialized form of the JobExecutionPlan
 * including information about all JobDefinitions, all Parameters,
 * Mappings and so on.
 */
class PlanInformation {

    /**
     * The name of the JobExecutionPlan
     */
    String name
    /**
     * All the Jobs of the JobExecutionPlan, the actual connections between
     * the Jobs are provided by the connections.
     */
    List<JobInformation> jobs = []
    /**
     * All the connections between the Jobs.
     */
    List<JobConnection> connections = []

    /**
     * Creates the PlanInformation for the given JobExecutionPlan
     */
    static PlanInformation fromPlan(JobExecutionPlan plan) {
        PlanInformation ret = new PlanInformation()
        ret.name = plan.name

        ret.jobs << JobInformation.fromJob(plan.startJob)
        ret.connections << new JobConnection(plan.startJob.id, plan.firstJob.id)
        JobDefinition job = plan.firstJob
        while (job) {
            ret.jobs << JobInformation.fromJob(job)
            if (job.next) {
                ret.connections << new JobConnection(job.id, job.next.id)
            }
            job = job.next
        }
        return ret
    }
}
