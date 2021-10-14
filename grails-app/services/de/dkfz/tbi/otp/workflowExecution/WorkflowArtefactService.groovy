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

import grails.gorm.transactions.Transactional
import groovy.transform.Canonical

import de.dkfz.tbi.otp.utils.StringUtils

@Transactional
class WorkflowArtefactService {

    /**
     * Creates a new unflushed WorkflowArtefact.
     *
     * The command creates a WorkflowArtefact with all the given parameter, save it in hibernate but do not flush it the database to improve the performance.
     * Therefore it is necessary to do somewhere later in the transaction a <b> flush </b> to get it in the database.
     *
     * @param values as wrapper for workflow artefact properties
     * @return the created, saved but not flushed WorkflowArtefact
     */
    //for performance we handle flushs manually
    @SuppressWarnings('NoExplicitFlushForSaveRule')
    WorkflowArtefact buildWorkflowArtefact(WorkflowArtefactValues values) {
        String displayName = StringUtils.generateMultiLineDisplayName(values.displayNameLines)

        return new WorkflowArtefact([
                producedBy      : values.run,
                outputRole      : values.role,
                withdrawnDate   : null,
                withdrawnComment: null,
                state           : WorkflowArtefact.State.PLANNED_OR_RUNNING,
                artefactType    : values.artefactType,
                displayName     : displayName,
        ]).save(flush: false)
    }
}

@Canonical
class WorkflowArtefactValues {
    WorkflowRun run
    String role
    ArtefactType artefactType
    List<String> displayNameLines
}
