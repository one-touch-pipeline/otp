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
package de.dkfz.tbi.otp.workflow.restartHandler

import groovy.transform.ToString

import de.dkfz.tbi.otp.utils.Entity

import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException

/**
 * Defines a job error for the new system.
 */
@ToString(includes = ['name', 'jobBeanName', 'sourceType'])
class WorkflowJobErrorDefinition implements Entity {
    /** The type of log to use for checking the expression */
    String jobBeanName

    /** The bean name of the job the error is defined for. */
    SourceType sourceType

    /** A name for the definition. It's used in the emails .*/
    String name

    /** The action which is connected with the error. */
    Action action

    /** A regular expression which is searched in the log. */
    String errorExpression

    /** The maximal allowed count of restarts (job and workflow) the rule is allowed to use. Normally only once. */
    int allowRestartingCount

    /** If the action is RESTART_JOB, its the bean name of the job to start. */
    String beanToRestart

    /** Additional text for the mail, for example which manual steps should be done before restart. */
    String mailText

    /**
     * Defines the different sources of logs handled by the restart handler.
     */
    enum SourceType {
        /** check the error message */
        MESSAGE,
        /** check the cluster logs of jobs sent directly by OTP or by Roddy */
        CLUSTER_JOB,
        /** check the WES run log */
        WES_RUN_LOG,
        /** check the wes task logs */
        WES_TASK_LOG,
    }

    /**
     * Defines the possible actions.
     */
    enum Action {
        /** The complete workflow should restart */
        RESTART_WORKFLOW,
        /** A job should be restarted (the failed or any of the previous) */
        RESTART_JOB,
        /** Restart handler should stop */
        STOP,
    }

    static constraints = {
        errorExpression(validator: { val, obj ->
            try {
                Pattern.compile(val)
            }
            catch (PatternSyntaxException e) {
                return 'workflowJobErrorDefinition.errorExpression.invalid'
            }
            return true
        })
        mailText nullable: true, blank: false
        allowRestartingCount min: 0
        jobBeanName blank: false
        name blank: false
        beanToRestart nullable: true, blank: false, validator: { val, obj ->
            if (obj.action == Action.RESTART_JOB) {
                if (obj.beanToRestart == null) {
                    return 'workflowJobErrorDefinition.beanToRestart.null'
                }
            } else {
                if (obj.beanToRestart != null) {
                    return 'workflowJobErrorDefinition.beanToRestart.notNull'
                }
            }
        }
    }

    static mapping = {
        mailText type: "text"
        errorExpression type: "text"
    }

}
