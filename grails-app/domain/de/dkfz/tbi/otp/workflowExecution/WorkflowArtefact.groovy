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

    State state = State.LEGACY

    static constraints = {
        producedBy nullable: true
        withdrawnDate nullable: true
        withdrawnComment nullable: true, validator: { val, obj ->
            if (obj.withdrawnDate && !val) {
                return ['default.when.X.then.Y', 'set', 'withdrawnDate', 'set']
            }
        }
    }

    static mapping = {
        withdrawnComment type: "text"
    }

    // gorm/hibernate ignores the property workflowArtefact of trait Artefact if this method returns Artefact
    Optional<Artefact> getArtefact() {
        Optional.ofNullable(atMostOneElement(executeQuery("FROM de.dkfz.tbi.otp.workflowExecution.Artefact WHERE workflowArtefact = :wa", [wa: this])) as Artefact)
    }
}
