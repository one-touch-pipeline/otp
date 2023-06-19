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

import grails.gorm.hibernate.annotation.ManagedEntity

import de.dkfz.tbi.otp.Commentable
import de.dkfz.tbi.otp.utils.Entity

import java.time.LocalDate

@ManagedEntity
class WorkflowVersion implements Entity, Commentable, Comparable<WorkflowVersion> {

    Workflow workflow
    String workflowVersion
    LocalDate deprecatedDate

    String getDisplayName() {
        return "${workflow.name} ${workflowVersion}"
    }

    static Closure constraints = {
        deprecatedDate nullable: true
        comment nullable: true
        workflow unique: 'workflowVersion'
    }

    static Closure mapping = {
        workflow index: "workflow_version_workflow_idx"
    }

    @Override
    String toString() {
        return displayName
    }

    @Override
    int compareTo(WorkflowVersion wv) {
        String[] s1 = splitByDot(this.workflowVersion)
        String[] s2 = splitByDot(wv.workflowVersion)

        for (int i = 0; i < Math.min(s1.length, s2.length); i++) {
            int result = compareToken(s1[i], s2[i])
            if (result != 0) {
                return -result
            }
        }
        return 0
    }

    private String[] splitByDot(String s) {
        return s.split(/[-\/.]/)
    }

    private int compareToken(String s1, String s2) {
        if (s1.isInteger() && s2.isInteger()) {
            return s1 as Integer <=> s2 as Integer
        }
        return String.CASE_INSENSITIVE_ORDER.compare(s1, s2)
    }
}
