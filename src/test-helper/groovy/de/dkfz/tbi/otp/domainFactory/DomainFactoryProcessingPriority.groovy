/*
 * Copyright 2011-2024 The OTP authors
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
package de.dkfz.tbi.otp.domainFactory

import de.dkfz.tbi.otp.workflowExecution.ProcessingPriority

trait DomainFactoryProcessingPriority implements DomainFactoryCore {

    final static String PRIORITY_NAME_MINIMUM = 'MINIMUM'
    final static String PRIORITY_NAME_NORMAL = 'NORMAL'
    final static String PRIORITY_NAME_FASTTRACK = 'FASTTRACK'
    final static String PRIORITY_NAME_MAXIMUM = 'MAXIMUM'

    final static int PRIORITY_VALUE_MINIMUM = ProcessingPriority.MINIMUM
    final static int PRIORITY_VALUE_NORMAL = ProcessingPriority.NORMAL
    final static int PRIORITY_VALUE_FASTTRACK = ProcessingPriority.FAST_TRACK
    final static int PRIORITY_VALUE_MAXIMUM = ProcessingPriority.SUPREMUM - 1

    final static String ERROR_SUFFIX = " ERROR"

    ProcessingPriority findOrCreateProcessingPriority(Map properties = [:]) {
        return findOrCreateDomainObject(ProcessingPriority, [
                name                       : "name_${nextId}",
                priority                   : nextId,
                errorMailPrefix            : "errorMailPrefix_${nextId} ${ERROR_SUFFIX}",
                queue                      : "queue_${nextId}",
                roddyConfigSuffix          : "roddyConfigSuffix${nextId}",
                allowedParallelWorkflowRuns: 1,
        ], properties)
    }

    ProcessingPriority findOrCreateProcessingPriorityNormal() {
        return findOrCreateProcessingPriority([
                name             : PRIORITY_NAME_NORMAL,
                priority         : PRIORITY_VALUE_NORMAL,
                errorMailPrefix  : PRIORITY_NAME_NORMAL + ERROR_SUFFIX,
                queue            : PRIORITY_NAME_NORMAL,
                roddyConfigSuffix: PRIORITY_NAME_NORMAL,
        ])
    }

    ProcessingPriority findOrCreateProcessingPriorityFastrack() {
        return findOrCreateProcessingPriority([
                name             : PRIORITY_NAME_FASTTRACK,
                priority         : PRIORITY_VALUE_FASTTRACK,
                errorMailPrefix  : PRIORITY_NAME_FASTTRACK + ERROR_SUFFIX,
                queue            : PRIORITY_NAME_FASTTRACK,
                roddyConfigSuffix: PRIORITY_NAME_FASTTRACK,
        ])
    }

    ProcessingPriority findOrCreateProcessingPriorityMinimum() {
        return findOrCreateProcessingPriority([
                name             : PRIORITY_NAME_MINIMUM,
                priority         : PRIORITY_VALUE_MINIMUM,
                errorMailPrefix  : PRIORITY_NAME_MINIMUM + ERROR_SUFFIX,
                queue            : PRIORITY_NAME_MINIMUM,
                roddyConfigSuffix: PRIORITY_NAME_MINIMUM,
        ])
    }

    ProcessingPriority findOrCreateProcessingPriorityMaximum() {
        return findOrCreateProcessingPriority([
                name             : PRIORITY_NAME_MAXIMUM,
                priority         : PRIORITY_VALUE_MAXIMUM,
                errorMailPrefix  : PRIORITY_NAME_MAXIMUM + ERROR_SUFFIX,
                queue            : PRIORITY_NAME_MAXIMUM,
                roddyConfigSuffix: PRIORITY_NAME_MAXIMUM,
        ])
    }
}
