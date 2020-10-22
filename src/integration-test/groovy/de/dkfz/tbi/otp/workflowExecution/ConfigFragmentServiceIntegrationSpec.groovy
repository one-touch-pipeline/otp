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

import grails.converters.JSON
import grails.testing.mixin.integration.Integration
import grails.transaction.Rollback
import spock.lang.Specification

import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory

@Rollback
@Integration
class ConfigFragmentServiceIntegrationSpec extends Specification implements WorkflowSystemDomainFactory {

    void "test parseExternalWorkflowConfigFragmentString"() {
        given:
        ConfigFragmentService service = new ConfigFragmentService()
        ExternalWorkflowConfigSelector ewcs1 = createExternalWorkflowConfigSelector([
                externalWorkflowConfigFragment: createExternalWorkflowConfigFragment([
                        configValues: '{ "A": "A_ThisNotSinceLowerPrio", "B": { "C": "C_ThisNotSinceLowerPrio", "D": "D_ThisNotSinceLowerPrio" } }'
                ]),
                fineTuningPriority            : 0,
        ])
        ExternalWorkflowConfigSelector ewcs2 = createExternalWorkflowConfigSelector([
                externalWorkflowConfigFragment: createExternalWorkflowConfigFragment([
                        configValues: '{ "A": "A_ShouldAppear", "B": { "C": "C_ThisSinceHigherPrio", "D": "D_ThisToo" } }'
                ]),
                fineTuningPriority            : 5,
        ])
        ExternalWorkflowConfigSelector ewcs3 = createExternalWorkflowConfigSelector([
                externalWorkflowConfigFragment: createExternalWorkflowConfigFragment([
                        configValues: '{ "F": "F_ShouldAppear", "B": { "C": "C_ThisNotSinceLowerPrio", "E": { "G": "G_ThisToo" } } }'
                ]),
                fineTuningPriority            : 1,
        ])
        service.configSelectorService = Mock(ConfigSelectorService) {
            1 * findAllSelectorsSortedByPriority(_) >> [ewcs2, ewcs3, ewcs1]
        }

        expect:
        String result = '{  "A": "A_ShouldAppear", "B": { "D": "D_ThisToo", "E": { "G": "G_ThisToo" }, "C": "C_ThisSinceHigherPrio" }, "F": "F_ShouldAppear" }'
        JSON.parse(result) == service.parseExternalWorkflowConfigFragmentString(new SingleSelectSelectorExtendedCriteria())
    }
}
