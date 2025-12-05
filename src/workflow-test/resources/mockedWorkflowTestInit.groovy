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

import groovy.transform.Field

import de.dkfz.tbi.otp.TestConfigService
import de.dkfz.tbi.otp.utils.CollectionUtils
import de.dkfz.tbi.otp.workflow.alignment.roddy.panCancer.PanCancerWorkflow
import de.dkfz.tbi.otp.workflow.alignment.roddy.rna.RnaAlignmentWorkflow
import de.dkfz.tbi.otp.workflow.alignment.roddy.wgbs.WgbsWorkflow
import de.dkfz.tbi.otp.workflow.analysis.aceseq.AceseqWorkflow
import de.dkfz.tbi.otp.workflow.analysis.indel.IndelWorkflow
import de.dkfz.tbi.otp.workflow.analysis.snv.SnvWorkflow
import de.dkfz.tbi.otp.workflow.analysis.sophia.SophiaWorkflow
import de.dkfz.tbi.otp.workflowExecution.*
import de.dkfz.tbi.otp.workflowExecution.commands.CreateCommand
import de.dkfz.tbi.otp.workflowTest.WorkflowTestProperty

/**
 * This script configures workflow test properties that were previously configured
 * in .otp.properties files for mocked workflow tests.
 *
 * This script is automatically loaded by AbstractWorkflowSpec and WorkflowTestCase
 * during test setup if configured via otp.testing.workflows.init.script property
 * in .otp.properties file.
 *
 * Note: Adapt this file to fit your local workflow test environment.
 */

@Field
TestConfigService configService = ctx.configService

@Field
ConfigSelectorService configSelectorService = ctx.configSelectorService

/**
 * Map of workflow test properties.
 * @see WorkflowTestProperty
 */
Map<WorkflowTestProperty, String> workflowTestProperties = [
        // Required properties for workflow tests
        (WorkflowTestProperty.TEST_WORKFLOW_INPUT_DIR)                        : "/workflows/reference-data",
        (WorkflowTestProperty.TEST_WORKFLOW_RESULT_DIR)                       : "/workflows/tests",
        (WorkflowTestProperty.TEST_WORKFLOW_RODDY_SHARED_FILES_BASE_DIRECTORY): '/workflows/ngs_share',
        (WorkflowTestProperty.TEST_WORKFLOW_RODDY_VIRTUAL_ENVS_DIRECTORY)     : '/workflows/virtualenvs',
        (WorkflowTestProperty.TEST_WORKFLOW_QUEUE)                            : 'devel',
        (WorkflowTestProperty.TEST_WORKFLOW_CONFIG_SUFFIX)                    : 'devel',
]

/**
 * configure apptainer for roddy
 */
void configureApptainer() {
    println "configure Apptainer"
    List<Workflow> roddyWorkflows = [
            // alignment
            PanCancerWorkflow.WORKFLOW,
            WgbsWorkflow.WORKFLOW,
            RnaAlignmentWorkflow.WORKFLOW,

            // analysis
            SnvWorkflow.WORKFLOW,
            IndelWorkflow.WORKFLOW,
            SophiaWorkflow.WORKFLOW,
            AceseqWorkflow.WORKFLOW,
    ].collect {
        println "- ${it}"
        CollectionUtils.exactlyOneElement(Workflow.findAllByName(it), "Could not find '${it}'")
    }

    println configSelectorService.create(new CreateCommand([
            selectorName: 'Roddy apptainer',
            type        : SelectorType.GENERIC,
            workflows   : roddyWorkflows,
            value       : """
                            {
                                "RODDY": {
                                    "cvalues": {
                                        "jobExecutionEnvironment": {
                                            "type": "string",
                                            "value": "apptainer"
                                        },
                                        "apptainerArguments": {
                                            "type": "string",
                                            "value": "--contain"
                                        },
                                        "containerEnginePath": {
                                            "type": "path",
                                            "value": "/usr/bin/apptainer"
                                        },
                                        "containerImage": {
                                            "type": "path",
                                            "value": "/workflows/dkfz_minimal/1.1.0"
                                        },
                                        "containerMounts": {
                                            "type": "bashArray",
                                            "value": "( /workflows/ngs_share/:/workflows/ngs_share/  /workflows/data/:/workflows/data/  /workflows/tests/:/workflows/tests/  /workflows/virtualenvs/:/workflows/virtualenvs/)"
                                        }
                                    }
                                }
                            }
                            """
    ]))
}

/**
 * configure Vep for snv and indel for roddy
 */
void configureVepForSnvAndIndelLocationSpecific() {
    println "configure vep for SNV and Indel"
    List<Workflow> roddyWorkflows = [
            // analysis
            SnvWorkflow.WORKFLOW,
            IndelWorkflow.WORKFLOW,
    ].collect {
        println "- ${it}"
        CollectionUtils.exactlyOneElement(Workflow.findAllByName(it), "Could not find '${it}'")
    }

    /**
     * Also the fragment is reference genome specific, in the workflow tests there is only one reference genome used,
     * which is also loaded later and therefore not available when this script is loaded
     */
    println configSelectorService.create(new CreateCommand([
            selectorName: 'VEP configuration',
            type        : SelectorType.GENERIC,
            workflows   : roddyWorkflows,
            value       : """
                            {
                                "RODDY": {
                                    "cvalues": {
                                        "VEP_CACHE_BASE": {
                                            "type": "path",
                                            "value": "/workflows/vep"
                                        }
                                    }
                                }
                            }
                            """
    ]))
}

/**
 * Configure workflow-specific settings with example configurations.
 * This method demonstrates how to configure various workflows
 * using the ConfigSelectorService
 * @see ConfigSelectorService#create().
 */
void configureWorkflowSpecificSettings() {
    String workflowName = "nf-seq-qc"
    List<Workflow> workflowsByName = [CollectionUtils.exactlyOneElement(Workflow.findAllByName(workflowName), "Could not find '${workflowName}'")]

    println configSelectorService.create(new CreateCommand([
            selectorName: 'weskit selector',
            type        : SelectorType.GENERIC,
            workflows   : workflowsByName,
            value       : """
                            {
                                "WESKIT": {
                                    "MAX_MEMORY": "512M",
                                    "MAX_RUNTIME": "24:00",
                                }
                            }
                          """
    ]))

    println configSelectorService.create(new CreateCommand([
            selectorName: 'Nextflow selector',
            type        : SelectorType.GENERIC,
            workflows   : workflowsByName,
            value       : """
                            {
                                "WESKIT": {
                                    "WORKFLOW_TYPE_VERSION": "23.10.1",
                                    "PROFILE": "lsf,apptainer",
                                }
                            }
                          """
    ]))

    println configSelectorService.create(new CreateCommand([
            selectorName: 'nf-seq-qc selector',
            type        : SelectorType.GENERIC,
            workflows   : workflowsByName,
            value       : """
                            {
                                "WESKIT": {
                                    "WORKFLOW_CONFIG_URL": "nf-seq-qc_1.2.2/main.nf",
                                }
                            }
                          """
    ]))
}

// Execute the configuration
try {
    println("=== Starting workflow test initialization ===")

    configService.storeWorkflowTestProperties(workflowTestProperties)

    if (Workflow.count == 0) {
        println "Skip fragment configuration, since no workflows in new system initialized"
    } else {
        configureApptainer()
        configureVepForSnvAndIndelLocationSpecific()
        configureWorkflowSpecificSettings()
    }

    println("=== Workflow test initialization completed successfully ===")
} catch (Exception e) {
    println("ERROR: Workflow test initialization failed: ${e.message}")
    e.printStackTrace()
    throw e
}
