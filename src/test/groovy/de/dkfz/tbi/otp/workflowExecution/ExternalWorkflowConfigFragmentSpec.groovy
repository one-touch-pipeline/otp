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
package de.dkfz.tbi.otp.workflowExecution

import grails.testing.gorm.DataTest
import spock.lang.Specification

import de.dkfz.tbi.TestCase
import de.dkfz.tbi.otp.domainFactory.workflowSystem.WorkflowSystemDomainFactory

class ExternalWorkflowConfigFragmentSpec extends Specification implements DataTest, WorkflowSystemDomainFactory {

    @Override
    Class[] getDomainClassesToMock() {
        return [
                ExternalWorkflowConfigFragment,
        ]
    }

    void "test validation for configValues"() {
        given:
        ExternalWorkflowConfigFragment fragment = createExternalWorkflowConfigFragment([
                configValues: config,
        ], false)

        expect:
        fragment.validate() == valid
        if (!valid) {
            TestCase.assertAtLeastExpectedValidateError(fragment, "configValues", error, config)
        }

        where:
        config                               || valid | error
        '{}'                                 || true  | _
        '{OTP_CLUSTER: {CORES: "5"}}'        || true  | _
        null                                 || false | "nullable"
        ''                                   || false | "invalid.json"
        '{a:b:c }'                           || false | "invalid.json"
        '{a:b}'                              || false | "wrong.type"
        '{OTP_CLUSTER: {}}'                  || false | "errors.json"
        '{OTP_CLUSTER: {a:"b"}}'             || false | "errors.json"
        '{OTP_CLUSTER: {CORES: "5", a:"b"}}' || false | "errors.json"
    }

    void "test validation for valid Roddy config values"() {
        given:
        ExternalWorkflowConfigFragment fragment = createExternalWorkflowConfigFragment([
                configValues: '''{
                  "RODDY": {
                    "cvalues": {
                      "mergedBamSuffixList": {
                        "type": "string",
                        "value": "asdf"
                      },
                      "INSERT_SIZE_LIMIT": {
                        "type": "integer",
                        "value": 1
                      },
                      "runSlimWorkflow": {
                        "type": "boolean",
                        "value": "true"
                      },
                      "useBioBamBamSort": {
                        "type": "boolean",
                        "value": "false"
                      },
                      "BWA_VERSION": {
                        "value": "0.7.8"
                      }
                    },
                    "resources": {
                      "alignAndPair": {
                        "value": "bwaMemSort.sh",
                        "basepath": "qcPipeline",
                        "memory": "1G",
                        "cores": 1,
                        "nodes": 2,
                        "walltime": "00:10:00"
                      }
                    }
                  },
                  "RODDY_FILENAMES": {
                    "filenames": [
                      {
                        "class": "asdf",
                        "pattern": "asdf",
                        "selectiontag": "asdf",
                        "derivedFrom": "asdf"
                      }
                    ]
                  }
                }''',
        ], false)

        expect:
        fragment.validate()
    }

    void "test validation for invalid Roddy config values"() {
        given:
        String configValues = "{\"RODDY\": ${config} }"
        ExternalWorkflowConfigFragment fragment = createExternalWorkflowConfigFragment([
                configValues: configValues,
        ], false)

        expect:
        TestCase.assertAtLeastExpectedValidateError(fragment, "configValues", "errors.json", configValues)

        where:
        config << [
                // wrong property
                '''
                {"asdf": {"asdf": {
                  "type": "string",
                  "value": "asdf"
                }}}''',
                // additional property
                '''
                {"cvalues": {"asdf": {
                  "type": "string",
                  "value": "asdf",
                  "asdf": "asdf",
                }}}''',
                // illegal type
                '''
                {"cvalues": {"asdf": {
                  "type": "asdf",
                  "value": "asdf"
                }}}''',
                // missing property "value"
                '''
                {"cvalues": {"asdf": {
                  "type": "path",
                }}}''',
                // illegal value
                '''
                {"cvalues": {"asdf": {
                  "type": "boolean",
                  "value": "not true"
                }}}''',
                // illegal value
                '''
                {"cvalues": {"asdf": {
                  "type": "integer",
                  "value": "asdf"
                }}}''',

                // additional property
                '''
                {"resources": {"asdf": {
                  "value": "asdf",
                  "basepath": "asdf",
                  "gigacores": "1",
                }}}''',
                // missing property "value"
                '''
                {"resources": {"asdf": {
                  "basepath": "asdf"
                }}}''',
                // missing property "basepath"
                '''
                {"resources": {"asdf": {
                  "value": "asdf",
                }}}''',
                // illegal type
                '''
                {"resources": {"asdf": {
                  "value": "asdf",
                  "basepath": "asdf",
                  "cores": "one"
                }}}''',
                // illegal type
                '''
                {"resources": {"asdf": {
                  "value": "asdf",
                  "basepath": "asdf",
                  "nodes": "two"
                }}}''',
        ]
    }

    void "test validation for invalid Roddy filename config values"() {
        given:
        String configValues = "{\"RODDY_FILENAMES\": ${config} }"
        ExternalWorkflowConfigFragment fragment = createExternalWorkflowConfigFragment([
                configValues: configValues,
        ], false)

        expect:
        TestCase.assertAtLeastExpectedValidateError(fragment, "configValues", "errors.json", configValues)

        where:
        config << [
                // additional property
                '''
                {"filenames": [{
                  "class": "asdf",
                  "pattern": "asdf",
                  "asdf": "asdf",
                }]}''',
                // missing property "class"
                '''
                {"filenames": [{
                  "pattern": "asdf",
                }]}''',
                // missing property "pattern"
                '''
                {"filenames": [{
                  "class": "asdf",
                }]}''',
        ]
    }
}
