/*
 * Copyright 2011-2019 The OTP authors
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
package de.dkfz.tbi.otp.job.processing

import de.dkfz.tbi.otp.job.plan.JobDefinition
import de.dkfz.tbi.otp.utils.Entity

/**
 * @deprecated class is part of the old workflow system
 */
@Deprecated
class ProcessingStepUpdate implements Serializable, Entity {

    /**
     * Link to the ProcessingStepUpdate before this one, may be null.
     */
    ProcessingStepUpdate previous

    /**
     * The new state of the ProcessingStep set with this update.
     */
    ExecutionState state

    /**
     * The date when this update was performed.
     *
     * suppressing because changing this would involve refactoring the code as well as the database columns
     */
    @SuppressWarnings("GrailsDomainReservedSqlKeywordName")
    Date date

    /**
     * The Processing Step this update belongs to.
     */
    ProcessingStep processingStep

    /**
     * The error object in case this update is a failure.
     */
    ProcessingError error

    static mapping = {
        date index: 'processing_step_update_date_idx'
        state index: 'processing_step_update_state_idx'
        processingStep index: 'processing_step_update_processing_step_idx'
        error index: 'processing_step_update_error_idx'
        previous index: 'processing_step_update_previous_idx'
    }

    static belongsTo = [
            processingStep: ProcessingStep,
    ]

    static constraints = {
        processingStep(nullable: false)
        previous(nullable: true, validator: { val, obj ->
            if (obj.id) {
                // this obj had already been validated, so no need to do it again
                return true
            }
            if (val) {
                // the previous update needs to belong to the same processing step us this update
                return val.processingStep.id == obj.processingStep.id
            }
            // we don't set a previous update, this is only allowed if the processingStep has no updates
            // or if it has one element which is this one
            List<ProcessingStepUpdate> stepUpdates = ProcessingStepUpdate.findAllByProcessingStep(obj.processingStep)
            return stepUpdates.isEmpty() ||
                    (stepUpdates.size() == 1 && stepUpdates.first() == obj)
        })
        date(nullable: false, validator: { val, obj ->
            if (obj.id) {
                // this obj had already been validated, so no need to do it again
                return true
            }
            List<ProcessingStepUpdate> stepUpdates = ProcessingStepUpdate.findAllByProcessingStep(obj.processingStep)
            if (stepUpdates.isEmpty() ||
                    (stepUpdates.size() == 1 && stepUpdates.first() == obj)) {
                // there are no processing step updates yet, so our value is acceptable
                return true
            }
            boolean ok = true
            // compare all the existing update dates with the current one, they all have to be before this one
            stepUpdates.sort { it.id }.each {
                if (it == obj) {
                    return
                }
                if (it.date > val) {
                    ok = false
                }
            }
            return ok
        })
        state(nullable: false, validator: { val, obj ->
            if (obj.id) {
                // this obj had already been validated, so no need to do it again
                return true
            }
            // the first ProcessingStepUpdate has to be CREATED, while all following can not be CREATED
            List<ProcessingStepUpdate> stepUpdates = ProcessingStepUpdate.findAllByProcessingStep(obj.processingStep)
            if (stepUpdates.isEmpty() || (stepUpdates.size() == 1 && stepUpdates.first() == obj)) {
                if (val != ExecutionState.CREATED) {
                    return "invalid.first"
                }
            } else {
                if (val == ExecutionState.CREATED) {
                    return "invalid.following"
                }
            }
        })
        error(nullable: true, validator: { val, obj ->
            if (!val) {
                return true
            }
            return obj.state == ExecutionState.FAILURE
        })
    }

    Process getProcess() {
        return processingStep.process
    }

    ProcessParameterObject getProcessParameterObject() {
        return processingStep.processParameterObject
    }

    JobDefinition getJobDefinition() {
        return processingStep.jobDefinition
    }
}
