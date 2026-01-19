/*
 * Copyright 2011-2026 The OTP authors
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
package de.dkfz.tbi.otp.workflowExecution.commands

import grails.databinding.BindUsing
import grails.validation.Validateable
import groovy.json.JsonSlurper
import groovy.transform.CompileDynamic

import de.dkfz.tbi.otp.workflowExecution.ExternalWorkflowConfigSelector
import de.dkfz.tbi.otp.workflowExecution.SelectorType

/**
 * Command for creating new external workflow config selectors
 */
@CompileDynamic
class CreateCommand extends SelectorCommand implements Validateable {
    String selectorName
    SelectorType type
    String value

    // Make this list read only - should not be modified by client
    @BindUsing({ obj, source -> obj.matchingSelectors })
    Set<ExternalWorkflowConfigSelector> matchingSelectors

    static constraints = {
        selectorName blank: false, maxSize: 255
        type(nullable: false, validator: { val, obj ->
            if (val == SelectorType.DEFAULT_VALUES) {
                return 'workflowConfig.validation.check'
            }
        })
        value validator: { String v, CreateCommand cmd ->
            try {
                new JsonSlurper().parseText(v)
                return true
            } catch (Exception ignore) {
                return 'invalid.json'
            }
        }
        matchingSelectors nullable: true
    }
}
