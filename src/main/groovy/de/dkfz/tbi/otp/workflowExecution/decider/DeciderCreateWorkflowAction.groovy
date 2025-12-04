/*
 * Copyright 2011-2025 The OTP authors
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

import groovy.transform.TupleConstructor

/**
 * Actions that deciders can take when creating a specific workflow.
 *
 * Names and descriptions of the actions are defined in the resource bundle, which are used to show in the UI.
 * All texts are prefixed with 'deciderCreateWorkflowAction'.
 *
 * @see "The messages.properties file with key prefix: deciderCreateWorkflowAction"
 */
@TupleConstructor
enum DeciderCreateWorkflowAction {

    CREATE_MISSING(1),
    CREATE_MISSING_AND_NEWER(2),
    CREATE_ALWAYS(3),
    SKIP(4)

    final int id

    /**
     * Returns the DeciderCreateWorkflowAction enum value for the given id
     * @param id the id of the DeciderCreateWorkflowAction
     * @return the DeciderCreateWorkflowAction enum value or null if not found
     */
    static DeciderCreateWorkflowAction getById(int id) {
        return values().find { it.id == id }
    }
}
