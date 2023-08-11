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
package de.dkfz.tbi.otp.workflowTest

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import de.dkfz.tbi.otp.workflowExecution.WorkflowArtefact
import de.dkfz.tbi.otp.workflowExecution.WorkflowRun
import de.dkfz.tbi.otp.workflowExecution.decider.Decider
import de.dkfz.tbi.otp.workflowExecution.decider.DeciderResult

/**
 * Provides using decider
 */
abstract class AbstractDecidedWorkflowSpec extends AbstractWorkflowSpec {

    //@Slf4j does not work with Spock containing tests and produces problems in closures
    @SuppressWarnings('PropertyName')
    final static Logger log = LoggerFactory.getLogger(AbstractDecidedWorkflowSpec)

    abstract protected Decider getDecider()

    protected void decide(int expectedExistingWorkflowArtefactCount, int expectedNewWorkflowArtefactCount) {
        List<WorkflowArtefact> workflowArtefacts = WorkflowArtefact.list().sort { it.id }
        assert workflowArtefacts.size() == expectedExistingWorkflowArtefactCount
        log.debug("Decide input artefacts ${workflowArtefacts.size()}:")
        workflowArtefacts.each {
            log.debug("- ${it} (${it.artefactType}) for ${it.artefact}")
        }
        log.debug("Run decider:")
        DeciderResult deciderResult = decider.decide(WorkflowArtefact.list())
        log.debug("Decide result ${deciderResult}")
        List<WorkflowArtefact> newWorkflowArtefact = deciderResult.newArtefacts
        log.debug("Decide output artefacts ${newWorkflowArtefact.size()}:")
        newWorkflowArtefact.each {
            log.debug("- ${it.toString().replaceAll('\n', ' ')} (${it.artefactType}) for ${it.artefact}")
        }
        log.debug("Created runs:")
        newWorkflowArtefact*.producedBy.unique().sort { it.id }.eachWithIndex { WorkflowRun workflowRun, int i ->
            log.debug("  - run ${i}: ${workflowRun.shortDisplayName}")
        }
        assert newWorkflowArtefact.size() == expectedNewWorkflowArtefactCount
    }
}
