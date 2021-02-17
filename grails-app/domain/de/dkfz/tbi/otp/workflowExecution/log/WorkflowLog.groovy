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
package de.dkfz.tbi.otp.workflowExecution.log

import de.dkfz.tbi.otp.utils.Entity
import de.dkfz.tbi.otp.workflowExecution.WorkflowStep

/**
 * Base class of different types of log messages for the workflow system.
 *
 * The objects should be created in a separate transaction to be sure they consist in case a rollback occurs.
 */
abstract class WorkflowLog implements Entity {

    WorkflowStep workflowStep

    String createdBy

    abstract String displayLog()

    static mapping = {
        /**
         * we do not use table-per-hierarchy here as WorkflowLog serves more as an interface
         * but Grails does not seem to support relations with interfaces
         */
        tablePerHierarchy false
    }

    static constraints = {
        createdBy nullable: false
    }
}
