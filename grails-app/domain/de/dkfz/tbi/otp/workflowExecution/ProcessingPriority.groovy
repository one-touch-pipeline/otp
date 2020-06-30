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

import groovy.transform.ToString

import de.dkfz.tbi.otp.utils.Entity

@ToString(includeNames = true)
class ProcessingPriority implements Entity {

    //Constant needed for start jobs/tests of old workflow system
    @Deprecated
    static final int SUPREMUM = Integer.MAX_VALUE

    //Constant needed for start jobs/tests of old workflow system
    @Deprecated
    static final int MINIMUM = Integer.MIN_VALUE

    //Constant needed for start jobs/tests of old workflow system
    @Deprecated
    static final int FAST_TRACK = 1000

    //Constant needed for start jobs/tests of old workflow system
    @Deprecated
    static final int NORMAL = 0

    /**
     * A name for the user
     */
    String name

    /**
     * The priority for sorting. Higher values have higher priority.
     */
    int priority

    /**
     * the prefix to use for error mails
     */
    String errorMailPrefix

    /**
     * Name of the cluster queue to used for jobs.
     * Not used for jobs submitted via roddy, therefore the property {@link #roddyConfigSuffix} is used.
     */
    String queue

    /**
     * suffix used for selection of the roddy config. Usually the same as queue.
     */
    String roddyConfigSuffix

    /**
     * The parallel count of allowed workflows. Workflows of this priority are only started, if the total count of running workflows are less then this count.
     */
    int allowedParallelWorkflowRuns

    static constraints = {
        name blank: false, unique: true
        priority unique: true
        queue blank: false
        roddyConfigSuffix blank: false
        allowedParallelWorkflowRuns min: 1
    }

    static mapping = {
        allowedParallelWorkflowRuns index: 'processing_priority_allowed_parallel_workflow_runs_idx'
    }
}
