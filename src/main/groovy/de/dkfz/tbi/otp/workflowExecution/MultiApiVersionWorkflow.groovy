/*
 * Copyright 2011-2026 The OTP authors
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
package de.dkfz.tbi.otp.workflowExecution

trait MultiApiVersionWorkflow extends OtpWorkflow {

    /**
     * @return the list of all jobs of the workflow in the correct order
     */
    abstract List<String> getJobList(Integer identifier)

    /**
     * @return the bean name of the first job of the workflow
     */
    @Override
    String getFirstJobBeanName(WorkflowRun workflowRun) {
        Integer identifier = workflowRun.workflowVersion.apiVersion.identifier
        List<String> jobBeanNames = getJobList(identifier)
        if (!jobBeanNames) {
            throw new IllegalArgumentException("No job list configured for API version identifier '${identifier}'")
        }
        return jobBeanNames.first()
    }

    /**
     * @return the bean name of the next job based on the given workflow step, or null if the given step is the last step
     */
    @Override
    String getNextJobBeanName(WorkflowStep workflowStep) {
        Integer identifier = workflowStep.workflowRun.workflowVersion.apiVersion.identifier
        List<String> jobBeanNames = getJobList(identifier)
        if (!jobBeanNames) {
            throw new IllegalArgumentException("No job list configured for API version identifier '${identifier}'")
        }
        int idx = jobBeanNames.indexOf(workflowStep.beanName)
        if (idx < 0) {
            throw new IllegalStateException("Workflow step bean '${workflowStep.beanName}' is not part of API version '${identifier}' job list")
        }
        return (idx < jobBeanNames.size() - 1) ?
                jobBeanNames[idx + 1] :
                null
    }
}
