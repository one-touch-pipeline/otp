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
package de.dkfz.tbi.otp.workflowExecution.decider

import grails.util.Holders
import org.springframework.stereotype.Component

import de.dkfz.tbi.otp.workflowExecution.WorkflowArtefact

/**
knows all deciders and also the order of the deciders based on the input and output  WorkflowArtefacts
can be called during Fastq or BAM import, realignment, …
is called with a list of new/changed workflowArtefacts (see method decide in Decider)
*/
@Component
class AllDecider implements Decider {
    /** list of Deciders in the correct order */
    List<Class<Decider>> deciders = [
            FastqcDecider,
    ]

    @Override
    Collection<WorkflowArtefact> decide(Collection<WorkflowArtefact> allWorkflowArtefacts, boolean forceRun = false, Map<String, String> userParams = [:]) {
        Collection<WorkflowArtefact> newWorkflowArtefacts = []

        deciders.each { it ->
            Decider decider = Holders.grailsApplication.mainContext.getBean(it)
            Collection<WorkflowArtefact> workflowArtefacts = decider.decide(allWorkflowArtefacts, forceRun, userParams)
            allWorkflowArtefacts += workflowArtefacts
            newWorkflowArtefacts += workflowArtefacts
        }
        return newWorkflowArtefacts
    }
}
