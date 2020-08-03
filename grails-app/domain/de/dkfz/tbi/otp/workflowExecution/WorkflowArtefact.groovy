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

import de.dkfz.tbi.otp.Withdrawable
import de.dkfz.tbi.otp.ngsdata.Individual
import de.dkfz.tbi.otp.ngsdata.SeqType
import de.dkfz.tbi.otp.project.Project
import de.dkfz.tbi.otp.utils.Entity

import static de.dkfz.tbi.otp.utils.CollectionUtils.atMostOneElement

class WorkflowArtefact implements Withdrawable, Entity {

    enum State {
        PLANNED_OR_RUNNING,
        SUCCESS,
        SKIPPED,
        FAILED,
        LEGACY,
    }

    WorkflowRun producedBy

    // 'role' of the artefact for the workflow it was produced by, e.g. "disease" and "control"
    String outputRole

    State state = State.LEGACY

    Individual individual

    SeqType seqType

    String displayName

    static constraints = {
        producedBy nullable: true, validator: { val, obj ->
            if (obj.outputRole && !val) {
                return ['default.when.X.then.Y', 'set', 'outputRole', 'set']
            }
        }
        outputRole nullable: true, validator: { val, obj ->
            if (obj.producedBy && !val) {
                return ['default.when.X.then.Y', 'set', 'producedBy', 'set']
            }
        }
        withdrawnDate nullable: true
        withdrawnComment nullable: true, validator: { val, obj ->
            if (obj.withdrawnDate && !val) {
                return ['default.when.X.then.Y', 'set', 'withdrawnDate', 'set']
            }
        }
        individual nullable: false
        seqType nullable: false
        displayName blank: false, nullable: false
    }

    static mapping = {
        withdrawnComment type: "text"
        state index: 'workflow_artefact_state_idx'
        individual index: 'workflow_artefact_individual_idx'
        seqType index: 'workflow_artefact_seq_type_idx'
    }

    // gorm/hibernate ignores the property workflowArtefact of trait Artefact if this method returns Artefact
    Optional<Artefact> getArtefact() {
        Optional.ofNullable(atMostOneElement(executeQuery("FROM de.dkfz.tbi.otp.workflowExecution.Artefact WHERE workflowArtefact = :wa", [wa: this])) as Artefact)
    }

    Project getProject() {
        return individual.project
    }
}
