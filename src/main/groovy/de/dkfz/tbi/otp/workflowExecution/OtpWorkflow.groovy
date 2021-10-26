/*
 * Copyright 2011-2020 The OTP authors
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

/**
 * Describes a workflow by returning all job bean names in the correct order
 */
trait OtpWorkflow {

    /**
     * returns the list of all jobs of the workflow in the correct order.
     */
    abstract List<String> getJobBeanNames()

    /**
     * Create new concrete artefact for the restarted workflow based on a given artefact.
     *
     * @param artefact to copy
     * @return new artefact
     */
    abstract Artefact createCopyOfArtefact(Artefact artefact)

    /**
     * Reconnect the dependencies of an artefact to a concrete workflow artefact.
     *
     * @param artefact with dependency connection as reference
     * @param newWorkflowArtefact which should be reconnected to the dependencies
     */
    abstract void reconnectDependencies(Artefact artefact, WorkflowArtefact newWorkflowArtefact)

    /**
     * Indicate, if an workflow use their output artefacts also for input.
     *
     * Usually a workflow works on input artefacts and produce from these output artefacts.
     * But their are some special workflows used complete a gui import.
     * These workflows has no separate input artefact, but only output artefacts.
     * Some operation needs to know that, since they have to do something a little bit another way.
     *
     * @return false to indicate, that this workflow use separate input artefacts
     */
    boolean useOutputArtefactAlsoAsInputArtefact() {
        return false
    }

    /**
     * Return message code for user documentation and code links,
     * to include in notification emails or show in user interface
     *
     * @return message code or null if not available
     */
    abstract String getUserDocumentation()
}
