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

import grails.testing.mixin.integration.Integration
import grails.transaction.Rollback
import spock.lang.Specification
import spock.lang.Unroll

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory

@Rollback
@Integration
class ExternalWorkflowConfigSelectorIntegrationSpec extends Specification implements WorkflowSystemDomainFactory {

    void "test validation of fragment, keys are unique"() {
        given:
        ExternalWorkflowConfigFragment fragment = createExternalWorkflowConfigFragment([
                configValues: '''{
                  "WORKFLOWS": {
                    "test1": "123",
                    "asdf": {
                      "test1": "123"
                    }
                  }
                }
                ''',
        ])
        ExternalWorkflowConfigSelector selector1 = createExternalWorkflowConfigSelector(externalWorkflowConfigFragment: fragment)

        ExternalWorkflowConfigFragment fragment2 = createExternalWorkflowConfigFragment([
                configValues: '''{
                  "WORKFLOWS": {
                    "test2": "123",
                    "asdf": {
                      "test2": "123"
                    }
                  }
                }
                ''',
        ])
        ExternalWorkflowConfigSelector selector2 = createExternalWorkflowConfigSelector([
                workflows: new ArrayList(selector1.workflows),
                workflowVersions: new ArrayList(selector1.workflowVersions),
                projects: new ArrayList(selector1.projects),
                seqTypes: new ArrayList(selector1.seqTypes),
                referenceGenomes: new ArrayList(selector1.referenceGenomes),
                libraryPreparationKits: new ArrayList(selector1.libraryPreparationKits),
                externalWorkflowConfigFragment: fragment2,
        ], false)

        expect:
        selector2.validate()
    }

    void "test validation of fragment, keys are duplicated"() {
        given:
        ExternalWorkflowConfigFragment fragment1 = createExternalWorkflowConfigFragment([
                configValues: '''{
                  "WORKFLOWS": {
                    "test1": "123",
                    "asdf": {
                      "test1": "123"
                    }
                  }
                }
                ''',
        ])
        ExternalWorkflowConfigSelector selector1 = createExternalWorkflowConfigSelector(externalWorkflowConfigFragment: fragment1)

        ExternalWorkflowConfigFragment fragment2 = createExternalWorkflowConfigFragment([
                configValues: fragment1.configValues,
        ])
        ExternalWorkflowConfigSelector selector2 = createExternalWorkflowConfigSelector([
                workflows: new ArrayList(selector1.workflows),
                workflowVersions: new ArrayList(selector1.workflowVersions),
                projects: new ArrayList(selector1.projects),
                seqTypes: new ArrayList(selector1.seqTypes),
                referenceGenomes: new ArrayList(selector1.referenceGenomes),
                libraryPreparationKits: new ArrayList(selector1.libraryPreparationKits),
                externalWorkflowConfigFragment: fragment2,
        ], false)

        expect:
        TestCase.assertValidateError(selector2, 'externalWorkflowConfigFragment', 'duplicate.key')
    }

    void "test the calculated priority if all fields are selected - highest priority is 127"() {
        when:
        ExternalWorkflowConfigSelector selector  = createExternalWorkflowConfigSelector()

        then:
        selector.priority == 127
    }

    @Unroll
    void "test if priority is calculated correctly - only workflows are selected for default"() {
        given:
        ExternalWorkflowConfigSelector selector  = createExternalWorkflowConfigSelector([
                name                          : "Default values 1",
                workflows                     : p_workflows(),
                workflowVersions              : [],
                referenceGenomes              : [],
                libraryPreparationKits        : [],
                seqTypes                      : [],
                projects                      : [],
                selectorType                  : SelectorType.DEFAULT_VALUES,
        ])

        expect:
        selector.priority == 64

        where:
        count | p_workflows
        1     | { [ createWorkflow() ] }
        2     | { [ createWorkflow(), createWorkflow() ] }
        3     | { [ createWorkflow(), createWorkflow(), createWorkflow() ] }
    }

    @Unroll
    void "test if priority is calculated correctly - multiple cases"() {
        given:
        ExternalWorkflowConfigSelector selector  = createExternalWorkflowConfigSelector([
                name                          : p_name,
                workflows                     : c_workflows(),
                workflowVersions              : c_workflowVersions(),
                seqTypes                      : c_seqTypes(),
                referenceGenomes              : c_referenceGenomes(),
                libraryPreparationKits        : c_libraryPreparationKits(),
                projects                      : c_projects(),
                selectorType                  : p_selectorType,
        ])

        expect:
        selector.priority == p_priority

        where:
        p_name              | p_selectorType                | c_workflows            | c_workflowVersions            | c_seqTypes            | c_referenceGenomes            | c_libraryPreparationKits            | c_projects             || p_priority
        "Default values 64" | SelectorType.DEFAULT_VALUES   | { [createWorkflow()] } | { [] }                        | { [] }                | { [] }                        | { [] }                              | { [] }                 || 0b01000000
        "Default values 96" | SelectorType.DEFAULT_VALUES   | { [createWorkflow()] } | { [createWorkflowVersion()] } | { [] }                | { [] }                        | { [] }                              | { [] }                 || 0b01100000
        "Default values 80" | SelectorType.DEFAULT_VALUES   | { [createWorkflow()] } | { [] }                        | { [createSeqType()] } | { [] }                        | { [] }                              | { [] }                 || 0b01010000
        "Not Default 65"    | SelectorType.ADAPTER_FILE     | { [createWorkflow()] } | { [] }                        | { [] }                | { [] }                        | { [] }                              | { [] }                 || 0b01000001
        "Not Default 33"    | SelectorType.BED_FILE         | { [] }                 | { [createWorkflowVersion()] } | { [] }                | { [] }                        | { [] }                              | { [] }                 || 0b00100001
        "Not Default 17"    | SelectorType.GENERIC          | { [] }                 | { [] }                        | { [createSeqType()] } | { [] }                        | { [] }                              | { [] }                 || 0b00010001
        "Not Default 41"    | SelectorType.REVERSE_SEQUENCE | { [] }                 | { [createWorkflowVersion()] } | { [] }                | { [createReferenceGenome()] } | { [] }                              | { [] }                 || 0b00101001
        "Not Default 13"    | SelectorType.GENERIC          | { [] }                 | { [] }                        | { [] }                | { [createReferenceGenome()] } | { [createLibraryPreparationKit()] } | { [] }                 || 0b00001101
        "Not Default 73"    | SelectorType.ADAPTER_FILE     | { [createWorkflow()] } | { [] }                        | { [] }                | { [createReferenceGenome()] } | { [] }                              | { [] }                 || 0b01001001
        "Not Default 127"   | SelectorType.ADAPTER_FILE     | { [createWorkflow()] } | { [createWorkflowVersion()] } | { [createSeqType()] } | { [createReferenceGenome()] } | { [createLibraryPreparationKit()] } | { [createProject() ] } || 0b01111111
    }
}