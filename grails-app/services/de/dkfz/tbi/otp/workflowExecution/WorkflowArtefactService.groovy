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

import de.dkfz.tbi.otp.ngsdata.Individual
import de.dkfz.tbi.otp.ngsdata.SeqType

@Transactional
class WorkflowArtefactService {

    /**
     * Creates a new unflushed WorkflowArtefact.
     *
     * The command creates a WorkflowArtefact with all the given parameter, save it in hibernate but do not flush it the database to improve the performance.
     * Therefore it is necessary to do somewhere later in the transaction a <b> flush </b> to get it in the database.
     *
     * @param run The workflowRun this artefact s produced by or null if it is an initial artefact
     * @param role The role this artefact is produced by or null if it is an initial artefact
     * @param individual The Individual the artefact is connect by
     * @param seqType The SeqType the artefact is connect by
     * @param name A name for the artefact. It is used in the GUI to show and also for filtering
     * @return the created, saved but not flushed WorkflowArtefact
     */
    WorkflowArtefact buildWorkflowArtefact(WorkflowRun run, String role, Individual individual, SeqType seqType, String name) {
        return new WorkflowArtefact([
                producedBy      : run,
                outputRole      : role,
                withdrawnDate   : null,
                withdrawnComment: null,
                state           : WorkflowArtefact.State.PLANNED_OR_RUNNING,
                individual      : individual,
                seqType         : seqType,
                displayName     : name,
        ]).save(flush: false)
    }
}
